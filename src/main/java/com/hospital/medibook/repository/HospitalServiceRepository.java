package com.hospital.medibook.repository;

import com.hospital.medibook.entity.HospitalService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HospitalServiceRepository extends JpaRepository<HospitalService, Long> {
    List<HospitalService> findByIsDeletedFalse();
}
