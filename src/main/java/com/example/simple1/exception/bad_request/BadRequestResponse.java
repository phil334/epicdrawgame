package com.example.simple1.exception.bad_request;

@SuppressWarnings({"unused", "SameReturnValue"})
public record BadRequestResponse(String error) {

    public int getStatusCode() {
        return 400;
    }
}
