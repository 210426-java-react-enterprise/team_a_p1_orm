package com.revature.ATeamORM.annotations;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JDBCConnection {
	String url();
	String username();
	String password();
	String schema() default "";
	String dbType() default "org.postgresql.Driver";
}
