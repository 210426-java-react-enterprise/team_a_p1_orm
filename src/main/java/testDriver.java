
import com.revature.ATeamORM.annotations.ConnectionConfig;
import com.revature.ATeamORM.datasource.Session;
import com.revature.ATeamORM.annotations.JDBCConnection;

import java.sql.SQLException;

/**
 * Test Driver.
 */
@ConnectionConfig(filepath = "application.properties")
@JDBCConnection(url = "${host_url}", username = "${username}", password = "${password}", schema = "${schema}")
public class testDriver {

	public static void main(String[] args) {
		Session session = new Session(testDriver.class);
		try {
			session.open();
			AppUser testUser = new AppUser();
			testUser.setUsername("test1");
			testUser.setPassword("password");
			System.out.println(testUser);

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
			System.out.println(session.findAll(AppUser.class).getList());
			//testUser.setEmail("newerEmail@email.com");
			//testUser.setUsername("newUsername");
			System.out.println(session.isEntityUnique(testUser));

		} catch (SQLException e) {
			e.printStackTrace();
		}

	}
}
