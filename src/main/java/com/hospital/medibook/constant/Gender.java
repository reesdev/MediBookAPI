package com.hospital.medibook.constant;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Gender {
    LAKI_LAKI("Laki-laki"),
    PEREMPUAN("Perempuan");

    private final String value;

    Gender(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static Gender fromValue(String value) {
        Gender[] genders = Gender.values();
        for (int i = 0; i < genders.length; i++) {
            if (genders[i].value.equalsIgnoreCase(value)) {
                return genders[i];
            }
        }
        throw new IllegalArgumentException("Unknown gender value: " + value);
    }
}
