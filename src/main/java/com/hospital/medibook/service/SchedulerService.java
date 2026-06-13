package com.hospital.medibook.service;

import com.hospital.medibook.constant.Actor;
import com.hospital.medibook.constant.BookingStatus;
import com.hospital.medibook.entity.Booking;
import com.hospital.medibook.entity.BookingEvent;
import com.hospital.medibook.entity.DoctorSchedule;
import com.hospital.medibook.repository.BookingEventRepository;
import com.hospital.medibook.repository.BookingRepository;
import com.hospital.medibook.repository.DoctorScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

    private final BookingRepository bookingRepository;
    private final DoctorScheduleRepository scheduleRepository;
    private final BookingEventRepository eventRepository;
    private final ApplicationContext applicationContext;

    // Berjalan setiap 60 detik (1 menit)
    @Scheduled(fixedRate = 60000)
    public void cancelUnpaidBookings() {
        LocalDateTime limitTime = LocalDateTime.now().minusMinutes(1);
        List<Booking> expiredBookings = bookingRepository.findByStatusAndCreatedAtBefore(
                BookingStatus.PENDING_PAYMENT, limitTime
        );

        if (!expiredBookings.isEmpty()) {
            log.info("Menemukan {} pendaftaran kedaluwarsa (belum dibayar > 1 menit). Memulai auto-cancel...", expiredBookings.size());
            SchedulerService proxy = applicationContext.getBean(SchedulerService.class);
            for (int i = 0; i < expiredBookings.size(); i++) {
                Booking booking = expiredBookings.get(i);
                try {
                    // Diproses satu per satu dalam transaksi terpisah via proxy agar @Transactional berjalan
                    proxy.processCancellation(booking.getId());
                } catch (Exception e) {
                    log.error("Gagal melakukan auto-cancel untuk booking ID: " + booking.getId(), e);
                }
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processCancellation(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null || booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            return;
        }

        // Kunci data jadwal untuk mengembalikan kuota
        if (booking.getSchedule() != null) {
            DoctorSchedule schedule = scheduleRepository.findByIdForUpdate(booking.getSchedule().getId()).orElse(null);
            if (schedule != null) {
                schedule.setBookedCount(Math.max(0, schedule.getBookedCount() - 1));
                scheduleRepository.save(schedule);
                log.info("Kuota jadwal ID: {} dipulihkan (booked_count: {}).", schedule.getId(), schedule.getBookedCount());
            }
        }

        // Ubah status menjadi batal otomatis
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Rekam log pembatalan
        BookingEvent event = BookingEvent.builder()
                .booking(booking)
                .status(BookingStatus.CANCELLED)
                .eventType(com.hospital.medibook.constant.EventType.AUTO_CANCELLED)
                .actor(Actor.SYSTEM)
                .detail("Pendaftaran dibatalkan otomatis oleh sistem karena tidak melakukan pembayaran dalam batas waktu 1 menit.")
                .createdAt(LocalDateTime.now())
                .build();
        eventRepository.save(event);

        log.info("Booking Code {} telah berhasil dibatalkan otomatis.", booking.getBookingCode());
    }
}
