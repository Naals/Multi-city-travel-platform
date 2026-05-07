package com.project.paymentservice.exception;

public class DuplicatePaymentException extends RuntimeException {
    public DuplicatePaymentException(String msg) { super(msg); }
}
