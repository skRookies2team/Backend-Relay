package com.story.relay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
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

    // Maximum file size: 10MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB in bytes

    /**
     * Upload image bytes to S3 and return the public URL
     * Validates file size and sets appropriate ACL
     *
     * @param fileKey File key (path) in S3 bucket
     * @param imageBytes Image data as byte array
     * @return Public URL of uploaded image
     * @throws RuntimeException if upload fails or file size exceeds limit
     */
    public String uploadImage(String fileKey, byte[] imageBytes) {
        // Validate file size
        if (imageBytes.length > MAX_FILE_SIZE) {
            log.error("File size exceeds limit: {} bytes (max: {} bytes)",
                imageBytes.length, MAX_FILE_SIZE);
            throw new RuntimeException(
                String.format("File size exceeds limit: %d bytes (max: %d bytes)",
                    imageBytes.length, MAX_FILE_SIZE)
            );
        }

        log.info("Uploading image to S3: bucket={}, key={}, size={} bytes",
            bucketName, fileKey, imageBytes.length);

        try {
            // Determine content type from file extension
            String contentType = getContentType(fileKey);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .contentType(contentType)
                    .acl(ObjectCannedACL.PRIVATE)  // Set ACL to private for security
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

    /**
     * Determine content type based on file extension
     *
     * @param fileKey File key/path
     * @return MIME type string
     */
    private String getContentType(String fileKey) {
        String lowerCaseKey = fileKey.toLowerCase();

        if (lowerCaseKey.endsWith(".png")) {
            return "image/png";
        } else if (lowerCaseKey.endsWith(".jpg") || lowerCaseKey.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerCaseKey.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerCaseKey.endsWith(".webp")) {
            return "image/webp";
        } else {
            // Default to PNG if extension not recognized
            log.warn("Unknown file extension for key: {}, defaulting to image/png", fileKey);
            return "image/png";
        }
    }
}
