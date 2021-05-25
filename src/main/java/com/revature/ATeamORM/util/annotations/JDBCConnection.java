package com.revature.ATeamORM.util.annotations;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JDBCConnection {
	String url();
	String username();
	String password();
}
