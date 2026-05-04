package com.project.bookingservice.exception;

public class BookingAccessDeniedException extends RuntimeException {
    public BookingAccessDeniedException(String m) { super(m); }
}