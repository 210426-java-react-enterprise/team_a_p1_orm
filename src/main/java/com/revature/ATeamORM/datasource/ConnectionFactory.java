package com.revature.ATeamORM.datasource;

import com.revature.ATeamORM.annotations.ConnectionConfig;
import com.revature.ATeamORM.exceptions.DataSourceException;
import com.revature.ATeamORM.annotations.JDBCConnection;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

/**
 * Singleton factory that establishes a connection with a database based on @JDBCConnection params
 * @author Juan Mendoza, Uros Vorkapic, Vinson Chin
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
	 * @author Juan Mendoza, Uros Vorkapic, Vinson Chin
	 */
	public static ConnectionFactory getInstance() {
		if (connectionFactory == null) {
			connectionFactory = new ConnectionFactory();
		}

		return connectionFactory;
	}

	/**
	 * Creates a connection to a predefined database
	 * @param clazz The class that has the @JDBCConnection annotation & the optional @ConnectionConfig
	 * @return The connection requested based on @JDBCConnection annotations
	 * @throws DataSourceException Throws if @JDBCConnection annotation does not exist in class
	 * @throws SQLException Throws if credentials for connection (url, username, or password) are invalid
	 * @author Juan Mendoza, Uros Vorkapic
	 */
	public Connection getConnection(Class<?> clazz) throws DataSourceException, SQLException {
		if (!clazz.isAnnotationPresent(JDBCConnection.class)) {
			throw new DataSourceException("Object does not have a @JDBCConnection annotation");
		}
		JDBCConnection anno = clazz.getAnnotation(JDBCConnection.class);
		List<String> annoList = new ArrayList<>();
		if (clazz.isAnnotationPresent(ConnectionConfig.class)) {
			annoList = configureConnection(clazz);
		} else {
			annoList.add(anno.url());
			annoList.add(anno.username());
			annoList.add(anno.password());
			annoList.add(anno.schema());
		}

		if (!anno.schema().equals("")) {
			currentSchema = annoList.get(3);
		}
		String url = injectDriver(annoList.get(0));

		return DriverManager.getConnection(url, annoList.get(1), annoList.get(2));
	}

	/**
	 * Creates a connection to a predefined database
	 * @param o The object whose class has the @JDBCConnection annotation
	 * @return The connection requested based on @JDBCConnection annotations
	 * @throws DataSourceException Throws if @JDBCConnection annotation does not exist in class
	 * @throws SQLException Throws if credentials for connection (url, username, or password) are invalid
	 * @author Juan Mendoza, Uros Vorkapic, Vinson Chin
	 */
	public Connection getConnection(Object o) throws DataSourceException, SQLException {
		return getConnection(Objects.requireNonNull(o.getClass()));
	}

	/**
	 * Changes the default driver for the JDBC connection. Currently non-functional.
	 * @param newDriver The full package name of driver
	 * @author Uros Vorkapic
	 */
	public static void changeDriver(String newDriver) {
		dbDriver = newDriver;
	}

	/**
	 * Injects database and host details into the url; currently only implements postgres syntax
	 * @param url The url of the database
	 * @return The modified url that includes language specific identifiers
	 * @author Uros Vorkapic
	 */
	private String injectDriver(String url) {
		StringBuilder urlWithDriver = new StringBuilder();
		if (dbDriver.equals("org.postgresql.Driver")) {
			urlWithDriver.append("jdbc:postgresql://").append(url).append(":5432/postgres");
			if (currentSchema != null) {
				urlWithDriver.append("?currentSchema=").append(currentSchema);
			}
		}

		return urlWithDriver.toString();
	}

	/**
	 * Configures connection with instructions from @ConnectionConfig to allow for file-based credentials
	 * @param clazz class contianing both @ConnectionConfig and @JDBCConnection
	 * @return ArrayList List used to instantiate connection
	 * @author Uros Vorkapic
	 */
	private List<String> configureConnection(Class<?> clazz) {
		ConnectionConfig config = clazz.getAnnotation(ConnectionConfig.class);
		JDBCConnection connProps = clazz.getAnnotation(JDBCConnection.class);
		String filePath = config.filepath();
		StringBuilder actualFilePath = new StringBuilder();

		// Preliminary filepath setting
		if (filePath.startsWith("/")) {
			filePath = filePath.substring(1, filePath.length() - 1);
		}

		actualFilePath.append(filePath);

		// Tries to find properties file using designated filepath
		Properties prop = new Properties();
		try {
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			InputStream input = loader.getResourceAsStream(actualFilePath.toString());
			prop.load(input);
		} catch (IOException e) {
			e.printStackTrace();
		}

		List<String> connPropsList = new LinkedList<>();
		connPropsList.add(connProps.url());
		connPropsList.add(connProps.username());
		connPropsList.add(connProps.password());
		connPropsList.add(connProps.schema());

		List<String> elementList = new ArrayList<>();
		// Configures JDBCConnection with appropriate values
		String contentSplicer;
		for (String element : connPropsList) {
			if (element.startsWith("${") && element.endsWith("}")) {
				contentSplicer = element.substring(2, element.length() - 1);
				element = prop.getProperty(contentSplicer);
			}
			elementList.add(element);
		}
		return elementList;
	}
}
