import com.revature.ATeamORM.repos.ObjectRepo;
import com.revature.ATeamORM.util.annotations.JDBCConnection;

@JDBCConnection(url = "fakeurl", username = "fakeusername", password = "fakepassword")
public class testDriver {
	public static void main(String[] args) {
		
		//ObjectRepo or = new ObjectRepo();
		/*try (Connection conn = ConnectionFactory.getInstance().getConnection(testDriver.class)){
			System.out.println("donothing");
		} catch (SQLException e) {
			e.printStackTrace();
		}*/
		
		
		
	}
}
