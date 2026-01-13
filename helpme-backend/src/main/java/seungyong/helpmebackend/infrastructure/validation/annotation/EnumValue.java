package seungyong.helpmebackend.infrastructure.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import seungyong.helpmebackend.infrastructure.validation.EnumValueValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EnumValueValidator.class)
public @interface EnumValue {
    String message() default "허용되지 않는 값입니다. {enumClass}의 값을 참고하세요.";
    Class<?>[] groups() default { };
    Class<? extends Payload>[] payload() default { };
    Class<? extends Enum<?>> enumClass();
    boolean ignoreCase() default true;
}
