package com.pasystem;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/file_storage")
public class CustomFolderController {

    private final Path customDir = Paths.get(System.getProperty("user.dir"), "custom");

    private void ensureCustomDirExists() throws IOException {
        if (!Files.exists(customDir)) {
            Files.createDirectories(customDir);
        }
    }

    private Path normalizeAndValidate(String filename) {
        Path resolved = customDir.resolve(filename).normalize();
        if (!resolved.startsWith(customDir)) {
            throw new IllegalArgumentException("Invalid filename escape attempt");
        }
        return resolved;
    }

    @GetMapping("/stored/{filename}")
    public ResponseEntity<Resource> getStoredFile(@PathVariable String filename) {
        try {
            Path filePath = normalizeAndValidate(filename);
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/list-stored")
    public ResponseEntity<List<String>> listStoredFiles() {
        try {
            if (!Files.exists(customDir) || !Files.isDirectory(customDir)) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            List<String> files;
            try (var stream = Files.list(customDir)) {
                files = stream
                        .filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .collect(Collectors.toList());
            }
            return ResponseEntity.ok(files);

        } catch (IOException e) {
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @PostMapping("/upload-stored")
    public ResponseEntity<String> uploadStoredFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("filename") String filename) {

        if (file.isEmpty() || filename == null || filename.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File and filename are required");
        }

        try {
            ensureCustomDirExists();
            Path destinationPath = normalizeAndValidate(filename);

            Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
            return ResponseEntity.ok("Uploaded " + destinationPath.getFileName().toString());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid filename");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server-side failure writing file");
        }
    }
}
