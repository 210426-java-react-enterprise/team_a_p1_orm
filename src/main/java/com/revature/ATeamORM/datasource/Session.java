package com.revature.ATeamORM.datasource;

import com.revature.ATeamORM.repos.ObjectRepo;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Maintains and establishes a connection with the database and invokes the CRUD+ operations
 */
public class Session implements AutoCloseable {

	private Connection connection;
	private final ObjectRepo repo;
	private final Class<?> clazz;

	/**
	 * Opens a connection using JDBCConnection annotated credentials from inserted class.
	 * @param clazz The class that contains the JDBCConnection annotation with appropriate credentials
	 * @author Uros Vorkapic
	 */
	public Session(Class<?> clazz) {
		this.clazz = clazz;
		repo = new ObjectRepo();
	}

	/**
	 * Finds and returns a Result List of objects instantiated from the clazz Class
	 * whose fieldName matches the fieldValue provided
	 * @param clazz The class reference for the objects to be built from
	 * @param fieldName The name of the field (not column) that will be searched
	 * @param fieldValue The value of the field as a String
	 * @param <T> The object type created from the injected class
	 * @return Result object containing a list of all entries returned from query
	 * @throws SQLException Thrown if connection cannot be established, fieldName does not exist or
	 * if @Column is not properly annotated
	 * @author Uros Vorkapic
	 */
	public <T> Result<T> find(Class<T> clazz, String fieldName, String fieldValue) throws SQLException {
		return repo.read(connection, clazz, fieldName, fieldValue);
	}

	/**
	 * Finds and returns all entries from database as a Result list of objects instantiated from clazz Class
	 * @param clazz The class reference for the objects to be built from
	 * @param <T> The object type created from the injected class
	 * @return Result object containing a list of all entries in database
	 * @throws SQLException Thrown if connection cannot be established or @Column fields are not correctly annotated
	 * @author Uros Vorkapic
	 */
	public <T> Result<T> findAll(Class<T> clazz) throws SQLException {
		return repo.read(connection, clazz);
	}

	/**
	 * Saves/Updates the values of the object provided into the database based on the @Id annotated field of the object
	 * @param object The object with non-null fields to use to update the database with
	 * @throws SQLException Thrown if connection cannot be established, object is missing field values or if
	 * ID cannot be found.
	 * @author Uros Vorkapic
	 */
	public void save(Object object) throws SQLException {
		repo.update(connection, object);
	}

	/**
	 * Inserts/Creates an entry in the database that matches the table structure of the object provided, then
	 * inserts the database generated Id into the object. Does not return object, but its reference is still updated
	 * @param object The object to be inserted into the database
	 * @throws SQLException Thrown if connection cannot be established, object is missing non-null field values or
	 * if uniqueness is not ensured
	 * @author Uros Vorkapic
	 */
	public void insert(Object object) throws SQLException {
		repo.create(connection, object);
	}

	/**
	 * Deletes the provided object from the database entirely using @Id annotated object field
	 * @param object The object to be removed from the database
	 * @throws SQLException Thrown if connection cannot be established or something went terribly wrong
	 * @author Uros Vorkapic
	 */
	public void remove(Object object) throws SQLException {
		repo.delete(connection, object);
	}
	
	
	/**
	 * Method that searches DB based on unique parameter from @Column annotation, returns true if provided
	 * information is unique, false otherwise.
	 * @param object	Object to that will search for if uniquely provided information is not present in DB.
	 * @return			Boolean value, true information is unique and false otherwise.
	 * @throws SQLException
	 * @throws IllegalAccessException
	 * @author Juan Mendoza
	 */
	public boolean isEntityUnique(Object object) throws SQLException, IllegalAccessException {
		return repo.isEntryUnique(connection,object);
	}
	

	/**
	 * Opens a session by establishing a connection based on provided @ConnectionConfig and @JDBCConnection
	 * @throws SQLException Thrown if connection cannot be established
	 * @author Uros Vorkapic
	 */
	public void open() throws SQLException {
		connection = ConnectionFactory.getInstance().getConnection(clazz);
	}

	/**
	 * Closes the current session. This method is automatically invoked if Session.open() is invoked as an
	 * AutoCloseable in the try-catch
	 * @throws SQLException Thrown if connection cannot be closed properly.
	 * @author Uros Vorkapic
	 */
	@Override
	public void close() throws SQLException {
		connection.close();
	}

}
