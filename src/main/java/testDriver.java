import com.revature.ATeamORM.util.annotations.JDBCConnection;
import com.revature.ATeamORM.util.datasource.ConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;

@JDBCConnection(url = "fakeurl", username = "fakeusername", password = "fakepassword")
public class testDriver {
	public static void main(String[] args) {
		try (Connection conn = ConnectionFactory.getInstance().getConnection(testDriver.class)){
			System.out.println("donothing");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
