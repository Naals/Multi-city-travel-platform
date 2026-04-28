package com.project.authservice.auth.exception;

public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String msg) { super(msg); }
}
