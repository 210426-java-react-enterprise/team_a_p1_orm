import com.revature.ATeamORM.util.annotations.JDBCConnection;
import com.revature.ATeamORM.util.datasource.ConnectionFactory;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;

@JDBCConnection(url = "java-react-enterprise-210426.cxg4xfp9ig7t.us-east-1.rds.amazonaws.com", username = "postgresAdmin", password = "r3vature&Uros")
public class testDriver {

	public static void main(String[] args) {
		try (Connection connection = ConnectionFactory.getInstance().getConnection(testDriver.class)) {
			System.out.println("Connection Established");
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}
}
