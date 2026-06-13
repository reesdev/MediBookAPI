package com.hospital.medibook.repository;

import com.hospital.medibook.entity.BookingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingEventRepository extends JpaRepository<BookingEvent, String> {
}
