package io.github.sueguo.finbridge.service;

import java.io.InputStream;

/**
 * Service contract for AWS S3 operations.
 */
public interface S3Service {

    /**
     * Upload an object to S3.
     *
     * @param bucket      target bucket name
     * @param key         object key (path)
     * @param inputStream object content
     * @param contentType MIME type
     * @param sizeBytes   content length in bytes
     */
    void upload(String bucket, String key, InputStream inputStream,
                String contentType, long sizeBytes);

    /**
     * Generate a pre-signed URL valid for a limited time.
     *
     * @param bucket bucket name
     * @param key    object key
     * @return pre-signed URL string
     */
    String generatePresignedUrl(String bucket, String key);

    /**
     * Delete an object from S3.
     *
     * @param bucket bucket name
     * @param key    object key
     */
    void delete(String bucket, String key);
}
