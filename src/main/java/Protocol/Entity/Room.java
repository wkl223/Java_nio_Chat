package Protocol.Entity;

import java.util.List;

public class Room{
    private String roomId;
    private String count;
    public String owner;
    public List<String> users;
    public static final int SUCCESS =1;
    public static final int FAILURE =-1;

    public Room(){
    }
    public Room(String roomId, String count, String owner) {
        this.roomId = roomId;
        this.count = count;
        this.owner=owner;
    }
    public Room(String roomId, String count) {
        this.roomId = roomId;
        this.count = count;
        this.owner="";
    }
    public synchronized int removeUser(String username){
        if(users.contains(username)){
            users.remove(username);
            return SUCCESS;
        }
        else return FAILURE;
    }
    public synchronized int addUser(String username){
        // avoid duplicate add
        if(users.contains(username)) return FAILURE;
        else{
            users.add(username);
            return SUCCESS;
        }
    }
    public String getRoomId(){
        return roomId;
    }
    public String getCount(){
        count = String.valueOf(users.size());
        return count;
    }
    public List<String> getUsers(){
        return users;
    }
    public String getOwner(){
        return owner;
    }
    public String toString(){
        return "roomId: "+roomId+", count: "+count;
    }
}