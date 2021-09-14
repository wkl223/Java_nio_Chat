import Protocol.Message;
import Protocol.Entity.Room;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MessageTest {
    static ObjectMapper mapper=new ObjectMapper();
    String testMessage="Hello World!";
    String type = "chat";
    String identity = "me";
    String room = "MainH";
    String jsonMessage="{\"content\":\""+testMessage+"\",\"type\":\""+type+"\",\"identity\":\""+ identity +"\"}";
    String jsonMessageWithRoom="{\"content\":\""+testMessage+"\",\"type\":\""+type+"\",\"identity\":\""+ identity +"\",\"room\":\""+room+"\"}";
    static void generateRooms(List<Room> rooms){
        for(int i =0; i < 10; i++){
            rooms.add(new Room("TEST_ROOM"+i,String.valueOf(i),"admin"));
        }
    }
    static int findAndRemoveRoom(List<Room> rooms, String target){
        for(Room r : rooms){
            if(r.getRoomId().equals(target)) {
                rooms.remove(r);
                return 1;
            }
        }
        return -1;
    }
    @Test
    @DisplayName("Simple get message")
    void getMessage() throws JsonProcessingException {
        Message value = mapper.readValue(jsonMessage, Message.class);
        System.out.println("original msg:"+jsonMessage);
        System.out.println("decoded msg:"+value.getContent());
        assert(value.getContent().equals(testMessage));
    }

    @Test
    @DisplayName("Simple get type")
    void getType() throws JsonProcessingException {
        Message value = mapper.readValue(jsonMessage, Message.class);
        assert(value.getType().equals(type));
    }

    @Test
    @DisplayName("Simple get sender")
    void getSender() throws JsonProcessingException {
        Message value = mapper.readValue(jsonMessage, Message.class);
        assert(value.getIdentity().equals(identity));
    }

    @Test
    @DisplayName("Simple get room")
    void getRoom() throws JsonProcessingException {
        Message value = mapper.readValue(jsonMessageWithRoom, Message.class);
        assert(value.getRoom().equals(room));
    }

    @Test
    @DisplayName("Write and read ")
    void transformJsonTest() throws JsonProcessingException {
        //let's use existing String value and combine it as Json format
        ArrayList<String> msg = new ArrayList<>();
        msg.add(Message.transformMessagePairs(Message.MESSAGE_HEAD,testMessage));
        msg.add(Message.transformMessagePairs(Message.TYPE_HEAD,type));
        msg.add(Message.transformMessagePairs(Message.IDENTITY_HEAD, identity));
        msg.add(Message.transformMessagePairs(Message.ROOM_HEAD,room));
        String transformedJson=Message.jsonCompose(msg);
        System.out.println("original message:"+jsonMessageWithRoom);
        System.out.println("transformed json:"+transformedJson);
        assert(transformedJson.equals(jsonMessageWithRoom));
        //transform it back by Jackson
        Message value = mapper.readValue(transformedJson, Message.class);
        assert(value.getRoom().equals(room));
        assert(value.getIdentity().equals(identity));
        assert(value.getType().equals(type));
        assert(value.getContent().equals(testMessage));
    }
    /*
    Note: Below should be Protocol.Protocol tests, but write tests beforehand will help my actual software implementation
    and that's why I leave them here below. The protocol tests may not be implemented formally. Not sure
    if i have got time or not.
    ---Caleb
     */
    @Test
    @DisplayName("Server respond NewIdentity message")
    void serverRespondsIdentityChange() throws JsonProcessingException {
        //SERVER: prepare a new identity to client
        ArrayList<String> msg = new ArrayList<>();
        String newIdentityName = "Guest111";
        msg.add(Message.transformMessagePairs(Message.TYPE_HEAD,Message.TYPE_NEW_IDENTITY));
        msg.add(Message.transformMessagePairs(Message.FORMER_HEAD,Message.EMPTY));
        msg.add(Message.transformMessagePairs(Message.IDENTITY_HEAD,newIdentityName));
        String transformedJson=Message.jsonCompose(msg);
        System.out.println("New identity message:"+transformedJson);
        //CLIENT: transform it back by Jackson
        Message value = mapper.readValue(transformedJson, Message.class);
        assert(value.getIdentity().equals(newIdentityName));
        assert(value.getType().equals(Message.TYPE_NEW_IDENTITY));
        assert(value.getFormer().equals(Message.EMPTY));
    }
    @Test
    @DisplayName("Client send identity change message")
    void clientSendsIdentityChange() throws JsonProcessingException{
        //CLIENT: prepare a new identity message to server
        ArrayList<String> msg = new ArrayList<>();
        String newIdentityName = "Guest111";
        String formerIdentityName = "OLD_NAME_12345"; //get name from key attachment
        msg.add(Message.transformMessagePairs(Message.TYPE_HEAD,Message.TYPE_IDENTITY_CHANGE));
        msg.add(Message.transformMessagePairs(Message.IDENTITY_HEAD,newIdentityName));
        String transformedJson=Message.jsonCompose(msg);
        System.out.println("CLIENT: Change identity message:"+transformedJson);
        //SERVER: receive and decode it
        Message value = mapper.readValue(transformedJson, Message.class);
        assert(value.getIdentity().equals(newIdentityName));
        assert(value.getType().equals(Message.TYPE_IDENTITY_CHANGE));
        //SERVER: respond message
        msg = new ArrayList<>();
        msg.add(Message.transformMessagePairs(Message.TYPE_HEAD,Message.TYPE_NEW_IDENTITY));
        msg.add(Message.transformMessagePairs(Message.FORMER_HEAD,formerIdentityName));
        msg.add(Message.transformMessagePairs(Message.IDENTITY_HEAD,newIdentityName));
        transformedJson=Message.jsonCompose(msg);
        System.out.println("SERVER: New identity message:"+transformedJson);
        //CLIENT: transform it back by Jackson
        value = mapper.readValue(transformedJson, Message.class);
        assert(value.getIdentity().equals(newIdentityName));
        assert(value.getType().equals(Message.TYPE_NEW_IDENTITY));
        assert(value.getFormer().equals(formerIdentityName));
    }
    @Test
    @DisplayName("Client send join room message")
    void clientSendsJoinRoom() throws JsonProcessingException{
        //CLIENT: send join room message
        ArrayList<String> msg = new ArrayList<>();
        String identity = "Guest111";//get name from key attachment
        String formerRoom = "MainHall";//retrieve by identity
        String newRoom = "HelloooooRoom";
        msg.add(Message.transformMessagePairs(Message.TYPE_HEAD,Message.TYPE_JOIN));
        msg.add(Message.transformMessagePairs(Message.ROOM_DESTINATION_HEAD,newRoom));
        String transformedJson=Message.jsonCompose(msg);
        System.out.println("CLIENT: Join room message:"+transformedJson);
        //SERVER: receive and decode it
        Message value = mapper.readValue(transformedJson, Message.class);
        assert(value.getRoomId().equals(newRoom));
        assert(value.getType().equals(Message.TYPE_JOIN));
        //SERVER: respond message
        msg = new ArrayList<>();
        msg.add(Message.transformMessagePairs(Message.TYPE_HEAD,Message.TYPE_ROOM_CHANGE));
        msg.add(Message.transformMessagePairs(Message.IDENTITY_HEAD,identity));
        msg.add(Message.transformMessagePairs(Message.FORMER_HEAD,formerRoom));
        msg.add(Message.transformMessagePairs(Message.ROOM_DESTINATION_HEAD,newRoom));

        transformedJson=Message.jsonCompose(msg);
        System.out.println("SERVER: Join room respond:"+transformedJson);
        //CLIENT: transform it back by Jackson
        value = mapper.readValue(transformedJson, Message.class);
        assert(value.getIdentity().equals(identity));
        assert(value.getType().equals(Message.TYPE_ROOM_CHANGE));
        assert(value.getFormer().equals(formerRoom));
        assert(value.getRoomId().equals(newRoom));
    }
    @Test
    @DisplayName("Client request for delete room")
    void clientRequestDelete() throws IOException {
        //CLIENT: send delete message
        ArrayList<String> msg = new ArrayList<>();
        String identity = "Test_admin1";//get name from key attachment
        String roomId = "TEST_ROOM1";//retrieve by identity
        List<Room> roomProperties = new ArrayList<>();
        msg.add(Message.transformMessagePairs(Message.TYPE_HEAD,Message.TYPE_DELETE));
        msg.add(Message.transformMessagePairs(Message.ROOM_DESTINATION_HEAD,roomId));
        String transformedJson=Message.jsonCompose(msg);
        System.out.println("CLIENT: Delete room message:"+transformedJson);
        generateRooms(roomProperties);
        System.out.println("SERVER: CURRENT ROOM LIST:"+roomProperties.toString());

        //SERVER: receive and decode it
        Message value = mapper.readValue(transformedJson, Message.class);
        assert(value.getRoomId().equals(roomId));
        assert(value.getType().equals(Message.TYPE_DELETE));
        //TODO:check identity and room availability
        // we don't check this in here.
        findAndRemoveRoom(roomProperties,roomId);

        //SERVER: respond message
        msg = new ArrayList<>();
        msg.add(Message.transformMessagePairs(Message.TYPE_HEAD,Message.TYPE_ROOM_LIST));
        msg.add(Message.transformListPairs(Message.ROOM_LIST_HEAD,Message.roomListToJson(roomProperties)));
        transformedJson=Message.jsonCompose(msg);
        System.out.println("SERVER: Join room respond:"+transformedJson);

        //CLIENT: transform it back by Jackson
        value = mapper.readValue(transformedJson, Message.class);
        System.out.println(value.getRooms());
        assert(value.getType().equals(Message.TYPE_ROOM_LIST));
        assert(!value.getRooms().toString().contains(roomId));
    }
    @Test
    @DisplayName("Client request for room list")
    void clientRequestRoomList() throws IOException {
        List<Room> roomProperties = new ArrayList<>();
        generateRooms(roomProperties);
        System.out.println("SERVER: CURRENT ROOM LIST:"+roomProperties.toString());

        //CLIENT: REQUEST FOR ROOM LIST
        ArrayList<String> msg = new ArrayList<>();
        String identity = "Test_admin1";//get name from key attachment
        msg.add(Message.transformMessagePairs(Message.TYPE_HEAD,Message.TYPE_LIST));
        String transformedJson=Message.jsonCompose(msg);
        System.out.println("CLIENT: List room message:"+transformedJson);

        //SERVER: RECEIVE REQUEST AND DECODE, THEN RESPOND WITH THE LIST
        msg = new ArrayList<>();
        msg.add(Message.transformMessagePairs(Message.TYPE_HEAD,Message.TYPE_ROOM_LIST));
        msg.add(Message.transformListPairs(Message.ROOM_LIST_HEAD,Message.roomListToJson(roomProperties)));
        transformedJson=Message.jsonCompose(msg);
        System.out.println("SERVER: Join room respond:"+transformedJson);

        Message value = mapper.readValue(transformedJson, Message.class);
        System.out.println(value.getRooms());
        assert(value.getType().equals(Message.TYPE_ROOM_LIST));
        assert(value.getRooms().toString().contains(roomProperties.get(0).getRoomId()));
    }
    @Test
    @DisplayName("Client request for roommates info")
    void clientRequestRoomMates(){
        //
    }
    @Test
    @DisplayName("Client request for room creation")
    void clientRequestRoomCreation(){
        //
    }
    @Test
    @DisplayName("Client sends message")
    void clientSendsMessage(){
        //
    }

    @Test
    @DisplayName("Client quits")
    void clientQuit(){
        //
    }

}