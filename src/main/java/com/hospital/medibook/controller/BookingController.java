package com.hospital.medibook.controller;

import com.hospital.medibook.dto.*;
import com.hospital.medibook.service.BookingService;
import com.hospital.medibook.service.PaymentService;
import com.hospital.medibook.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.beans.PropertyEditorSupport;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "3. Booking & Pembayaran (PATIENT)", description = "Endpoint untuk pasien: membuat booking, membayar, dan memberikan ulasan. Wajib login sebagai PATIENT.")
public class BookingController {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final ReviewService reviewService;

    public BookingController(BookingService bookingService,
                             PaymentService paymentService,
                             ReviewService reviewService) {
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.reviewService = reviewService;
    }

    /**
     * Menangani kasus Swagger mengirim string kosong "" untuk field referralFile.
     * Tanpa ini, Spring akan gagal konversi "" → MultipartFile dan throw 422.
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(MultipartFile.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                // string kosong atau null → set referralFile ke null (opsional, skip)
                setValue(null);
            }
        });
    }

    @Operation(
        summary = "Buat Booking Baru (Reservasi Antrean)",
        description = """
            Pasien membuat booking/reservasi antrean ke dokter.
            
            **Penting**: Request harus menggunakan `Content-Type: multipart/form-data`.
            
            - `serviceId`, `doctorId`, `scheduleId`, `bookingDate`, `complaint` adalah field **wajib**.
            - `referralFile` adalah **OPSIONAL** — boleh dikosongkan jika tidak ada surat rujukan.
            - `bookingDate` tidak boleh di masa lalu.
            - Sistem menggunakan Redis Idempotency Lock untuk mencegah double submission.
            - Kuota dokter akan berkurang otomatis dengan Pessimistic Lock.
            - Booking akan otomatis dibatalkan jika pembayaran tidak dilakukan dalam 15 menit.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Booking berhasil dibuat, status PENDING_PAYMENT"),
        @ApiResponse(responseCode = "400", description = "Validasi bisnis gagal (tanggal masa lalu, jadwal tidak cocok, dll)"),
        @ApiResponse(responseCode = "401", description = "Token JWT tidak valid atau tidak dikirim"),
        @ApiResponse(responseCode = "403", description = "Role tidak memiliki akses (bukan PATIENT)"),
        @ApiResponse(responseCode = "404", description = "Pasien, jadwal, dokter, atau layanan tidak ditemukan"),
        @ApiResponse(responseCode = "409", description = "Kuota dokter untuk jadwal ini sudah penuh"),
        @ApiResponse(responseCode = "415", description = "Content-Type tidak didukung. Gunakan multipart/form-data"),
        @ApiResponse(responseCode = "422", description = "Validasi form gagal (field wajib kosong)")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        content = @Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema = @Schema(implementation = BookingRequest.class)
        )
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BookingResponse> createBooking(@Valid @ModelAttribute BookingRequest request) {
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "Simulasi Pembayaran Sandbox",
        description = """
            Pasien melakukan pembayaran untuk booking yang berstatus `PENDING_PAYMENT`.
            
            - Booking harus dalam status `PENDING_PAYMENT` (belum expired).
            - Jumlah `amount` harus persis sama dengan `totalFee` di booking.
            - Setelah berhasil, status booking berubah menjadi `CONFIRMED`.
            - Log transaksi tersimpan di tabel `transactions`.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pembayaran berhasil, status booking menjadi CONFIRMED"),
        @ApiResponse(responseCode = "400", description = "Jumlah pembayaran tidak sesuai, atau booking sudah dibayar/dibatalkan"),
        @ApiResponse(responseCode = "401", description = "Token JWT tidak valid atau tidak dikirim"),
        @ApiResponse(responseCode = "403", description = "Role tidak memiliki akses (bukan PATIENT)"),
        @ApiResponse(responseCode = "404", description = "Booking tidak ditemukan"),
        @ApiResponse(responseCode = "422", description = "Validasi form gagal")
    })
    @PostMapping("/{id}/pay")
    public ResponseEntity<PaymentResponse> payBooking(
            @Parameter(description = "ID Booking yang akan dibayar", required = true) @PathVariable("id") Long bookingId,
            @Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.payBooking(bookingId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Submit Ulasan & Rating Dokter",
        description = """
            Pasien memberikan ulasan dan rating bintang setelah booking selesai.
            
            - Booking harus dalam status `COMPLETED` (dokter sudah selesai memeriksa).
            - Ulasan hanya bisa dikirim satu kali per booking.
            - Rating valid: 1–5 bintang.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Ulasan berhasil dikirim"),
        @ApiResponse(responseCode = "400", description = "Booking bukan milik pasien ini, belum COMPLETED, atau ulasan sudah dikirim"),
        @ApiResponse(responseCode = "401", description = "Token JWT tidak valid atau tidak dikirim"),
        @ApiResponse(responseCode = "403", description = "Role tidak memiliki akses (bukan PATIENT)"),
        @ApiResponse(responseCode = "404", description = "Booking tidak ditemukan"),
        @ApiResponse(responseCode = "422", description = "Validasi form gagal (rating wajib 1–5)")
    })
    @PostMapping("/{id}/reviews")
    public ResponseEntity<ReviewResponse> submitReview(
            @Parameter(description = "ID Booking yang akan diulas", required = true) @PathVariable("id") Long bookingId,
            @Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService.submitReview(bookingId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
