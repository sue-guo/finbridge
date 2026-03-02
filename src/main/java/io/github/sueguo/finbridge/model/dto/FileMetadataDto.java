package io.github.sueguo.finbridge.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTOs for FileMetadata API requests and responses.
 */
public final class FileMetadataDto {

    private FileMetadataDto() {}

    // ── Request ──────────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {

        @NotBlank(message = "filename must not be blank")
        private String filename;

        @NotBlank(message = "contentType must not be blank")
        private String contentType;

        @NotNull
        @Positive(message = "sizeBytes must be positive")
        private Long sizeBytes;

        @NotBlank(message = "bucket must not be blank")
        private String bucket;
    }

    // ── Response ─────────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String filename;
        private String contentType;
        private long sizeBytes;
        private String s3Key;
        private String bucket;
        private Instant uploadedAt;
        private String status;
    }

    // ── Error ─────────────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorResponse {
        private int status;
        private String error;
        private String message;
        private Instant timestamp;
    }
}

