package webby.commons.bean.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация, содержащая наименование параметра.<br>
 * Используется для получения информации в рантайме.
 *
 * @author den
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
public @interface PName {
    String value();
}