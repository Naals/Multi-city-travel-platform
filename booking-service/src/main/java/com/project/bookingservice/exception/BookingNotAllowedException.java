package com.project.bookingservice.exception;

public class BookingNotAllowedException extends RuntimeException {
    public BookingNotAllowedException(String m) { super(m); }
}
