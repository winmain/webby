package webby.commons.system.mbean;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * Аннотация, содержащая описание конструктора, метода, параметра, или типа.<br>
 * Используется для получения информации в рантайме.
 *
 * @author den
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ CONSTRUCTOR, METHOD, PARAMETER, TYPE })
public @interface Description {
    String value();
}