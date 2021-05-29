import com.revature.ATeamORM.datasource.Session;
import com.revature.ATeamORM.annotations.JDBCConnection;

import java.sql.SQLException;

@JDBCConnection(url = "", username = "", password = "", schema = "")
public class testDriver {

	public static void main(String[] args) {
		try {
			AppUser testUser = new AppUser();
			testUser.setUsername("test1");
			testUser.setPassword("password");
			System.out.println(testUser);
			Session session = new Session(testDriver.class);
			System.out.println(session.find(AppUser.class, "username", testUser.getUsername()).getFirstEntry());

			AppUser newUser = new AppUser("ormTest7", "password", "email7@email.com");
			System.out.println(newUser);
			session.insert(newUser);
			System.out.println(newUser);

			AppUser deleteUser = newUser;
			System.out.println(deleteUser);
			session.remove(deleteUser);

			AppUser saveUser = testUser;
			System.out.println(saveUser);
			saveUser.setEmail("newEmail@email.com");
			saveUser.setId(1);
			System.out.println(saveUser);
			session.save(saveUser);
			System.out.println(saveUser);

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
