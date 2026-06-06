package com.hospital.medibook.controller.api;

import com.hospital.medibook.dto.DoctorCreateRequest;
import com.hospital.medibook.dto.ScheduleCreateRequest;
import com.hospital.medibook.dto.ServiceCreateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.Map;

@RequestMapping("/api")
@Tag(name = "4. Master Data (ADMIN Only)", description = "Endpoint manajemen master data: tambah dokter, layanan, dan jadwal. Wajib login sebagai ADMIN.")
public interface AdminApi {

    @Operation(
        summary = "Tambah Profil Dokter Baru",
        description = """
            Admin menambahkan profil dokter baru.
            
            - `userId` harus merujuk ke user yang sudah ada dengan role `DOCTOR`.
            - SIP (Surat Izin Praktek) harus unik.
            - Untuk membuat user berdokter, daftarkan user DOCTOR terlebih dahulu via seeder atau endpoint internal.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Profil dokter berhasil dibuat"),
        @ApiResponse(responseCode = "400", description = "User tidak ditemukan, role bukan DOCTOR, atau SIP sudah terdaftar"),
        @ApiResponse(responseCode = "401", description = "Token JWT tidak valid atau tidak dikirim"),
        @ApiResponse(responseCode = "403", description = "Role tidak memiliki akses (bukan ADMIN)"),
        @ApiResponse(responseCode = "422", description = "Validasi form gagal")
    })
    @PostMapping("/doctors")
    ResponseEntity<Map<String, Object>> createDoctor(@Valid @RequestBody DoctorCreateRequest request);

    @Operation(
        summary = "Tambah Layanan Rumah Sakit Baru",
        description = """
            Admin menambahkan layanan rumah sakit baru ke katalog.
            
            - `category` harus berupa salah satu: `POLIKLINIK` atau `PENUNJANG_MEDIS` (case-insensitive).
            - `basePrice` adalah harga dasar layanan dalam Rupiah.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Layanan berhasil dibuat"),
        @ApiResponse(responseCode = "400", description = "Kategori tidak valid"),
        @ApiResponse(responseCode = "401", description = "Token JWT tidak valid atau tidak dikirim"),
        @ApiResponse(responseCode = "403", description = "Role tidak memiliki akses (bukan ADMIN)"),
        @ApiResponse(responseCode = "422", description = "Validasi form gagal")
    })
    @PostMapping("/services")
    ResponseEntity<Map<String, Object>> createService(@Valid @RequestBody ServiceCreateRequest request);

    @Operation(
        summary = "Tambah Jadwal Dokter Baru",
        description = """
            Admin menambahkan jadwal praktik dokter.
            
            - `doctorId` harus merujuk ke profil dokter yang sudah ada.
            - `serviceId` harus merujuk ke layanan yang sudah ada.
            - `dayOfWeek`: 1=Senin, 2=Selasa, 3=Rabu, 4=Kamis, 5=Jumat, 6=Sabtu, 7=Minggu.
            - `startTime` dan `endTime` menggunakan format `HH:mm:ss` (contoh: `08:00:00`).
            - `maxPatients` adalah kuota maksimal pasien per jadwal.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Jadwal dokter berhasil dibuat"),
        @ApiResponse(responseCode = "401", description = "Token JWT tidak valid atau tidak dikirim"),
        @ApiResponse(responseCode = "403", description = "Role tidak memiliki akses (bukan ADMIN)"),
        @ApiResponse(responseCode = "404", description = "Dokter atau layanan tidak ditemukan"),
        @ApiResponse(responseCode = "422", description = "Validasi form gagal")
    })
    @PostMapping("/schedules")
    ResponseEntity<Map<String, Object>> createSchedule(@Valid @RequestBody ScheduleCreateRequest request);
}
