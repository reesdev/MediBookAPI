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
        for (Gender gender : Gender.values()) {
            if (gender.value.equalsIgnoreCase(value)) {
                return gender;
            }
        }
        throw new IllegalArgumentException("Unknown gender value: " + value);
    }
}
