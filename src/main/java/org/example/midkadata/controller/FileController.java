package org.example.midkadata.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.midkadata.entity.FileEntity;
import org.example.midkadata.repository.FileRepository;
import org.example.midkadata.service.FileValidationService;
import org.example.midkadata.service.MinioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileValidationService fileValidationService;
    private final MinioService minioService;
    private final FileRepository fileRepository;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            fileValidationService.validateFile(file);

            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            minioService.uploadFile(file, uniqueFileName);

            FileEntity fileEntity = new FileEntity();
            fileEntity.setFileName(uniqueFileName);
            fileEntity.setOriginalFileName(originalFilename);
            fileEntity.setContentType(file.getContentType());
            fileEntity.setFileSize(file.getSize());
            fileEntity = fileRepository.save(fileEntity);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Файл успешно загружен");
            response.put("fileId", fileEntity.getId());
            response.put("fileName", fileEntity.getOriginalFileName());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Validation error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Error uploading file: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Ошибка при загрузке файла: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        return ResponseEntity.ok(response);
    }
}

