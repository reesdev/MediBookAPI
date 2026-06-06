package com.hospital.medibook.dto;

import lombok.*;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorScheduleResponse {
    private Long id;
    private DoctorResponse doctor;
    private HospitalServiceResponse service;
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer maxPatients;
    private Integer bookedCount;
    private Integer availableQuota;
}
