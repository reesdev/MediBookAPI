package com.hospital.medibook.controller;

import com.hospital.medibook.dto.DoctorResponse;
import com.hospital.medibook.dto.DoctorScheduleResponse;
import com.hospital.medibook.dto.HospitalServiceResponse;
import com.hospital.medibook.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "2. Katalog (Public - Auth Required)", description = "Endpoint untuk melihat daftar dokter, layanan, dan jadwal. Wajib login (semua role).")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

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
    public ResponseEntity<Map<String, Object>> getDoctors(
            @Parameter(description = "Nomor halaman (mulai dari 0)", example = "0") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Jumlah data per halaman", example = "10") @RequestParam(value = "size", defaultValue = "10") int size,
            @Parameter(description = "Filter berdasarkan spesialisasi dokter (opsional)", example = "Spesialis Anak") @RequestParam(value = "specialization", required = false) String specialization,
            @Parameter(description = "Kata kunci pencarian nama dokter (opsional)", example = "Hendrawan") @RequestParam(value = "search", required = false) String search) {

        Pageable pageable = PageRequest.of(page, size);
        Page<DoctorResponse> doctorPage = catalogService.searchDoctors(specialization, search, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("content", doctorPage.getContent());
        response.put("currentPage", doctorPage.getNumber());
        response.put("totalPages", doctorPage.getTotalPages());
        response.put("totalItems", doctorPage.getTotalElements());

        return ResponseEntity.ok(response);
    }

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
    public ResponseEntity<List<HospitalServiceResponse>> getServices() {
        List<HospitalServiceResponse> services = catalogService.getActiveServices();
        return ResponseEntity.ok(services);
    }

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
    public ResponseEntity<List<DoctorScheduleResponse>> getSchedules() {
        List<DoctorScheduleResponse> schedules = catalogService.getActiveSchedules();
        return ResponseEntity.ok(schedules);
    }
}
