package io.github.sueguo.finbridge.route;

import io.github.sueguo.finbridge.model.dto.FileMetadataDto;
import io.github.sueguo.finbridge.model.entity.FileMetadata;
import io.github.sueguo.finbridge.service.FileMetadataService;
import lombok.RequiredArgsConstructor;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Apache Camel REST DSL route definitions for file metadata endpoints.
 *
 * <p>Endpoints:
 * <pre>
 *   GET    /api/files        - list all active files
 *   GET    /api/files/{id}   - get a single file by ID
 *   POST   /api/files        - create file metadata
 *   DELETE /api/files/{id}   - soft-delete a file
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class FileMetadataRoute extends RouteBuilder {

    private final FileMetadataService fileMetadataService;

    @Override
    public void configure() {
        restConfiguration()
                .component("servlet")
                .bindingMode(RestBindingMode.json)
                .dataFormatProperty("prettyPrint", "true")
                .enableCORS(true)
                .contextPath("/api")
                .apiContextPath("/api-doc");

        rest("/files")
                .get()
                .description("List all active file metadata records")
                .produces(MediaType.APPLICATION_JSON_VALUE)
                .to("direct:listFiles")
                .get("/{id}")
                .description("Get file metadata by ID")
                .produces(MediaType.APPLICATION_JSON_VALUE)
                .to("direct:getFileById")
                .post()
                .description("Create new file metadata")
                .consumes(MediaType.APPLICATION_JSON_VALUE)
                .produces(MediaType.APPLICATION_JSON_VALUE)
                .type(FileMetadataDto.CreateRequest.class)
                .to("direct:createFile")
                .delete("/{id}")
                .description("Soft-delete file metadata by ID")
                .to("direct:deleteFile");

        from("direct:listFiles")
                .routeId("list-files")
                .log("Listing all files")
                .process(exchange -> {
                    List<FileMetadata> files = fileMetadataService.findAll();
                    exchange.getMessage().setBody(files);
                });

        from("direct:getFileById")
                .routeId("get-file-by-id")
                .log("Getting file: id=${header.id}")
                .process(exchange -> {
                    String id = exchange.getMessage().getHeader("id", String.class);
                    Optional<FileMetadata> result = fileMetadataService.findById(id);
                    if (result.isPresent()) {
                        exchange.getMessage().setBody(result.get());
                    } else {
                        exchange.getMessage().setBody(buildError(404, "Not Found", "File not found: " + id));
                        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
                    }
                });

        from("direct:createFile")
                .routeId("create-file")
                .log("Creating file metadata")
                .process(exchange -> {
                    FileMetadataDto.CreateRequest request =
                            exchange.getMessage().getBody(FileMetadataDto.CreateRequest.class);
                    FileMetadata created = fileMetadataService.create(request);
                    exchange.getMessage().setBody(created);
                    exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 201);
                });

        from("direct:deleteFile")
                .routeId("delete-file")
                .log("Deleting file: id=${header.id}")
                .process(exchange -> {
                    String id = exchange.getMessage().getHeader("id", String.class);
                    boolean deleted = fileMetadataService.deleteById(id);
                    if (deleted) {
                        exchange.getMessage().setBody(null);
                        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 204);
                    } else {
                        exchange.getMessage().setBody(buildError(404, "Not Found", "File not found: " + id));
                        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
                    }
                });
    }

    private FileMetadataDto.ErrorResponse buildError(int status, String error, String message) {
        return FileMetadataDto.ErrorResponse.builder()
                .status(status).error(error).message(message).timestamp(Instant.now()).build();
    }
}

