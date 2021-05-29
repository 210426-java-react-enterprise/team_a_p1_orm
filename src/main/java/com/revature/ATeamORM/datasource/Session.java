package com.revature.ATeamORM.datasource;


import com.revature.ATeamORM.exceptions.DataSourceException;
import com.revature.ATeamORM.repos.ObjectRepo;

import java.sql.Connection;
import java.sql.SQLException;

public class Session {

	private Connection connection;
	private ObjectRepo repo;

	public Session(Class<?> clazz) throws SQLException, DataSourceException {
		connection = ConnectionFactory.getInstance().getConnection(clazz);
		repo = new ObjectRepo();
	}

	public <T> Result<T> find(Class<T> clazz, String fieldName, String fieldValue) throws SQLException {
		return repo.read(connection, clazz, fieldName, fieldValue);
	}

	public void save(Object object) throws SQLException {
		repo.update(connection, object);
	}

	public void insert(Object object) throws SQLException {
		repo.create(connection, object);
	}

	public void remove(Object object) throws SQLException {
		repo.delete(connection, object);
	}

}
