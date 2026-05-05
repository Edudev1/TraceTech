package com.tracetech.eah.tracetecheah.web.service;

import com.tracetech.eah.tracetecheah.common.entity.Comment;
import com.tracetech.eah.tracetecheah.common.entity.CommentAttachment;
import com.tracetech.eah.tracetecheah.common.repository.CommentAttachmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
public class CommentAttachmentStorageService {

    private final Path commentsDir;
    private final CommentAttachmentRepository attachmentRepository;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    public CommentAttachmentStorageService(
            @Value("${app.upload.comments-dir}") String commentsDir,
            CommentAttachmentRepository attachmentRepository) {

        this.commentsDir = Paths.get(commentsDir).toAbsolutePath().normalize();
        this.attachmentRepository = attachmentRepository;
    }

    public CommentAttachment store(MultipartFile file, Comment comment) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Solo se permiten imágenes JPG, PNG, WEBP o GIF.");
        }

        Files.createDirectories(commentsDir);

        String originalFilename = file.getOriginalFilename() != null
                ? file.getOriginalFilename()
                : "imagen";

        String extension = getExtension(originalFilename);
        String storedFilename = UUID.randomUUID() + extension;

        Path destination = commentsDir.resolve(storedFilename).normalize();

        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        CommentAttachment attachment = new CommentAttachment();
        attachment.setComment(comment);
        attachment.setOriginalFilename(originalFilename);
        attachment.setStoredFilename(storedFilename);
        attachment.setContentType(contentType);
        attachment.setSizeBytes(file.getSize());

        return attachmentRepository.save(attachment);
    }

    public Path load(CommentAttachment attachment) {
        return commentsDir.resolve(attachment.getStoredFilename()).normalize();
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex == -1) {
            return "";
        }

        return filename.substring(dotIndex);
    }
}