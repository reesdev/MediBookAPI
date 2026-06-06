CREATE DATABASE IF NOT EXISTS medibook_db;
USE medibook_db;

-- 1. tabel users
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role ENUM('ADMIN', 'PATIENT', 'DOCTOR') NOT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. tabel pasien (menggunakan RESTRICT pada user_id)
CREATE TABLE patients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    nik VARCHAR(20) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    birth_date DATE NOT NULL,
    gender ENUM('Laki-laki', 'Perempuan') NOT NULL,
    address TEXT,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
);

-- 3. tabel dokter (menggunakan RESTRICT pada user_id)
CREATE TABLE doctors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    specialization VARCHAR(100) NOT NULL,
    sip VARCHAR(50) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
);

-- 4. tabel layanan rumah sakit
CREATE TABLE hospital_services (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category ENUM('POLIKLINIK', 'LAB', 'RAD', 'FISIO') NOT NULL,
    description TEXT,
    base_price DECIMAL(12, 2) NOT NULL CHECK (base_price >= 0.0),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0
);

-- 5. tabel jadwal praktek dokter (ditambahkan booked_count untuk optimasi kuota)
CREATE TABLE doctor_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    day_of_week INT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    max_patients INT NOT NULL CHECK (max_patients > 0),
    booked_count INT NOT NULL DEFAULT 0 CHECK (booked_count >= 0),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE RESTRICT,
    FOREIGN KEY (service_id) REFERENCES hospital_services(id) ON DELETE RESTRICT,
    CONSTRAINT uq_doctor_day UNIQUE (doctor_id, day_of_week, start_time)
);

-- 6. tabel transaksi booking (menggunakan ENUM untuk status)
CREATE TABLE bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_code VARCHAR(50) NOT NULL UNIQUE,
    patient_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    doctor_id BIGINT NULL,
    schedule_id BIGINT NULL,
    booking_date DATE NOT NULL,
    queue_number INT NOT NULL,
    status ENUM('PENDING_PAYMENT', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'PENDING_PAYMENT',
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

-- indexing tabel bookings untuk meningkatkan performa query
CREATE INDEX idx_booking_code ON bookings(booking_code);
CREATE INDEX idx_booking_patient ON bookings(patient_id);
CREATE INDEX idx_booking_doctor ON bookings(doctor_id);
CREATE INDEX idx_booking_service ON bookings(service_id);
CREATE INDEX idx_booking_schedule ON bookings(schedule_id);
CREATE INDEX idx_booking_date ON bookings(booking_date);
CREATE INDEX idx_booking_status_date ON bookings(status, created_at);

-- 7. tabel transaksi pembayaran (untuk pencatatan log transaksi finansial)
CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    transaction_code VARCHAR(50) NOT NULL UNIQUE,
    amount DECIMAL(12, 2) NOT NULL CHECK (amount >= 0.0),
    payment_method VARCHAR(50) NOT NULL,
    status ENUM('PENDING', 'SUCCESS', 'FAILED') NOT NULL DEFAULT 'PENDING',
    paid_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE RESTRICT
);

CREATE INDEX idx_transaction_code ON transactions(transaction_code);
CREATE INDEX idx_transaction_booking ON transactions(booking_id);

-- 8. tabel log audit pelacakan status pendaftaran
CREATE TABLE booking_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    actor ENUM('PATIENT', 'DOCTOR', 'ADMIN', 'SYSTEM') NOT NULL,
    detail TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE RESTRICT
);

-- 9. tabel upload file berkas rujukan dan resep digital
CREATE TABLE medical_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(150) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    uploaded_by ENUM('PATIENT', 'DOCTOR') NOT NULL,
    document_type ENUM('REFERRAL_LETTER', 'PRESCRIPTION', 'LAB_RESULT') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE RESTRICT
);

-- 10. tabel rating & ulasan pelayanan dokter/layanan rumah sakit
CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NULL,
    service_id BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    review_text TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE RESTRICT,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE RESTRICT,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE RESTRICT,
    FOREIGN KEY (service_id) REFERENCES hospital_services(id) ON DELETE RESTRICT
);

CREATE INDEX idx_review_doctor ON reviews(doctor_id);
CREATE INDEX idx_review_service ON reviews(service_id);
