package org.anta.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.anta.entity.FileMetadata;
import org.anta.repository.FileMetadataRepository;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@ApplicationScoped
public class CloudService {

    @Inject
    FileMetadataRepository fileMetadataRepository;

    @Inject
    Cloudinary cloudinary;

    private Logger log = Logger.getLogger(CloudService.class.getName());

    @Transactional
    public List<FileMetadata> uploadMultiple(List<FileUpload> files, Long uploaderId) {
        List<FileMetadata> results = new ArrayList<>();

        for (FileUpload file : files) {
            try {
                byte[] bytes = Files.readAllBytes(file.uploadedFile());

                Map uploadResult = cloudinary.uploader().upload(bytes, ObjectUtils.emptyMap());
                String url = (String) uploadResult.get("secure_url");
                String publicId = (String) uploadResult.get("public_id");
                String format = (String) uploadResult.get("format");
                String resourceType = (String) uploadResult.get("resource_type");

                FileMetadata metadata = FileMetadata.builder()
                        .publicId(publicId)
                        .url(url)
                        .productId(null)
                        .uploaderId(uploaderId)
                        .format(format)
                        .resourceType(resourceType)
                        .uploadedAt(LocalDateTime.now())
                        .isMain(false)
                        .build();

                metadata = fileMetadataRepository.save(metadata);
                results.add(metadata);

            } catch (IOException e) {
                throw new RuntimeException("Failed to upload file: " + file.fileName(), e);
            }
        }

        return results;
    }

    @Transactional
    public void assignImagesToProduct(Long productId, List<Long> imageIds, Long mainId) {
        // nếu imageIds null -> clear all link for product
        if (imageIds == null || imageIds.isEmpty()) {
            // clear product_id and is_main for all files previously linked to this product
            fileMetadataRepository.clearProductForProductId(productId);
            return;
        }

        // Reset is_main for all files currently attached to this product
        fileMetadataRepository.resetIsMainForProduct(productId);

        // Find current files linked to product (so we can unassign those not in new list)
        List<FileMetadata> currently = fileMetadataRepository.findByProductId(productId);

        // Convert incoming list to Set for quick lookup
        java.util.Set<Long> incomingSet = new java.util.HashSet<>(imageIds);

        // Unassign files currently linked but not in incomingSet
        for (FileMetadata f : currently) {
            if (!incomingSet.contains(f.getId())) {
                f.setProductId(null);
                f.setIsMain(false);
                fileMetadataRepository.save(f);
            }
        }

        // Now assign/mark for incoming ids
        for (int i = 0; i < imageIds.size(); i++) {
            Long id = imageIds.get(i);
            fileMetadataRepository.findById(id).ifPresentOrElse(fm -> {
                // assign product
                fm.setProductId(productId);
                // decide isMain
                boolean isMainHere = (mainId != null && mainId.equals(fm.getId()));
                fm.setIsMain(isMainHere);
                if (isMainHere) {
                    // nothing else
                }
                fileMetadataRepository.save(fm);
            }, () -> {
                // log if id not found
                // you might want to create record or ignore
                log.warning("assignImagesToProduct: file id {} not found" + id);
            });
        }

        // If no explicit mainId was provided, try to set first existing in imageIds as main
        if (mainId == null) {
            // check whether any of the imageIds now has isMain true
            List<FileMetadata> linked = fileMetadataRepository.findByProductId(productId);
            boolean anyMain = linked.stream().anyMatch(FileMetadata::getIsMain); // adjust getter if boolean/int
            if (!anyMain && !linked.isEmpty()) {
                FileMetadata first = linked.stream().filter(f -> incomingSet.contains(f.getId())).findFirst().orElse(linked.get(0));
                first.setIsMain(true);
                fileMetadataRepository.save(first);
            }
        }

        // done (transaction will commit)
    }

    public List<FileMetadata> getByProductId(Long productId) {
        return fileMetadataRepository.findByProductId(productId);
    }

    @Transactional
    public void cleanUnusedFiles() {
        // delete DB rows older than cutoff and delete actual files from Cloudinary
        LocalDateTime cutoff = LocalDateTime.now().minusDays(3); // configurable
        List<FileMetadata> toDelete = fileMetadataRepository.findByProductIdIsNullAndUploadedAtBefore(cutoff);

        for (FileMetadata f : toDelete) {
            try {
                if (f.getPublicId() != null) {
                    cloudinary.uploader().destroy(f.getPublicId(), ObjectUtils.emptyMap());
                }
            } catch (Exception ex) {
                // log but continue
            }
        }

        fileMetadataRepository.deleteTempFilesOlderThan(cutoff);
    }

    @Transactional
    public void deleteFileById(Long fileId) {
        // tìm metadata từ repo
        FileMetadata file = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        try {
            if (file.getPublicId() != null) {
                cloudinary.uploader().destroy(file.getPublicId(), ObjectUtils.emptyMap());
            }
        } catch (Exception ex) {
            // giữ flow như cũ: nếu xóa provider lỗi thì vẫn xóa DB record
        }

        // sau đó xóa record DB
        fileMetadataRepository.delete(file);
    }
}