package com.revature.ATeamORM.datasource;

import com.revature.ATeamORM.annotations.JDBCConnection;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.sql.Connection;
import java.sql.PreparedStatement;

@JDBCConnection(url="url", username="username", password="password")
public class ConnectionFactoryTest {

	@Mock private Connection mockConnection;
	@Mock private PreparedStatement mockStatement;

	@Before
	public void setUpTest() {

	}

	@After
	public void tearDownTest() {

	}

	@Test
	public void test_getInstanceWithNoExistingInstance() {
		ConnectionFactory f = ConnectionFactory.getInstance();
		Assert.assertNotNull(f);
	}

	@Test
	public void test_getInstanceWithExistingInstance() {
		ConnectionFactory f = ConnectionFactory.getInstance();
		ConnectionFactory h = ConnectionFactory.getInstance();
		Assert.assertEquals(f, h);
	}

	@Test
	public void test_getConnectionWithValidCredentials() {

	}


}
