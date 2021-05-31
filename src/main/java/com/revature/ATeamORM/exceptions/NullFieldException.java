package com.revature.ATeamORM.exceptions;

public class NullFieldException extends RuntimeException{
	public NullFieldException() {
		super("One of your @Columns is notNull() but the field is null");
	}

	public NullFieldException(String message) {
		super(message);
	}
}
