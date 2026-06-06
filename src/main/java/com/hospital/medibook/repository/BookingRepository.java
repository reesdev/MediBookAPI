package com.hospital.medibook.repository;

import com.hospital.medibook.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingCode(String bookingCode);
    List<Booking> findByPatientId(Long patientId);
    boolean existsByPatientIdAndScheduleIdAndBookingDate(Long patientId, Long scheduleId, LocalDate bookingDate);
    List<Booking> findByStatusAndCreatedAtBefore(com.hospital.medibook.constant.BookingStatus status, java.time.LocalDateTime time);
    List<Booking> findByDoctorIdAndBookingDateOrderByQueueNumberAsc(Long doctorId, LocalDate bookingDate);
    List<Booking> findByPatientIdOrderByCreatedAtDesc(Long patientId);
}
