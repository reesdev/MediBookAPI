package com.hospital.medibook.repository;

import com.hospital.medibook.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {
    Optional<Booking> findByBookingCode(String bookingCode);
    List<Booking> findByPatientId(String patientId);
    boolean existsByPatientIdAndScheduleIdAndBookingDate(String patientId, String scheduleId, LocalDate bookingDate);
    List<Booking> findByStatusAndCreatedAtBefore(com.hospital.medibook.constant.BookingStatus status, java.time.LocalDateTime time);
    List<Booking> findByDoctorIdAndBookingDateOrderByQueueNumberAsc(String doctorId, LocalDate bookingDate);
    List<Booking> findByDoctorIdAndBookingDateGreaterThanEqualOrderByBookingDateAscQueueNumberAsc(String doctorId, LocalDate bookingDate);
    List<Booking> findByPatientIdOrderByCreatedAtDesc(String patientId);
    
    // Untuk menghitung jumlah pasien per tanggal spesifik (mengabaikan yang dibatalkan)
    int countByScheduleIdAndBookingDateAndStatusNot(String scheduleId, java.time.LocalDate bookingDate, com.hospital.medibook.constant.BookingStatus status);

    // Untuk fitur Time Slot
    boolean existsByScheduleIdAndBookingDateAndBookingTimeAndStatusNot(String scheduleId, java.time.LocalDate bookingDate, java.time.LocalTime bookingTime, com.hospital.medibook.constant.BookingStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT b.bookingTime FROM Booking b WHERE b.schedule.id = :scheduleId AND b.bookingDate = :bookingDate AND b.status != 'CANCELLED'")
    List<java.time.LocalTime> findBookedTimes(@org.springframework.data.repository.query.Param("scheduleId") String scheduleId, @org.springframework.data.repository.query.Param("bookingDate") java.time.LocalDate bookingDate);
}
