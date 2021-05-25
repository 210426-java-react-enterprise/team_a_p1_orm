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
	static String dbDriver = "org.postgresql.Driver";

	static {
		try {
			Class.forName(dbDriver);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	private ConnectionFactory() {}

	public static ConnectionFactory getInstance() {
		if (connectionFactory == null) {
			connectionFactory = new ConnectionFactory();
		}

		return connectionFactory;
	}

	public Connection getConnection(Class<?> oClass) throws DataSourceException, SQLException {
		if (!oClass.isAnnotationPresent(JDBCConnection.class)) {
			throw new DataSourceException("Object does not have a @JDBCConnection annotation");
		}
		JDBCConnection anno = oClass.getAnnotation(JDBCConnection.class);

		return DriverManager.getConnection(anno.url(), anno.username(), anno.password());
	}

	public Connection getConnection(Object o) throws DataSourceException, SQLException {
		Class<?> oClass =  Objects.requireNonNull(o.getClass());

		return getConnection(oClass);
	}

	public void changeDriver(String newDriver) {
		dbDriver = newDriver;
	}
}
