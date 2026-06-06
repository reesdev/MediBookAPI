package com.hospital.medibook.controller.api;

import com.hospital.medibook.dto.DoctorScheduleResponse;
import com.hospital.medibook.dto.HospitalServiceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.Map;

@RequestMapping("/api")
@Tag(name = "2. Katalog (Public - Auth Required)", description = "Endpoint untuk melihat daftar dokter, layanan, dan jadwal. Wajib login (semua role).")
public interface CatalogApi {

    @Operation(
        summary = "Lihat Daftar Dokter (dengan Pencarian & Paginasi)",
        description = """
            Mengambil daftar semua dokter aktif. Mendukung filter berdasarkan spesialisasi dan pencarian nama.
            
            - Hasil di-cache di Redis untuk performa optimal.
            - Gunakan `scheduleId` dari endpoint GET /api/schedules untuk booking.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Berhasil, mengembalikan daftar dokter dengan paginasi"),
        @ApiResponse(responseCode = "401", description = "Token JWT tidak valid atau tidak dikirim")
    })
    @GetMapping("/doctors")
    ResponseEntity<Map<String, Object>> getDoctors(
            @Parameter(description = "Nomor halaman (mulai dari 0)", example = "0") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Jumlah data per halaman", example = "10") @RequestParam(value = "size", defaultValue = "10") int size,
            @Parameter(description = "Filter berdasarkan spesialisasi dokter (opsional)", example = "Spesialis Anak") @RequestParam(value = "specialization", required = false) String specialization,
            @Parameter(description = "Kata kunci pencarian nama dokter (opsional)", example = "Hendrawan") @RequestParam(value = "search", required = false) String search);

    @Operation(
        summary = "Lihat Daftar Layanan Rumah Sakit",
        description = """
            Mengambil semua layanan aktif (tidak soft-deleted).
            
            - Hasil di-cache di Redis.
            - Gunakan `id` dari response ini sebagai `serviceId` saat booking.
            - Kategori layanan: `POLIKLINIK` atau `PENUNJANG_MEDIS`.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Berhasil, mengembalikan daftar layanan"),
        @ApiResponse(responseCode = "401", description = "Token JWT tidak valid atau tidak dikirim")
    })
    @GetMapping("/services")
    ResponseEntity<List<HospitalServiceResponse>> getServices();

    @Operation(
        summary = "Lihat Jadwal Dokter Aktif",
        description = """
            Mengambil semua jadwal dokter yang aktif (tidak soft-deleted).
            
            - Hasil di-cache di Redis.
            - Response berisi informasi dokter, layanan, hari, jam, kuota max, dan sisa kuota.
            - `dayOfWeek`: 1=Senin, 2=Selasa, ..., 7=Minggu.
            - Gunakan `id` dari response ini sebagai `scheduleId` saat booking.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Berhasil, mengembalikan daftar jadwal dokter"),
        @ApiResponse(responseCode = "401", description = "Token JWT tidak valid atau tidak dikirim")
    })
    @GetMapping("/schedules")
    ResponseEntity<List<DoctorScheduleResponse>> getSchedules();
}
