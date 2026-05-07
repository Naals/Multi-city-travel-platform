package com.project.paymentservice.exception;

public class RefundNotAllowedException extends RuntimeException {
    public RefundNotAllowedException(String msg) { super(msg); }
}
