package ru.akiselev.paymentservice.persistence.dao;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EntityValidator {
    private EntityValidator() {

    }

    public static <T> void validate(final List<? extends T> entities) {
        for (final T entity : entities) {
            validate(entity);
        }
    }

    public static <T> void validate(final T entity) {
        final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        final Validator validator = validatorFactory.getValidator();
        final Set<ConstraintViolation<T>> violations = validator.validate(entity);
        if (!violations.isEmpty()) {
            throw new RuntimeException("Validation errors." + violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet()));
        }
    }
}
