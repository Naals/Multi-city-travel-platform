package com.project.reviewservice.exception;

public class ReviewAlreadySubmittedException extends RuntimeException {
    public ReviewAlreadySubmittedException(String msg) { super(msg); }
}
