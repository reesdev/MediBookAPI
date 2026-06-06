package com.hospital.medibook.controller;

import com.hospital.medibook.controller.api.CatalogApi;
import com.hospital.medibook.dto.DoctorResponse;
import com.hospital.medibook.dto.DoctorScheduleResponse;
import com.hospital.medibook.dto.HospitalServiceResponse;
import com.hospital.medibook.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CatalogController implements CatalogApi {

    private final CatalogService catalogService;

    @Override
    public ResponseEntity<Map<String, Object>> getDoctors(int page, int size, String specialization, String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<DoctorResponse> doctorPage = catalogService.searchDoctors(specialization, search, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("content", doctorPage.getContent());
        response.put("currentPage", doctorPage.getNumber());
        response.put("totalPages", doctorPage.getTotalPages());
        response.put("totalItems", doctorPage.getTotalElements());

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<HospitalServiceResponse>> getServices() {
        List<HospitalServiceResponse> services = catalogService.getActiveServices();
        return ResponseEntity.ok(services);
    }

    @Override
    public ResponseEntity<List<DoctorScheduleResponse>> getSchedules() {
        List<DoctorScheduleResponse> schedules = catalogService.getActiveSchedules();
        return ResponseEntity.ok(schedules);
    }
}
