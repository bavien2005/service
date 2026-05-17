package org.anta.client;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Arrays;

@ApplicationScoped
@Slf4j
public class CloudClient {

    @Inject
    @RestClient
    CloudRestClient cloudRestClient;

    public FileMetadataDTO getMainImage(Long productId) {
        try {
            FileMetadataDTO[] files = cloudRestClient.getFilesByProductId(productId);

            if (files == null || files.length == 0) {
                return null;
            }

            return Arrays.stream(files)
                    .filter(FileMetadataDTO::isMain)
                    .findFirst()
                    .orElse(files[0]);
        } catch (Exception e) {
            // Không để exception làm fail toàn bộ flow — trả null để BE tiếp tục lưu item (không có ảnh)
            log.warn("[CloudClient] getMainImage failed for product {} -> {}", productId, e.toString());
            return null;
        }
    }
}
