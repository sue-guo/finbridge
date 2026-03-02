package io.github.sueguo.finbridge.model.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents metadata of a file stored in S3.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadata {

    private String id;
    private String filename;
    private String contentType;
    private long sizeBytes;
    private String s3Key;
    private String bucket;
    private Instant uploadedAt;
    private String status;

    public static final String STATUS_ACTIVE   = "ACTIVE";
    public static final String STATUS_DELETED  = "DELETED";
}
