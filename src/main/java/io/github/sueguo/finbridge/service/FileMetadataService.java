package io.github.sueguo.finbridge.service;


import io.github.sueguo.finbridge.model.dto.FileMetadataDto;
import io.github.sueguo.finbridge.model.entity.FileMetadata;

import java.util.List;
import java.util.Optional;

/**
 * Service contract for managing file metadata.
 */
public interface FileMetadataService {

    /**
     * Create and persist new file metadata.
     *
     * @param request creation request DTO
     * @return created {@link FileMetadata}
     */
    FileMetadata create(FileMetadataDto.CreateRequest request);

    /**
     * Retrieve all active file metadata records.
     *
     * @return list of active records
     */
    List<FileMetadata> findAll();

    /**
     * Find a single record by its ID.
     *
     * @param id record identifier
     * @return an {@link Optional} containing the record, or empty if not found
     */
    Optional<FileMetadata> findById(String id);

    /**
     * Soft-delete a record by ID.
     *
     * @param id record identifier
     * @return {@code true} if deleted, {@code false} if not found
     */
    boolean deleteById(String id);
}
