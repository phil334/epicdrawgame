package com.example.simple1.exception;

import com.example.simple1.exception.bad_request.BadRequestException;
import com.example.simple1.exception.bad_request.BadRequestResponse;
import com.example.simple1.exception.method_argument_not_valid.MethodArgumentNotValidResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<BadRequestResponse> handleBadRequestException(BadRequestException ex) {
        return ResponseEntity.badRequest().body(new BadRequestResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MethodArgumentNotValidResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> "The field '%s' %s!".formatted(fieldError.getField(), fieldError.getDefaultMessage()))
                .collect(Collectors.joining(" ; "));

        return ResponseEntity.badRequest().body(new MethodArgumentNotValidResponse(errorMessage));
    }

}
