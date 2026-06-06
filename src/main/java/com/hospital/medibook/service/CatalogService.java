package com.hospital.medibook.service;

import com.hospital.medibook.dto.DoctorResponse;
import com.hospital.medibook.dto.DoctorScheduleResponse;
import com.hospital.medibook.dto.HospitalServiceResponse;
import com.hospital.medibook.entity.Doctor;
import com.hospital.medibook.entity.DoctorSchedule;
import com.hospital.medibook.entity.HospitalService;
import com.hospital.medibook.repository.DoctorRepository;
import com.hospital.medibook.repository.DoctorScheduleRepository;
import com.hospital.medibook.repository.HospitalServiceRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CatalogService {

    private final DoctorRepository doctorRepository;
    private final HospitalServiceRepository serviceRepository;
    private final DoctorScheduleRepository scheduleRepository;

    public CatalogService(DoctorRepository doctorRepository,
                          HospitalServiceRepository serviceRepository,
                          DoctorScheduleRepository scheduleRepository) {
        this.doctorRepository = doctorRepository;
        this.serviceRepository = serviceRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Cacheable(value = "doctors", key = "#specialization + '-' + #search + '-' + #pageable.pageNumber")
    public Page<DoctorResponse> searchDoctors(String specialization, String search, Pageable pageable) {
        Page<Doctor> doctors = doctorRepository.searchDoctors(specialization, search, pageable);
        return doctors.map(this::mapToDoctorResponse);
    }

    @Cacheable(value = "services")
    public List<HospitalServiceResponse> getActiveServices() {
        List<HospitalService> services = serviceRepository.findByIsDeletedFalse();
        return services.stream()
                .map(this::mapToServiceResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "schedules")
    public List<DoctorScheduleResponse> getActiveSchedules() {
        List<DoctorSchedule> schedules = scheduleRepository.findByIsDeletedFalse();
        return schedules.stream()
                .map(this::mapToScheduleResponse)
                .collect(Collectors.toList());
    }

    public DoctorResponse mapToDoctorResponse(Doctor doctor) {
        if (doctor == null) return null;
        return DoctorResponse.builder()
                .id(doctor.getId())
                .fullName(doctor.getFullName())
                .specialization(doctor.getSpecialization())
                .sip(doctor.getSip())
                .phone(doctor.getPhone())
                .email(doctor.getEmail())
                .build();
    }

    public HospitalServiceResponse mapToServiceResponse(HospitalService service) {
        if (service == null) return null;
        return HospitalServiceResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .category(service.getCategory().name())
                .description(service.getDescription())
                .basePrice(service.getBasePrice())
                .build();
    }

    public DoctorScheduleResponse mapToScheduleResponse(DoctorSchedule schedule) {
        if (schedule == null) return null;
        int max = schedule.getMaxPatients();
        int booked = schedule.getBookedCount();
        return DoctorScheduleResponse.builder()
                .id(schedule.getId())
                .doctor(mapToDoctorResponse(schedule.getDoctor()))
                .service(mapToServiceResponse(schedule.getService()))
                .dayOfWeek(schedule.getDayOfWeek())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .maxPatients(max)
                .bookedCount(booked)
                .availableQuota(Math.max(0, max - booked))
                .build();
    }
}
