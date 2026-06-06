package com.hospital.medibook.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorResponse {
    private Long id;
    private String fullName;
    private String specialization;
    private String sip;
    private String phone;
    private String email;
}
