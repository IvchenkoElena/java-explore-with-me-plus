package ru.practicum.request.model;

public enum RequestStatus {
    PENDING,
    CONFIRMED,
    REJECTED,
    CANCELED;

    public static RequestStatus from(String status) {
        switch (status.toUpperCase()) {
            case "PENDING":
                return PENDING;
            case "CONFIRMED":
                return CONFIRMED;
            case "REJECTED":
                return REJECTED;
            case "CANCELED":
                return CANCELED;
            default:
                return null;
        }
    }
}
