package com.project.bookingservice.model;

public enum CancellationReason {
    USER_REQUESTED,
    PAYMENT_FAILED,
    FLIGHT_CANCELLED,
    FLIGHT_DELAYED_UNACCEPTABLE,
    OVERBOOKING,
    SYSTEM_ERROR
}
