package com.hospital.medibook.repository;

import com.hospital.medibook.entity.MedicalDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicalDocumentRepository extends JpaRepository<MedicalDocument, Long> {
}
