package com.revature.ATeamORM.annotations;

import java.lang.annotation.*;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ForeignKey {
	String name();
	String references(); // references a primary key in another table
}
