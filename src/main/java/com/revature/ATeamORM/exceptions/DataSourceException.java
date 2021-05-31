package com.revature.ATeamORM.exceptions;

/**
 * Thrown if there are non-SQL related problems with the object or class passed into the ORM to be retrieved.
 * @author Vinson Chin
 */
public class DataSourceException extends RuntimeException {
    public DataSourceException() {
        super("There was a problem when communicating with the database. Check the logs for more details.");
    }

    public DataSourceException(String message) {
        super(message);
    }
}
