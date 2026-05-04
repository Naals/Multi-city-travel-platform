package com.project.bookingservice.repository.repository;


import com.project.bookingservice.model.BookingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookingEventRepository extends JpaRepository<BookingEvent, Long> {
    List<BookingEvent> findByBookingIdOrderByOccurredAtAsc(UUID bookingId);
}