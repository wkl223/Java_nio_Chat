package Logic;

import Protocol.Message;
import Protocol.Entity.Room;
import Protocol.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
* Construct Json message (Protocol) from Message object
* */
public class ServerResponds {
    public static int ClientCount = 1;
    public static final String NEW_USER_INDICATOR = "";
    public static Protocol newIdentity(Map<String, String> clients, List<String> freeName, String currentClientName, String requestedClientName) throws IOException {
        String former, identity;
        Message m = new Message();
        // 1. client name duplicate/existed
        if(clients.get(requestedClientName)!= null){
            // server side invalid message
            former = currentClientName;
            identity = currentClientName;
            m.setSuccessed(false);
        }
        // 2. new client
        else if(requestedClientName.equals(NEW_USER_INDICATOR)){
            former = NEW_USER_INDICATOR;
            identity = getAvailableClientName(freeName);
            m.setSuccessed(true);
        }
        // 3. general case
        else{
            former = currentClientName;
            identity = requestedClientName;
            m.setSuccessed(true);
        }
        m.setType(Message.TYPE_NEW_IDENTITY);
        m.setFormer(former);
        m.setIdentity(identity);
        return new Protocol(m);
    }
    private static synchronized String getAvailableClientName(List<String> freeName){
        if(freeName.size()>1)
            return freeName.get(0);
        else{
            String name = "Guest"+ ClientCount;
            ClientCount+=1;
            return name;
        }
    }
    // ROOM CHANGE
    public static Protocol roomChange(List<Room> chatRoom, String identity, String formerRoomId, String requestedRoomId) throws IOException {
        Message m = new Message();
        String former = formerRoomId;
        String roomId = requestedRoomId;
        m.setType(Message.TYPE_ROOM_CHANGE);
        // 1. room not found
        if(findRoom(chatRoom,requestedRoomId) == null){
            m.setFormer(former);
            m.setRoomId(former);
            m.setIdentity(identity);
            m.setSuccessed(false);
        }
        // 2. general case
        else{
            // prepare ROOM CHANGE message
            m.setFormer(former);
            m.setRoomId(requestedRoomId);
            m.setIdentity(identity);
            m.setSuccessed(true);
        }
        return new Protocol(m);
    }
    //ROOM CONTENTS
    public static Protocol roomContents(String roomId, List<Room> chatRoom){
        Message m = new Message();
        m.setType(Message.TYPE_ROOM_CONTENTS);
        Room target =findRoom(chatRoom,roomId);
        if (target==null){
            // do not send anything
            m.setSuccessed(false);
        }
        else{
            m.setSuccessed(true);
            m.setRoomId(roomId);
            m.setParticipants(target.getUsers());
            m.setOwner(target.getOwner());
        }
        return new Protocol(m);
    }
    //ROOM LIST
    public static Protocol roomList(List<Room> chatRoom){
        Message m = new Message();
        m.setType(Message.TYPE_ROOM_LIST);
        Map<String,String> tempRoomsInfo = new HashMap<>();
        for(Room r: chatRoom){
            tempRoomsInfo.put(r.getRoomId(),r.getCount());
        }
        m.setRooms(tempRoomsInfo);
        m.setSuccessed(true);
        return new Protocol(m);
    }
    //MESSAGE
    public static Protocol message(String content, String identity){
        Message m = new Message();
        m.setType(Message.TYPE_MESSAGE);
        m.setContent(content);
        m.setIdentity(identity);
        m.setSuccessed(true);
        return new Protocol(m);
    }
    public synchronized static Room findRoom(List<Room> rooms, String roomId){
        for (Room r : rooms){
            if (r.getRoomId().equals(roomId))
                return r;
        }
        return null;
    }
}
