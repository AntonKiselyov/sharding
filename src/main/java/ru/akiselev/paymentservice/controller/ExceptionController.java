package ru.akiselev.paymentservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class ExceptionController {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> handleException(final RuntimeException e) {
        e.printStackTrace();
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "error", HttpStatus.BAD_REQUEST,
                        "message", e.getMessage()
                ));
    }
}
