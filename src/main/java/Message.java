import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message<T> {
    private String type;
    private String content;
    private String identity;
    private String room;
    private String former;
    private String sender;
    private String roomId;
    //@JsonDeserialize(using = RoomListJsonDeserializer.class)
    private List<RoomList> rooms;
    private List<String> participants;
    // message head constant
    public static final String MESSAGE_HEAD = "content";
    public static final String TYPE_HEAD = "type";
    public static final String SENDER_HEAD = "sender";
    public static final String ROOM_HEAD ="room";
    public static final String ROOM_DESTINATION_HEAD ="roomId";
    public static final String COUNT_HEAD = "count";
    public static final String ROOM_LIST_HEAD ="rooms";
    public static final String FORMER_HEAD ="former";
    public static final String IDENTITY_HEAD ="identity";
    public static final String OWNER_HEAD = "owner";
    public static final String EMPTY = "";
    // type constant
    public static final String TYPE_MESSAGE = "message";
    public static final String TYPE_QUIT = "quit";
    public static final String TYPE_NEW_IDENTITY = "newidentity";
    public static final String TYPE_IDENTITY_CHANGE = "identitychange";
    public static final String TYPE_JOIN = "join";
    public static final String TYPE_ROOM_CONTENTS = "roomcontents";
    public static final String TYPE_WHO = "who";
    public static final String TYPE_ROOM_LIST = "roomlist";
    public static final String TYPE_ROOM_CREATION = "createroom";
    public static final String TYPE_DELETE = "delete";
    public static final String TYPE_LIST = "list";
    public static final String TYPE_ROOM_CHANGE ="roomchange";
    // delimiters and marks
    public static final String KV_DELIMITER = ":";
    public static final String TUPLE_DELIMITER =",";
    public static final String ROOM_OWNER_MARK ="*";

    public String getContent(){
        return this.content;
    }
    public String getType(){return this.type;}
    public String getIdentity(){return this.identity;}
    public String getRoom(){
        return this.room;
    }
    public String getSender(){return this.sender;}
    public String getFormer(){return this.former;}
    public String getRoomId(){return this.roomId;}
    public List<RoomList> getRooms(){
        return this.rooms;
    }
    public List<String> getParticipants(){return this.participants;}

    public void setType(String type) {this.type = type;}
    public void setContent(String content) {this.content = content;}
    public void setIdentity(String identity) {this.identity = identity;}
    public void setRoom(String room) {this.room = room;}
    public void setFormer(String former) {this.former = former;}
    public void setSender(String sender) {this.sender = sender;}
    public void setRoomId(String roomId) {this.roomId = roomId;}
    public void setRooms(List<RoomList> rooms) {this.rooms = rooms;}
    public void setParticipants(List<String> participants) {this.participants = participants;}


    public Message(){}//empty constructor

    public static String addHeadAndTail(String message, String head, String tail){
        String out = message;
        out = head+out+tail;
        return out;
    }
    public static String transformMessagePairs(String head, String content){
        String message = addHeadAndTail(head,"\"","\"")+KV_DELIMITER+addHeadAndTail(content,"\"","\"");
        return message;
    }
    public static String transformListPairs(String head, String content){
        String message = addHeadAndTail(head,"\"","\"")+KV_DELIMITER+content;
        return message;
    }
    public static String jsonCompose(String[] content){
        return jsonCompose(new ArrayList<>(List.of(content)));
    }
    public static String jsonCompose(List<String> content){
        String out="";
        for(String s :content){
            out += s+TUPLE_DELIMITER;
        }
        out = out.substring(0,out.length()-1);//ignore the last comma
        return addHeadAndTail(out,"{","}");
    }
    public static String jsonCompose(Message m) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        mapper.writeValue(out,m);
        return new String(out.toByteArray());
    }
    public static String roomListToJson(List<RoomList> rooms) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(out, rooms);
        final byte[] data =out.toByteArray();
        return new String(data);
    }



}
class RoomList{
    private String roomId;
    private String count;
    RoomList(){
    }
    RoomList(String roomId, String count) {
        this.roomId = roomId;
        this.count = count;
    }

    public String getRoomId(){
        return roomId;
    }
    public String getCount(){
        return count;
    }
    public String toString(){
        return "roomId: "+roomId+", count: "+count;
    }
}
