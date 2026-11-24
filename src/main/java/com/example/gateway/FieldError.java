package com.example.gateway;

/** One validation failure in a 422 response. */
public record FieldError(String field, String message) {
}
