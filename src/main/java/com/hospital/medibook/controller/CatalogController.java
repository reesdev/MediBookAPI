package com.hospital.medibook.controller;

import com.hospital.medibook.dto.DoctorResponse;
import com.hospital.medibook.dto.DoctorScheduleResponse;
import com.hospital.medibook.dto.HospitalServiceResponse;
import com.hospital.medibook.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/doctors")
    public ResponseEntity<Map<String, Object>> getDoctors(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "specialization", required = false) String specialization,
            @RequestParam(value = "search", required = false) String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<DoctorResponse> doctorPage = catalogService.searchDoctors(specialization, search, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("content", doctorPage.getContent());
        response.put("currentPage", doctorPage.getNumber());
        response.put("totalPages", doctorPage.getTotalPages());
        response.put("totalItems", doctorPage.getTotalElements());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/services")
    public ResponseEntity<List<HospitalServiceResponse>> getServices() {
        List<HospitalServiceResponse> services = catalogService.getActiveServices();
        return ResponseEntity.ok(services);
    }

    @GetMapping("/schedules")
    public ResponseEntity<List<DoctorScheduleResponse>> getSchedules() {
        List<DoctorScheduleResponse> schedules = catalogService.getActiveSchedules();
        return ResponseEntity.ok(schedules);
    }
}
