package com.revature.ATeamORM.util.datasource;


import com.revature.ATeamORM.exceptions.DataSourceException;
import com.revature.ATeamORM.repos.ObjectRepo;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class Session {

	private Connection connection;
	private ObjectRepo repo;

	public Session(Class<?> clazz) throws SQLException, DataSourceException {
		connection = ConnectionFactory.getInstance().getConnection(clazz);
		repo = new ObjectRepo();
	}

	<T> List<T> find(Class<?> clazz, String fieldName, T fieldValue) {
		// return repo.read(connection, clazz, fieldName, fieldValue);
		return null;
	}

	public void save() {

	}

	public void insert() {

	}

	public void remove() {

	}

}
