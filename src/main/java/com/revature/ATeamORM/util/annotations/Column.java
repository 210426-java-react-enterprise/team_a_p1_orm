package com.revature.ATeamORM.util.annotations;

import java.lang.annotation.*;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
    String name();
    boolean notNull() default false;
    boolean unique() default false;
}
