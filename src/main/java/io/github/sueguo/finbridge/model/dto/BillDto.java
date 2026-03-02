package io.github.sueguo.finbridge.model.dto;

import io.github.sueguo.finbridge.model.entity.BillSource;
import io.github.sueguo.finbridge.model.entity.BillStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTOs for the Bill API.
 */
public final class BillDto {

    private BillDto() {}

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadResponse {
        private Long billId;
        private BillSource source;
        private String billMonth;
        private BillStatus status;
        private String s3Key;
        private Instant uploadedAt;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillSummary {
        private Long id;
        private BillSource source;
        private String billMonth;
        private BillStatus status;
        private Integer transactionCount;
        private Instant uploadedAt;
        private Instant processedAt;
    }

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

