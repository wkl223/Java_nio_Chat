package Logic;

import Protocol.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ClientReception {
    public static final String INCORRECT_PROTOCOL ="Server sends a message with incorrect type!";
    public static final String SAFE_TO_QUIT = "OK";
    public static String newIdentity(Protocol p) throws IOException {
        String former = p.getMessage().getFormer();
        String identity =p.getMessage().getIdentity();
        if(former.equals(identity))
            return "Requested identity invalid or inuse";
        else
            return former+" is now "+identity;
    }
    public static String roomChange(Protocol p, boolean waitToQuit, String clientName) throws IOException {
        String formerRoom = p.getMessage().getFormer();
        String roomId = p.getMessage().getRoomId();
        String identity = p.getMessage().getIdentity();
        if(formerRoom.equals(roomId)) return "The requested room is invalid or non existent.";
        if(waitToQuit){
            if (identity.equals(clientName) && roomId.equals(""))
                return SAFE_TO_QUIT;
        }
        return identity+" moved from "+formerRoom+" to "+roomId;
    }
    public static String roomContents(Protocol p) throws IOException{
        String roomId = p.getMessage().getRoomId();
        List<String> identities = p.getMessage().getParticipants();
        String owner = p.getMessage().getOwner();
        return "Room: "+roomId+", who is in this room: "+identities.toString()+", Owner: "+owner;
    }
    public static String roomList(Protocol p) throws IOException{
        // partial message, not necessary pass Room object
        Map<String,String> rooms =p.getMessage().getRooms();
        String response="";
        for(Map.Entry<String,String> entry: rooms.entrySet()){
            response= Message.addHeadAndTail(entry.getKey()+", "+entry.getValue(),"[","]\r\n");
        }
        return response;
    }
    public static String message(Protocol p) throws IOException{
        String identity = p.getMessage().getIdentity();
        String content = p.getMessage().getContent();
        return Message.addHeadAndTail(identity,"[","]")+": "+content;
    }

}
