import com.revature.ATeamORM.models.AppUser;
import com.revature.ATeamORM.repos.ObjectRepo;
import com.revature.ATeamORM.util.annotations.JDBCConnection;
import org.postgresql.core.ConnectionFactory;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;

//@JDBCConnection(url = "", username = "", password = "")
public class testDriver {

	public static void main(String[] args) {
		ObjectRepo or = new ObjectRepo();
		/*try (Connection conn = ConnectionFactory.getInstance().getConnection(testDriver.class)){
			System.out.println("donothing");
		} catch (SQLException e) {
			e.printStackTrace();
		}*/


	}
}
