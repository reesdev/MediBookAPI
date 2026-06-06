package com.hospital.medibook.repository;

import com.hospital.medibook.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByUserId(Long userId);
    boolean existsBySip(String sip);
}
