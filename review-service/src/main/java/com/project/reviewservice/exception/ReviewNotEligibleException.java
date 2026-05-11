package com.project.reviewservice.exception;

public class ReviewNotEligibleException extends RuntimeException {
    public ReviewNotEligibleException(String msg) { super(msg); }
}