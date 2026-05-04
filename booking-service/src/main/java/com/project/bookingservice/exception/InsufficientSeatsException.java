package com.project.bookingservice.exception;

public class InsufficientSeatsException extends RuntimeException {
    public InsufficientSeatsException(String m) { super(m); }
}
