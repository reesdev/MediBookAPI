package com.hospital.medibook.repository;

import com.hospital.medibook.entity.DoctorSchedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {

    List<DoctorSchedule> findByDoctorIdAndIsDeletedFalse(Long doctorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM DoctorSchedule s WHERE s.id = :id AND s.isDeleted = false")
    Optional<DoctorSchedule> findByIdForUpdate(@Param("id") Long id);
}
