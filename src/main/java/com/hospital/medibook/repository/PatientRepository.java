package com.hospital.medibook.repository;

import com.hospital.medibook.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, String> {
    Optional<Patient> findByUserId(String userId);
    boolean existsByNik(String nik);
}
