package me.dw1e.ff.check.api.annotations;

import me.dw1e.ff.check.api.Category;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckInfo {

    Category category();

    String type();

    String desc();

    double minVL() default 0.0;

    int maxVL() default 30;

    boolean punish() default true;
}