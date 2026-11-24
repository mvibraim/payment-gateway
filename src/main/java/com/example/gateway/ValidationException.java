package com.example.gateway;

import java.util.List;

class ValidationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final transient List<FieldError> errors;

	ValidationException(List<FieldError> errors) {
		super("Payment request is invalid");
		this.errors = List.copyOf(errors);
	}

	List<FieldError> errors() {
		return errors;
	}
}
