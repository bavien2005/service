package org.anta.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.anta.client.CategoryClient;
import org.anta.client.CloudClient;
import org.anta.dto.request.ProductRequest;
import org.anta.dto.request.ProductVariantRequest;
import org.anta.dto.response.CategoryResponse;
import org.anta.dto.response.FileMetadataDto;
import org.anta.dto.response.ProductResponse;
import org.anta.entity.Product;
import org.anta.entity.ProductVariant;
import org.anta.mapper.ProductMapper;
import org.anta.mapper.ProductVariantMapper;
import org.anta.repository.ProductRepository;
import org.anta.repository.ProductVariantRepository;
import org.anta.util.StringUtils;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class ProductService {

    private static final Logger log = Logger.getLogger(ProductService.class);

    @Inject
    ProductRepository productRepository;

    @Inject
    ProductMapper productMapper;

    @Inject
    ProductVariantRepository productVariantRepository;

    @Inject
    ProductVariantMapper productVariantMapper;

    @Inject
    CategoryClient categoryClient;

    @Inject
    CloudClient cloudClient;

    @Transactional
    public List<ProductResponse> getAllProduct() {
        List<Product> products = productRepository.listAll();

        Map<Long, CategoryResponse> catById = buildCategoryIndex();

        return products.stream().map(product -> {
            ProductResponse response = productMapper.toResponse(product);

            List<ProductVariant> variants = productVariantRepository.findByProductId(product.getId());
            response.setVariants(productVariantMapper.toResponseList(variants));

            int totalStock;
            if (variants != null && !variants.isEmpty()) {
                totalStock = variants.stream()
                        .mapToInt(v -> v.getStock() == null ? 0 : v.getStock())
                        .sum();
            } else {
                totalStock = product.getTotalStock() != null ? product.getTotalStock() : 0;
            }

            response.setTotalStock(totalStock);

            if (response.getImages() != null && !response.getImages().isEmpty()) {
                response.setThumbnail(response.getImages().get(0));
            }

            double displayPrice = computeDisplayPrice(product);
            response.setPrice(BigDecimal.valueOf(displayPrice));

            if (response.getRating() == null) {
                response.setRating(5);
            }

            if (response.getSales() == null) {
                response.setSales(0L);
            }

            attachCategoryInfo(response, catById);

            return response;
        }).collect(Collectors.toList());
    }

    @Transactional
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findByIdOptional(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        ProductResponse response = productMapper.toResponse(product);

        List<ProductVariant> variants = productVariantRepository.findByProductId(product.getId());
        response.setVariants(productVariantMapper.toResponseList(variants));

        int totalStock;
        if (variants != null && !variants.isEmpty()) {
            totalStock = variants.stream()
                    .mapToInt(v -> v.getStock() == null ? 0 : v.getStock())
                    .sum();
        } else {
            totalStock = product.getTotalStock() != null ? product.getTotalStock() : 0;
        }

        response.setTotalStock(totalStock);

        if (response.getImages() != null && !response.getImages().isEmpty()) {
            response.setThumbnail(response.getImages().get(0));
        }

        if (response.getCategoryId() != null) {
            try {
                CategoryResponse c = categoryClient.getCategoryById(response.getCategoryId());
                response.setCategoryName(c.getName());
                response.setCategorySlug(c.getSlug());
                response.setCategoryTitle(c.getTitle());
            } catch (Exception ignored) {
            }
        }

        double displayPrice = computeDisplayPrice(product);
        response.setPrice(BigDecimal.valueOf(displayPrice));

        if (response.getRating() == null) {
            response.setRating(5);
        }

        if (response.getSales() == null) {
            response.setSales(0L);
        }

        return response;
    }

    @Transactional
    public ProductResponse addProduct(ProductRequest productRequest) {
        if (productRequest.getCategoryId() != null) {
            boolean exists = categoryClient.existsCategory(productRequest.getCategoryId());

            if (!exists) {
                throw new RuntimeException("Category not found with ID: " + productRequest.getCategoryId());
            }
        }

        Product entity = productMapper.toEntityWithParents(productRequest);

        entity.setCategoryId(productRequest.getCategoryId());

        if (entity.getImages() == null) {
            entity.setImages(List.of());
        }

        if (entity.getVariants() != null && !entity.getVariants().isEmpty()) {
            int total = entity.getVariants().stream()
                    .mapToInt(v -> v.getStock() == null ? 0 : v.getStock())
                    .sum();
            entity.setTotalStock(total);
        } else {
            if (productRequest.getTotalStock() != null) {
                entity.setTotalStock(productRequest.getTotalStock());
            } else {
                entity.setTotalStock(0);
            }
        }

        productRepository.persist(entity);
        productRepository.flush();

        Product saved = entity;

        List<Long> imageIds = productRequest.getImageIds();

        if (imageIds != null && !imageIds.isEmpty()) {
            boolean assignedToCloud = false;

            try {
                cloudClient.updateProduct(saved.getId(), imageIds);
                assignedToCloud = true;

                FileMetadataDto[] files = cloudClient.getFilesByProduct(saved.getId());

                if (files != null && files.length > 0) {
                    List<String> urls = Arrays.stream(files)
                            .map(FileMetadataDto::getUrl)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    saved.setImages(urls);

                    String thumbnail = Arrays.stream(files)
                            .filter(f -> Boolean.TRUE.equals(f.getIsMain()))
                            .map(FileMetadataDto::getUrl)
                            .findFirst()
                            .orElse(urls.size() > 0 ? urls.get(0) : null);

                    productRepository.persist(saved);
                } else {
                    throw new RuntimeException("No files returned from cloud for product " + saved.getId());
                }

            } catch (Exception ex) {
                log.errorf(ex, "FAILED TO ASSIGN IMAGES for productId %s : %s", saved.getId(), ex.getMessage());

                if (assignedToCloud) {
                    try {
                        cloudClient.updateProduct(saved.getId(), List.of());
                    } catch (Exception inner) {
                        log.warnf("ROLLBACK FAILED for product %s : %s", saved.getId(), inner.getMessage());
                    }
                }

                throw new RuntimeException("Failed to associate images. Product was not created.", ex);
            }
        }

        ProductResponse resp = productMapper.toResponse(saved);

        if (resp.getImages() != null && !resp.getImages().isEmpty()) {
            resp.setThumbnail(resp.getImages().get(0));
        }

        double displayPriceAfter = computeDisplayPrice(saved);
        resp.setPrice(BigDecimal.valueOf(displayPriceAfter));

        if (resp.getRating() == null) {
            resp.setRating(5);
        }

        if (resp.getSales() == null) {
            resp.setSales(0L);
        }

        return resp;
    }

    private double computeDisplayPrice(Product product) {
        try {
            if (product.getPrice() != null && product.getPrice().doubleValue() > 0) {
                return product.getPrice().doubleValue();
            }
        } catch (Exception ignored) {
        }

        if (product.getId() == null) {
            return 0.0;
        }

        List<ProductVariant> variants = productVariantRepository.findByProductId(product.getId());

        Optional<Double> min = variants.stream()
                .map(v -> v.getPrice() == null ? 0.0 : v.getPrice().doubleValue())
                .filter(p -> p != null && p > 0)
                .min(Double::compareTo);

        return min.orElse(0.0);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest productRequest) {
        Product product = productRepository.findByIdOptional(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        productMapper.updateFromRequest(productRequest, product);

        List<ProductVariant> managed = product.getVariants();

        if (managed == null) {
            managed = new ArrayList<>();
            product.setVariants(managed);
        }

        Map<Long, ProductVariant> managedById = managed.stream()
                .filter(v -> v.getId() != null)
                .collect(Collectors.toMap(ProductVariant::getId, v -> v));

        Map<String, ProductVariant> managedBySku = managed.stream()
                .filter(v -> v.getSku() != null)
                .collect(Collectors.toMap(ProductVariant::getSku, v -> v));

        List<ProductVariant> incoming = new ArrayList<>();

        if (productRequest.getVariants() != null) {
            for (ProductVariantRequest vReq : productRequest.getVariants()) {
                ProductVariant match = null;

                if (vReq.getId() != null) {
                    match = managedById.get(vReq.getId());
                }

                if (match == null && vReq.getSku() != null) {
                    match = managedBySku.get(vReq.getSku());
                }

                if (match != null) {
                    productVariantMapper.updateFromRequest(vReq, match);
                    incoming.add(match);

                    if (match.getId() != null) {
                        managedById.remove(match.getId());
                    }

                    if (match.getSku() != null) {
                        managedBySku.remove(match.getSku());
                    }
                } else {
                    ProductVariant nv = productVariantMapper.toEntity(vReq);
                    nv.setProduct(product);
                    incoming.add(nv);
                }
            }
        }

        Set<Long> idsToRemove = new HashSet<>(managedById.keySet());

        Iterator<ProductVariant> it = managed.iterator();

        while (it.hasNext()) {
            ProductVariant mv = it.next();

            boolean shouldRemove = false;

            if (mv.getId() != null && idsToRemove.contains(mv.getId())) {
                shouldRemove = true;
            } else {
                boolean presentInIncoming = incoming.stream().anyMatch(i ->
                        (i.getId() != null && i.getId().equals(mv.getId()))
                                || (i.getSku() != null && i.getSku().equals(mv.getSku()))
                );

                if (!presentInIncoming) {
                    shouldRemove = true;
                }
            }

            if (shouldRemove) {
                it.remove();
            }
        }

        for (ProductVariant inv : incoming) {
            boolean exists = false;

            if (inv.getId() != null) {
                exists = managed.stream().anyMatch(m -> Objects.equals(m.getId(), inv.getId()));
            } else if (inv.getSku() != null) {
                exists = managed.stream().anyMatch(m -> Objects.equals(m.getSku(), inv.getSku()));
            }

            if (!exists) {
                inv.setProduct(product);
                managed.add(inv);
            }
        }

        for (ProductVariant v : managed) {
            if (v.getSku() == null) {
                continue;
            }

            Optional<ProductVariant> found = productVariantRepository.findBySku(v.getSku());

            if (found.isPresent()
                    && v.getId() != null
                    && !Objects.equals(found.get().getId(), v.getId())) {
                throw new RuntimeException("SKU already exists: " + v.getSku());
            }

            if (found.isPresent()
                    && v.getId() == null
                    && !Objects.equals(found.get().getProduct().getId(), product.getId())) {
                throw new RuntimeException("SKU already exists in another product: " + v.getSku());
            }
        }

        int total = managed.stream()
                .mapToInt(v -> v.getStock() == null ? 0 : v.getStock())
                .sum();

        product.setTotalStock(total);

        Product saved = product;

        ProductResponse resp = productMapper.toResponse(saved);

        List<ProductVariant> variants = productVariantRepository.findByProductId(saved.getId());
        resp.setVariants(productVariantMapper.toResponseList(variants));

        resp.setTotalStock(saved.getTotalStock() != null ? saved.getTotalStock() : 0);

        if (resp.getImages() != null && !resp.getImages().isEmpty()) {
            resp.setThumbnail(resp.getImages().get(0));
        }

        double displayPrice = computeDisplayPrice(saved);
        resp.setPrice(BigDecimal.valueOf(displayPrice));

        if (resp.getRating() == null) {
            resp.setRating(5);
        }

        if (resp.getSales() == null) {
            resp.setSales(0L);
        }

        return resp;
    }

    @Transactional
    public ProductResponse deleteProduct(Long id) {
        Product product = productRepository.findByIdOptional(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        try {
            FileMetadataDto[] files = cloudClient.getFilesByProduct(id);

            if (files != null && files.length > 0) {
                for (FileMetadataDto f : files) {
                    try {
                        cloudClient.deleteFile(f.getId());
                    } catch (Exception e) {
                        log.warnf(
                                "Failed to delete file %s on cloud for product %s : %s",
                                f.getId(),
                                id,
                                e.getMessage()
                        );
                    }
                }
            }
        } catch (Exception ex) {
            log.warnf("Failed to fetch files from cloud for product %s : %s", id, ex.getMessage());
        }

        ProductResponse response = productMapper.toResponse(product);

        productRepository.delete(product);

        return response;
    }

    @Transactional
    public List<ProductResponse> getProductByName(String name) {
        String search = StringUtils.removeAccent(name).toLowerCase();

        List<Product> products = productRepository.listAll().stream()
                .filter(p -> p.getName() != null
                        && StringUtils.removeAccent(p.getName()).toLowerCase().contains(search))
                .collect(Collectors.toList());

        return productMapper.toResponseList(products);
    }

    @Transactional
    public ProductResponse syncImagesFromCloud(Long productId) {
        Product product = productRepository.findByIdOptional(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        try {
            FileMetadataDto[] files = null;

            try {
                files = cloudClient.getFilesByProduct(productId);
            } catch (Exception e) {
                log.warnf("Failed to map cloud response to FileMetadataDto[]: %s", e.getMessage());
            }

            List<String> urls;

            if (files != null && files.length > 0) {
                urls = Arrays.stream(files)
                        .map(f -> {
                            if (f.getUrl() != null) {
                                return f.getUrl();
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            } else {
                try {
                    Object[] raw = cloudClient.getFilesByProductRaw(productId);

                    urls = Arrays.stream(raw != null ? raw : new Object[0])
                            .map(o -> {
                                try {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> m = (Map<String, Object>) o;

                                    if (m.get("url") != null) {
                                        return String.valueOf(m.get("url"));
                                    }

                                    if (m.get("secure_url") != null) {
                                        return String.valueOf(m.get("secure_url"));
                                    }

                                    if (m.get("publicUrl") != null) {
                                        return String.valueOf(m.get("publicUrl"));
                                    }

                                    if (m.get("path") != null) {
                                        return String.valueOf(m.get("path"));
                                    }
                                } catch (Exception ex) {
                                    // ignore
                                }

                                return null;
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                } catch (Exception ex) {
                    log.errorf("Failed to fetch raw files from cloud for product %s : %s", productId, ex.getMessage());
                    urls = List.of();
                }
            }

            if (urls == null || urls.isEmpty()) {
                throw new RuntimeException("No files returned from cloud for product " + productId);
            }

            product.setImages(urls);

            Product saved = product;

            ProductResponse resp = productMapper.toResponse(saved);

            List<ProductVariant> variants = productVariantRepository.findByProductId(saved.getId());
            resp.setVariants(productVariantMapper.toResponseList(variants));

            int totalStock;
            if (variants != null && !variants.isEmpty()) {
                totalStock = variants.stream()
                        .mapToInt(v -> v.getStock() == null ? 0 : v.getStock())
                        .sum();
            } else {
                totalStock = saved.getTotalStock() != null ? saved.getTotalStock() : 0;
            }

            resp.setTotalStock(totalStock);

            String thumbnail = null;

            if (files != null && files.length > 0) {
                thumbnail = Arrays.stream(files)
                        .filter(f -> Boolean.TRUE.equals(f.getIsMain()))
                        .map(FileMetadataDto::getUrl)
                        .findFirst()
                        .orElse(null);
            }

            if (thumbnail == null && !urls.isEmpty()) {
                thumbnail = urls.get(0);
            }

            resp.setThumbnail(thumbnail);

            double displayPrice = computeDisplayPrice(saved);
            resp.setPrice(BigDecimal.valueOf(displayPrice));

            if (resp.getRating() == null) {
                resp.setRating(5);
            }

            if (resp.getSales() == null) {
                resp.setSales(0L);
            }

            return resp;

        } catch (Exception ex) {
            log.errorf(ex, "Failed to sync images from cloud for product %s : %s", productId, ex.getMessage());
            throw new RuntimeException("Failed to sync images from cloud: " + ex.getMessage(), ex);
        }
    }

    @Transactional
    public Product assignCategory(Long productId, Long categoryId) {
        if (!categoryClient.existsCategory(categoryId)) {
            throw new RuntimeException("Category not found: " + categoryId);
        }

        Product product = productRepository.findByIdOptional(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        product.setCategoryId(categoryId);

        return product;
    }

    @Transactional
    public Product removeCategory(Long productId) {
        Product product = productRepository.findByIdOptional(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        product.setCategoryId(null);

        return product;
    }

    @Transactional
    public List<ProductResponse> searchProducts(String q) {
        if (q == null || q.trim().isEmpty()) {
            return getAllProduct();
        }

        String keyword = q.trim();

        List<Product> products = productRepository.searchFullTextLoose(keyword);

        return products.stream().map(product -> {
            ProductResponse response = productMapper.toResponse(product);

            List<ProductVariant> variants = productVariantRepository.findByProductId(product.getId());
            response.setVariants(productVariantMapper.toResponseList(variants));

            int totalStock;
            if (variants != null && !variants.isEmpty()) {
                totalStock = variants.stream()
                        .mapToInt(v -> v.getStock() == null ? 0 : v.getStock())
                        .sum();
            } else {
                totalStock = product.getTotalStock() != null ? product.getTotalStock() : 0;
            }

            response.setTotalStock(totalStock);

            if (response.getImages() != null && !response.getImages().isEmpty()) {
                response.setThumbnail(response.getImages().get(0));
            }

            double displayPrice = computeDisplayPrice(product);
            response.setPrice(BigDecimal.valueOf(displayPrice));

            if (response.getRating() == null) {
                response.setRating(5);
            }

            if (response.getSales() == null) {
                response.setSales(0L);
            }

            return response;
        }).collect(Collectors.toList());
    }

    @Transactional
    public List<ProductResponse> listByCategory(Long categoryId) {
        List<Product> products = productRepository.findByCategoryId(categoryId);

        return products.stream().map(p -> {
            ProductResponse r = productMapper.toResponse(p);

            List<ProductVariant> vars = productVariantRepository.findByProductId(p.getId());
            r.setVariants(productVariantMapper.toResponseList(vars));

            int total = (vars == null || vars.isEmpty())
                    ? (p.getTotalStock() == null ? 0 : p.getTotalStock())
                    : vars.stream().mapToInt(v -> v.getStock() == null ? 0 : v.getStock()).sum();

            r.setTotalStock(total);

            if (r.getImages() != null && !r.getImages().isEmpty()) {
                r.setThumbnail(r.getImages().get(0));
            }

            double displayPrice = computeDisplayPrice(p);
            r.setPrice(BigDecimal.valueOf(displayPrice));

            if (r.getRating() == null) {
                r.setRating(5);
            }

            if (r.getSales() == null) {
                r.setSales(0L);
            }

            return r;
        }).toList();
    }

    @Transactional
    public List<ProductResponse> getAllFiltered(String title, String categorySlug) {
        List<Product> products;

        if (title != null && categorySlug != null) {
            Optional<Long> catIdOpt = categoryClient.resolveCategoryId(title, categorySlug);
            products = catIdOpt.map(productRepository::findByCategoryId).orElse(List.of());
        } else if (title != null) {
            Map<String, List<CategoryResponse>> grouped = categoryClient.getGrouped();
            List<CategoryResponse> cats = grouped.getOrDefault(title, grouped.get(title.toLowerCase()));

            if (cats == null || cats.isEmpty()) {
                return List.of();
            }

            products = cats.stream()
                    .flatMap(c -> productRepository.findByCategoryId(c.getId()).stream())
                    .toList();
        } else if (categorySlug != null) {
            Map<String, List<CategoryResponse>> grouped = categoryClient.getGrouped();

            Optional<Long> catIdOpt = grouped.values().stream()
                    .flatMap(List::stream)
                    .filter(c -> c.getSlug() != null && c.getSlug().equalsIgnoreCase(categorySlug))
                    .map(CategoryResponse::getId)
                    .findFirst();

            products = catIdOpt.map(productRepository::findByCategoryId).orElse(List.of());
        } else {
            products = productRepository.listAll();
        }

        Map<Long, CategoryResponse> catById = buildCategoryIndex();

        return products.stream().map(p -> {
            ProductResponse r = productMapper.toResponse(p);

            List<ProductVariant> vars = productVariantRepository.findByProductId(p.getId());
            r.setVariants(productVariantMapper.toResponseList(vars));

            int total = (vars == null || vars.isEmpty())
                    ? (p.getTotalStock() == null ? 0 : p.getTotalStock())
                    : vars.stream().mapToInt(v -> v.getStock() == null ? 0 : v.getStock()).sum();

            r.setTotalStock(total);

            if (r.getImages() != null && !r.getImages().isEmpty()) {
                r.setThumbnail(r.getImages().get(0));
            }

            r.setPrice(BigDecimal.valueOf(computeDisplayPrice(p)));

            if (r.getRating() == null) {
                r.setRating(5);
            }

            if (r.getSales() == null) {
                r.setSales(0L);
            }

            attachCategoryInfo(r, catById);

            return r;
        }).toList();
    }

    @Transactional
    public int deleteProductsByCategory(Long categoryId) {
        List<Product> products = productRepository.findByCategoryId(categoryId);

        if (products == null || products.isEmpty()) {
            return 0;
        }

        List<Long> ids = products.stream()
                .map(Product::getId)
                .toList();

        for (Long id : ids) {
            deleteProduct(id);
        }

        return ids.size();
    }

    private Map<Long, CategoryResponse> buildCategoryIndex() {
        Map<String, List<CategoryResponse>> grouped = categoryClient.getGrouped();

        return grouped.values().stream()
                .flatMap(List::stream)
                .filter(c -> c.getId() != null)
                .collect(Collectors.toMap(CategoryResponse::getId, c -> c, (a, b) -> a));
    }

    private void attachCategoryInfo(ProductResponse r, Map<Long, CategoryResponse> catById) {
        if (r == null || r.getCategoryId() == null) {
            return;
        }

        CategoryResponse c = catById.get(r.getCategoryId());

        if (c == null) {
            return;
        }

        r.setCategoryName(c.getName());
        r.setCategorySlug(c.getSlug());
        r.setCategoryTitle(c.getTitle());
    }
}