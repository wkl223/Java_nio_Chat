package Logic;

import Protocol.*;
import Protocol.Entity.Room;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ClientReception {
    private static Room findRoom(List<Room> rooms, String roomId){
        for (Room r : rooms){
            if (r.getRoomId().equals(roomId))
                return r;
        }
        return null;
    }
    public static String newIdentity(Protocol p, String client) throws IOException {
        String former = p.getMessage().getFormer();
        String identity =p.getMessage().getIdentity();
        System.out.println("DEBUG - newIdentity Reception former:"+former+", identity:"+identity+", client:"+client);
        if(former.equals(identity) && former.equals(client))
            return "Requested identity invalid or inuse";
        else if(former.equals("")) return "Welcome! "+identity;
        else return former+" is now "+identity;
    }
    public static String roomChange(Protocol p, String clientName) throws IOException {
        String formerRoom = p.getMessage().getFormer();
        String roomId = p.getMessage().getRoomId();
        String identity = p.getMessage().getIdentity();
        if(formerRoom.equals(roomId)) return "The requested room is invalid or non existent.";
        if (identity.equals(clientName) && roomId.equals("")) return identity+" has left";
        return identity+" moved from "+formerRoom+" to "+roomId;
    }
    public static String roomContents(Protocol p) throws IOException{
        String roomId = p.getMessage().getRoomId();
        List<String> identities = p.getMessage().getParticipants();
        String owner = p.getMessage().getOwner();
        return "Room: "+roomId+", who is in this room: "+identities.toString()+", Owner: "+owner;
    }
    public static String roomList(Protocol p, Map<String,String> request) throws IOException{
        // partial message
        List<Room> rooms =p.getMessage().getRooms();
        String response="";
        //if it is a follow-up of delete request.
        if(request.size()!=0){
            String out = "";
            if(findRoom(rooms,request.get(Message.TYPE_DELETE))!=null) {
                out = "Error, the room "+request.get(Message.TYPE_DELETE)+" was not deleted.";
            }
            else if(request.containsKey(Message.TYPE_DELETE)){
                out ="The room "+request.get(Message.TYPE_DELETE)+ " was not found or successfully deleted";
            }
            if(findRoom(rooms,request.get(Message.TYPE_ROOM_CREATION))!=null){
                out = "The room "+request.get(Message.TYPE_ROOM_CREATION)+" is created";
            }
            else if(request.containsKey(Message.TYPE_ROOM_CREATION)){
                out = "Error, the room "+request.get(Message.TYPE_ROOM_CREATION)+" was not created";
            }
            request.clear();
            return out;
        }
        for(Room r: rooms){
            response += Message.addHeadAndTail(r.getRoomId()+", Number of people in the room:"+r.getCount(),"[","]");
        }
        return response;
    }
    public static String message(Protocol p) throws IOException{
        String identity = p.getMessage().getIdentity();
        String content = p.getMessage().getContent();
        return Message.addHeadAndTail(identity,"From server, by [","]")+": "+content;
    }

}
