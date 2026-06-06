package com.hospital.medibook.service;

import com.hospital.medibook.constant.Actor;
import com.hospital.medibook.constant.BookingStatus;
import com.hospital.medibook.entity.Booking;
import com.hospital.medibook.entity.BookingEvent;
import com.hospital.medibook.entity.DoctorSchedule;
import com.hospital.medibook.repository.BookingEventRepository;
import com.hospital.medibook.repository.BookingRepository;
import com.hospital.medibook.repository.DoctorScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

    private final BookingRepository bookingRepository;
    private final DoctorScheduleRepository scheduleRepository;
    private final BookingEventRepository eventRepository;

    public SchedulerService(BookingRepository bookingRepository,
                            DoctorScheduleRepository scheduleRepository,
                            BookingEventRepository eventRepository) {
        this.bookingRepository = bookingRepository;
        this.scheduleRepository = scheduleRepository;
        this.eventRepository = eventRepository;
    }

    // Berjalan setiap 60 detik (1 menit)
    @Scheduled(fixedRate = 60000)
    public void cancelUnpaidBookings() {
        LocalDateTime limitTime = LocalDateTime.now().minusMinutes(15);
        List<Booking> expiredBookings = bookingRepository.findByStatusAndCreatedAtBefore(
                BookingStatus.PENDING_PAYMENT, limitTime
        );

        if (!expiredBookings.isEmpty()) {
            log.info("Menemukan {} pendaftaran kedaluwarsa (belum dibayar > 15 menit). Memulai auto-cancel...", expiredBookings.size());
            for (Booking booking : expiredBookings) {
                try {
                    // Diproses satu per satu dalam transaksi terpisah agar kegagalan satu data tidak menggagalkan seluruh antrean
                    processCancellation(booking.getId());
                } catch (Exception e) {
                    log.error("Gagal melakukan auto-cancel untuk booking ID: " + booking.getId(), e);
                }
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processCancellation(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null || booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            return;
        }

        // 1. Pessimistic Lock pada Jadwal Dokter untuk memulihkan kuota dengan aman
        if (booking.getSchedule() != null) {
            DoctorSchedule schedule = scheduleRepository.findByIdForUpdate(booking.getSchedule().getId()).orElse(null);
            if (schedule != null) {
                schedule.setBookedCount(Math.max(0, schedule.getBookedCount() - 1));
                scheduleRepository.save(schedule);
                log.info("Kuota jadwal ID: {} dipulihkan (booked_count: {}).", schedule.getId(), schedule.getBookedCount());
            }
        }

        // 2. Ubah Status Booking ke CANCELLED
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // 3. Catat Audit Event
        BookingEvent event = BookingEvent.builder()
                .booking(booking)
                .status(BookingStatus.CANCELLED.name())
                .eventType("SYSTEM_AUTO_CANCEL")
                .actor(Actor.SYSTEM)
                .detail("Pendaftaran dibatalkan otomatis oleh sistem karena tidak melakukan pembayaran dalam batas waktu 15 menit.")
                .createdAt(LocalDateTime.now())
                .build();
        eventRepository.save(event);

        log.info("Booking Code {} telah berhasil dibatalkan otomatis.", booking.getBookingCode());
    }
}
