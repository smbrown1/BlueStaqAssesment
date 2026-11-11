package com.bluestaq.notes.user;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

/**
 * Limits a string by its size in UTF-8 bytes rather than characters.
 * BCrypt cannot hash more than 72 bytes, and a character outside ASCII takes more than one.
 */
@Documented
@Constraint(validatedBy = MaxUtf8Bytes.Validator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxUtf8Bytes {

	int value();

	String message() default "must be at most {value} bytes when encoded as UTF-8";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	class Validator implements ConstraintValidator<MaxUtf8Bytes, String> {

		private int max;

		@Override
		public void initialize(MaxUtf8Bytes constraint) {
			this.max = constraint.value();
		}

		@Override
		public boolean isValid(String value, ConstraintValidatorContext context) {
			// A missing value is reported by @NotNull, not here.
			return value == null || value.getBytes(StandardCharsets.UTF_8).length <= max;
		}

	}

}
