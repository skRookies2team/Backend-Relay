package com.story.relay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3UploadService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    /**
     * Upload image bytes to S3 and return the public URL
     */
    public String uploadImage(String fileKey, byte[] imageBytes) {
        log.info("Uploading image to S3: bucket={}, key={}, size={} bytes",
            bucketName, fileKey, imageBytes.length);

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .contentType("image/png")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(imageBytes));

            String imageUrl = String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName, region, fileKey);

            log.info("Image uploaded successfully: {}", imageUrl);
            return imageUrl;

        } catch (Exception e) {
            log.error("Failed to upload image to S3: {}", e.getMessage(), e);
            throw new RuntimeException("S3 upload failed: " + e.getMessage());
        }
    }
}
