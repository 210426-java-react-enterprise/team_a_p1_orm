package com.revature.ATeamORM.util;

import java.lang.annotation.*;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Constraints {
	String name();
	String foreignKey() default "";
	String onDelete() default "NO ACTION";
	String onUpdate() default "NO ACTION";
	boolean primaryKey() default false;
	boolean notNull() default false;
	boolean unique() default false;

}
