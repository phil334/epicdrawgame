package com.example.simple1.exception.method_argument_not_valid;

@SuppressWarnings("SameReturnValue")
public record MethodArgumentNotValidResponse(String error) {

    public int getStatusCode() {
        return 400;
    }

}
