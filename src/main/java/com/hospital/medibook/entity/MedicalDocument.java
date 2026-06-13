package com.hospital.medibook.entity;

import com.hospital.medibook.constant.DocumentType;
import com.hospital.medibook.constant.UploadedBy;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "medical_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, foreignKey = @ForeignKey(name = "fk_documents_booking"))
    private Booking booking;

    @Column(name = "file_path", nullable = false, length = 255)
    private String filePath;

    @Column(name = "original_file_name", nullable = false, length = 150)
    private String originalFileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "uploaded_by", nullable = false, columnDefinition = "ENUM('PATIENT', 'DOCTOR')")
    private UploadedBy uploadedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, columnDefinition = "ENUM('REFERRAL_LETTER', 'PRESCRIPTION', 'LAB_RESULT')")
    private DocumentType documentType;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
}
