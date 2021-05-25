package com.revature.ATeamORM.util.datasource;

import com.revature.ATeamORM.exceptions.DataSourceException;
import com.revature.ATeamORM.util.annotations.JDBCConnection;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Set;

/**
 * Tries to set up a connection factory
 *
 * @author Uros Vorkapic
 * @author Juan Mendoza
 */
public class ConnectionFactory {
	static ConnectionFactory connectionFactory;
	static String dbDriver;
	String currentSchema;

	/*
	 * Ensures the driver is loaded into memory before the ConnectionFactory is ever even instantiated
	 */
	static {
		try {
			dbDriver = (String) JDBCConnection.class.getDeclaredMethod("dbType").getDefaultValue();
			Class.forName(dbDriver);
		} catch (ClassNotFoundException | NoSuchMethodException e) {
			e.printStackTrace();
		}
	}

	private ConnectionFactory() {}

	/**
	 * Gets an instance of the ConnectionFactory singleton, or creates one if none exist
	 * @return The only ConnectionFactory instance
	 */
	public static ConnectionFactory getInstance() {
		if (connectionFactory == null) {
			connectionFactory = new ConnectionFactory();
		}

		return connectionFactory;
	}

	/**
	 * Creates a connection to a predefined database
	 * @param oClass The class that has the @JDBCConnection annotation
	 * @return The connection requested based on @JDBCConnection annotations
	 * @throws DataSourceException Throws if @JDBCConnection annotation does not exist in class
	 * @throws SQLException Throws if credentials for connection (url, username, or password) are invalid
	 */
	public Connection getConnection(Class<?> oClass) throws DataSourceException, SQLException {
		if (!oClass.isAnnotationPresent(JDBCConnection.class)) {
			throw new DataSourceException("Object does not have a @JDBCConnection annotation");
		}
		JDBCConnection anno = oClass.getAnnotation(JDBCConnection.class);

		if (!anno.schema().equals("")) {
			currentSchema = anno.schema();
		}
		String url = injectDriver(anno.url());

		return DriverManager.getConnection(url, anno.username(), anno.password());
	}

	/**
	 * Creates a connection to a predefined database
	 * @param o The object whose class has the @JDBCConnection annotation
	 * @return The connection requested based on @JDBCConnection annotations
	 * @throws DataSourceException Throws if @JDBCConnection annotation does not exist in class
	 * @throws SQLException Throws if credentials for connection (url, username, or password) are invalid
	 */
	public Connection getConnection(Object o) throws DataSourceException, SQLException {
		return getConnection(Objects.requireNonNull(o.getClass()));
	}

	/**
	 * Changes the default driver for the JDBC connection
	 * @param newDriver The full package name of driver
	 */
	public static void changeDriver(String newDriver) {
		dbDriver = newDriver;
	}

	/**
	 * Injects database and host details into the url
	 * @param url The url of the database
	 * @return The modified url that includes language specific identifiers
	 * @author Uros Vorkapic
	 */
	private String injectDriver(String url) {
		String urlWithDriver = url;
		if (dbDriver.equals("org.postgresql.Driver")) {
			urlWithDriver = "jdbc:postgresql://" + url + ":5432/postgres";
			if (currentSchema != null) {
				urlWithDriver +="?currentSchema=" + currentSchema;
			}
		}

		return urlWithDriver;
	}
}
