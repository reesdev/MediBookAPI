package com.hospital.medibook.repository;

import com.hospital.medibook.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, String> {
    Optional<Doctor> findByUserId(String userId);
    boolean existsBySip(String sip);

    @Query("SELECT d FROM Doctor d WHERE d.isDeleted = false " +
           "AND (:specialization IS NULL OR d.specialization = :specialization) " +
           "AND (:search IS NULL OR LOWER(d.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(d.specialization) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Doctor> searchDoctors(@Param("specialization") String specialization,
                               @Param("search") String search,
                               Pageable pageable);
}
