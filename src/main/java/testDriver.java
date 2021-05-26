import com.revature.ATeamORM.models.AppUser;
import com.revature.ATeamORM.repos.ObjectRepo;
import com.revature.ATeamORM.util.annotations.JDBCConnection;
import org.postgresql.core.ConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;

@JDBCConnection(url = "fakeurl", username = "fakeusername", password = "fakepassword")
public class testDriver {
	public static void main(String[] args) {
		
		ObjectRepo or = new ObjectRepo();
		/*try (Connection conn = ConnectionFactory.getInstance().getConnection(testDriver.class)){
			System.out.println("donothing");
		} catch (SQLException e) {
			e.printStackTrace();
		}*/
		
		AppUser testUser = new AppUser("testeyoozer","newpassword");
		testUser.setId(3);
		
		try {
			or.sqlUpdateQuery(testUser);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}
	}
}
