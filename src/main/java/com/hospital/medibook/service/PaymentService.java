package com.hospital.medibook.service;

import com.hospital.medibook.constant.*;
import com.hospital.medibook.dto.PaymentRequest;
import com.hospital.medibook.dto.PaymentResponse;
import com.hospital.medibook.entity.Booking;
import com.hospital.medibook.entity.BookingEvent;
import com.hospital.medibook.entity.Transaction;
import com.hospital.medibook.exception.BadRequestException;
import com.hospital.medibook.exception.ResourceNotFoundException;
import com.hospital.medibook.repository.BookingEventRepository;
import com.hospital.medibook.repository.BookingRepository;
import com.hospital.medibook.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final BookingRepository bookingRepository;
    private final TransactionRepository transactionRepository;
    private final BookingEventRepository eventRepository;

    @Transactional
    public PaymentResponse payBooking(Long bookingId, PaymentRequest request) {
        // 1. Ambil data Booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking tidak ditemukan."));

        // 2. Validasi Status Booking
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new BadRequestException("Booking ini sudah dibayar.");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking ini sudah dibatalkan atau kedaluwarsa.");
        }
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Booking tidak berada dalam status menunggu pembayaran.");
        }

        // 3. Validasi Jumlah Pembayaran
        if (booking.getTotalFee().compareTo(request.getAmount()) != 0) {
            throw new BadRequestException("Jumlah pembayaran tidak sesuai. Diperlukan: " + booking.getTotalFee());
        }

        // 4. Update Status Booking menjadi CONFIRMED
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        // 5. Generate Kode Transaksi Unik: TX-yyyyMMdd-XXXX
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%05d", (int)(Math.random() * 100000));
        String transactionCode = "TX-" + datePart + "-" + randomPart;

        // 6. Simpan Detail Transaksi
        Transaction transaction = Transaction.builder()
                .booking(booking)
                .transactionCode(transactionCode)
                .amount(booking.getTotalFee())
                .paymentMethod(request.getPaymentMethod())
                .status(TransactionStatus.SUCCESS)
                .paidAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);

        // 7. Catat Audit Booking Event
        BookingEvent event = BookingEvent.builder()
                .booking(booking)
                .status(BookingStatus.CONFIRMED.name())
                .eventType("PAYMENT_RECEIVED")
                .actor(Actor.PATIENT)
                .detail("Pembayaran sandbox berhasil diterima melalui " + request.getPaymentMethod())
                .createdAt(LocalDateTime.now())
                .build();
        eventRepository.save(event);

        return PaymentResponse.builder()
                .bookingCode(booking.getBookingCode())
                .status(booking.getStatus().name())
                .paymentTime(transaction.getPaidAt())
                .transactionCode(transaction.getTransactionCode())
                .message("Simulasi pembayaran sandbox berhasil. Pendaftaran Anda terkonfirmasi.")
                .build();
    }
}
