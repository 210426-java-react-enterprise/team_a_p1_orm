import com.revature.ATeamORM.models.AppUser;
import com.revature.ATeamORM.repos.ObjectRepo;
import com.revature.ATeamORM.util.annotations.JDBCConnection;
import org.postgresql.core.ConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;

@JDBCConnection(url = "fakeurl", username = "fakeusername", password = "fakepassword")
public class testDriver {
	public static void main(String[] args) {
		

	}
}
