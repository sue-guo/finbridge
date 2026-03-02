package io.github.sueguo.finbridge.service.impl;

import io.github.sueguo.finbridge.model.dto.FileMetadataDto;
import io.github.sueguo.finbridge.model.entity.FileMetadata;
import io.github.sueguo.finbridge.service.FileMetadataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory implementation of {@link FileMetadataService}.
 * Suitable for development and testing; replace with a DB-backed impl for production.
 */
@Slf4j
@Service
public class InMemoryFileMetadataService implements FileMetadataService {

    private final ConcurrentHashMap<String, FileMetadata> store = new ConcurrentHashMap<>();

    @Override
    public FileMetadata create(FileMetadataDto.CreateRequest request) {
        String id = UUID.randomUUID().toString();
        String s3Key = request.getBucket() + "/" + id + "/" + request.getFilename();

        FileMetadata metadata = FileMetadata.builder()
                .id(id)
                .filename(request.getFilename())
                .contentType(request.getContentType())
                .sizeBytes(request.getSizeBytes())
                .s3Key(s3Key)
                .bucket(request.getBucket())
                .uploadedAt(Instant.now())
                .status(FileMetadata.STATUS_ACTIVE)
                .build();

        store.put(id, metadata);
        log.info("Created file metadata: id={}, filename={}", id, request.getFilename());
        return metadata;
    }

    @Override
    public List<FileMetadata> findAll() {
        return store.values().stream()
                .filter(m -> FileMetadata.STATUS_ACTIVE.equals(m.getStatus()))
                .toList();
    }

    @Override
    public Optional<FileMetadata> findById(String id) {
        return Optional.ofNullable(store.get(id))
                .filter(m -> FileMetadata.STATUS_ACTIVE.equals(m.getStatus()));
    }

    @Override
    public boolean deleteById(String id) {
        FileMetadata metadata = store.get(id);
        if (metadata == null || FileMetadata.STATUS_DELETED.equals(metadata.getStatus())) {
            log.warn("Attempted to delete non-existent or already-deleted record: id={}", id);
            return false;
        }
        metadata.setStatus(FileMetadata.STATUS_DELETED);
        log.info("Soft-deleted file metadata: id={}", id);
        return true;
    }
}

