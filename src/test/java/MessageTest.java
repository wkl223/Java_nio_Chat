import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

class MessageTest {
    static ObjectMapper mapper=new ObjectMapper();
    String testMessage="Hello World!";
    String type = "chat";
    String sender = "me";
    String room = "MainH";
    String jsonMessage="{\"message\":\""+testMessage+"\",\"type\":\""+type+"\",\"sender\":\""+sender+"\"}";
    String jsonMessageWithRoom="{\"message\":\""+testMessage+"\",\"type\":\""+type+"\",\"sender\":\""+sender+"\",\"room\":\""+room+"\"}";
    @Test
    @DisplayName("Simple get message")
    void getMessage() throws JsonProcessingException {
        Message value = mapper.readValue(jsonMessage, Message.class);
        System.out.println("original msg:"+jsonMessage);
        System.out.println("decoded msg:"+value.getMessage());
        assert(value.getMessage().equals(testMessage));
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
        assert(value.getSender().equals(sender));
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
        msg.add(Message.transformMessagePairs(Message.SENDER_HEAD,sender));
        msg.add(Message.transformMessagePairs(Message.ROOM_HEAD,room));
        String transformedJson=Message.jsonCompose(msg);
        System.out.println("original message:"+jsonMessageWithRoom);
        System.out.println("transformed json:"+transformedJson);
        assert(transformedJson.equals(jsonMessageWithRoom));
        //transform it back by Jackson
        Message value = mapper.readValue(transformedJson, Message.class);
        assert(value.getRoom().equals(room));
        assert(value.getSender().equals(sender));
        assert(value.getType().equals(type));
        assert(value.getMessage().equals(testMessage));
    }

}