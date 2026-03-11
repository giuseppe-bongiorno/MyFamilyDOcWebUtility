package it.myfamilydoc.webutility.aop;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Auditable {
    String action();                      // Descrizione obbligatoria
    String entityType() default "";       // Tipo di entità, es. "User"
    String entityId() default "";         // ID entità, es. "123"
    String status() default "SUCCESS";    // SUCCESS, FAILURE, ecc.
    String details() default "";          // Altri dettagli opzionali
    String entityIdParam() default "";
}