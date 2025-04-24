package com.sythinian.app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "video_files",
        indexes = {
                @Index(name = "idx_status", columnList = "status")
        })
public class VideoFileModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Original filename is required")
    @Size(max = 512, message = "Filename must be size <= 512")
    @Column(nullable = false)
    private String originalFilename;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    public enum Status {
        NONE,
        PROCESSING,
        AVAILABLE
    }

    public VideoFileModel() {
        this.status = Status.NONE;
    }

    public VideoFileModel(String originalFilename) {
        this.originalFilename = originalFilename;
        this.status = Status.NONE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
} 