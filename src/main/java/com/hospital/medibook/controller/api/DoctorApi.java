package com.hospital.medibook.controller.api;

import com.hospital.medibook.dto.BookingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RequestMapping("/api/doctor")
@Tag(name = "5. Layanan Dokter (DOCTOR Only)", description = "Endpoint untuk dokter: melihat antrean pasien hari ini dan memperbarui status pemeriksaan. Wajib login sebagai DOCTOR.")
public interface DoctorApi {

    @Operation(
        summary = "Lihat Daftar Antrean Pasien Hari Ini",
        description = """
            Dokter melihat daftar semua booking pasien untuk hari ini, diurutkan berdasarkan nomor antrean.
            
            - Hanya menampilkan booking dengan tanggal sama dengan hari ini (`bookingDate = today`).
            - Dokter hanya bisa melihat booking miliknya sendiri (berdasarkan JWT login).
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Berhasil, mengembalikan daftar antrean hari ini (bisa kosong)"),
        @ApiResponse(responseCode = "401", description = "Token JWT tidak valid atau tidak dikirim"),
        @ApiResponse(responseCode = "403", description = "Role tidak memiliki akses (bukan DOCTOR)"),
        @ApiResponse(responseCode = "404", description = "Profil dokter tidak ditemukan untuk user yang login")
    })
    @GetMapping("/bookings")
    ResponseEntity<List<BookingResponse>> getTodayBookings(
            @RequestParam(value = "date", required = false) 
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date);

    @Operation(
        summary = "Update Status Pemeriksaan Pasien",
        description = """
            Dokter mengubah status pemeriksaan pasien. Hanya 2 transisi yang valid:
            
            1. `CONFIRMED` → `IN_PROGRESS` (dokter mulai memeriksa)
            2. `IN_PROGRESS` → `COMPLETED` (dokter selesai memeriksa)
            
            Setiap perubahan status dicatat di tabel `booking_events` sebagai audit trail.
            Setelah `COMPLETED`, pasien dapat memberikan ulasan via POST /api/bookings/{id}/reviews.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status berhasil diubah"),
        @ApiResponse(responseCode = "400", description = "Transisi status tidak valid, atau booking bukan milik dokter ini"),
        @ApiResponse(responseCode = "401", description = "Token JWT tidak valid atau tidak dikirim"),
        @ApiResponse(responseCode = "403", description = "Role tidak memiliki akses (bukan DOCTOR)"),
        @ApiResponse(responseCode = "404", description = "Booking atau profil dokter tidak ditemukan")
    })
    @PutMapping("/bookings/{id}/status")
    ResponseEntity<BookingResponse> updateBookingStatus(
            @Parameter(description = "ID Booking yang akan diupdate statusnya", required = true) @PathVariable("id") Long bookingId,
            @Parameter(description = "Status baru: IN_PROGRESS atau COMPLETED", required = true, example = "IN_PROGRESS") @RequestParam("status") String status);
}
