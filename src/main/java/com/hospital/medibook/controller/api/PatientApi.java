package com.hospital.medibook.controller.api;

import com.hospital.medibook.dto.BookingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;

@RequestMapping("/api/patient")
@Tag(name = "3. Booking & Pembayaran (PATIENT)", description = "Endpoint untuk pasien: membuat booking, membayar, dan memberikan ulasan. Wajib login sebagai PATIENT.")
public interface PatientApi {

    @Operation(
        summary = "Lihat Semua Booking Milik Pasien",
        description = "Mengembalikan seluruh riwayat booking milik pasien yang sedang login, diurutkan dari yang terbaru."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Berhasil, mengembalikan daftar booking"),
        @ApiResponse(responseCode = "401", description = "Token JWT tidak valid atau tidak dikirim"),
        @ApiResponse(responseCode = "404", description = "Profil pasien tidak ditemukan")
    })
    @GetMapping("/bookings")
    ResponseEntity<List<BookingResponse>> getMyBookings();
}
