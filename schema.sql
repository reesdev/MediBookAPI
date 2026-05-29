CREATE DATABASE IF NOT EXISTS medibook_db;
USE medibook_db;

-- tabel users untuk menampung data login pasien, dokter dan admin
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'PATIENT', 'DOCTOR')),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- tabel pasien untuk data detail rekam medis pasien
CREATE TABLE patients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    nik VARCHAR(20) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    birth_date DATE NOT NULL,
    gender VARCHAR(10) NOT NULL CHECK (gender IN ('Laki-laki', 'Perempuan')),
    address TEXT,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- tabel dokter spesialis
CREATE TABLE doctors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    specialization VARCHAR(100) NOT NULL,
    sip VARCHAR(50) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- tabel layanan poliklinik dan pemeriksaan penunjang (lab, rad, fisio)
CREATE TABLE hospital_services (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL CHECK (category IN ('POLIKLINIK', 'LAB', 'RAD', 'FISIO')),
    description TEXT,
    base_price DECIMAL(12, 2) NOT NULL CHECK (base_price >= 0.0),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0
);

-- tabel jadwal praktek dokter (junction many-to-many dokter & layanan)
CREATE TABLE doctor_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    day_of_week INT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    max_patients INT NOT NULL CHECK (max_patients > 0),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE RESTRICT,
    FOREIGN KEY (service_id) REFERENCES hospital_services(id) ON DELETE RESTRICT,
    CONSTRAINT uq_doctor_day UNIQUE (doctor_id, day_of_week, start_time)
);

-- tabel transaksi booking/pendaftaran pasien
CREATE TABLE bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_code VARCHAR(50) NOT NULL UNIQUE,
    patient_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    doctor_id BIGINT NULL,
    schedule_id BIGINT NULL,
    booking_date DATE NOT NULL,
    queue_number INT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT' CHECK (status IN ('PENDING_PAYMENT', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    complaint TEXT,
    total_fee DECIMAL(12, 2) NOT NULL CHECK (total_fee >= 0.0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE RESTRICT,
    FOREIGN KEY (service_id) REFERENCES hospital_services(id) ON DELETE RESTRICT,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE RESTRICT,
    FOREIGN KEY (schedule_id) REFERENCES doctor_schedules(id) ON DELETE RESTRICT,
    CONSTRAINT uq_patient_schedule_date UNIQUE (patient_id, schedule_id, booking_date)
);

-- index status booking untuk kebutuhan scheduler auto-cancel
CREATE INDEX idx_booking_status_date ON bookings(status, created_at);

-- tabel log audit pelacakan status pendaftaran
CREATE TABLE booking_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    actor VARCHAR(20) NOT NULL CHECK (actor IN ('PATIENT', 'DOCTOR', 'ADMIN', 'SYSTEM')),
    detail TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
);

-- tabel upload file berkas rujukan dan resep digital
CREATE TABLE medical_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(150) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    uploaded_by VARCHAR(20) NOT NULL CHECK (uploaded_by IN ('PATIENT', 'DOCTOR')),
    document_type VARCHAR(50) NOT NULL CHECK (document_type IN ('REFERRAL_LETTER', 'PRESCRIPTION', 'LAB_RESULT')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
);
