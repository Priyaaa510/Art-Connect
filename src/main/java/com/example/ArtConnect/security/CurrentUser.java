package com.example.ArtConnect.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the logged-in {@link com.example.ArtConnect.model.User} resolved from the
 * "Authorization: Bearer &lt;token&gt;" header. Responds 401 when missing, unless required is false,
 * in which case null is injected for anonymous visitors.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
    boolean required() default true;
}
