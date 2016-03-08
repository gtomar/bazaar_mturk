package basilica2.myagent;

import java.sql.Timestamp;

public class User {
	
    public String name;
    public String id;
	public Timestamp time_of_entry;
	public int score;
	public boolean[] reasoning_flag;
	public boolean reasoning;
	public String reasoning_type;
	public int wait_duration;
	public int perspective;
    
    public User(String user_name, String user_id, Timestamp timestamp, int user_perspective) {
    	name = user_name;
    	id = user_id;
    	time_of_entry = timestamp;
    	score = 1;
    	reasoning_flag = new boolean[4];
    	reasoning_type = "";
    	reasoning = false;
    	wait_duration = 10;
    	perspective = user_perspective;
    }
    
}
