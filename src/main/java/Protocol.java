import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Protocol {
    static ObjectMapper mapper = new ObjectMapper();
    public static final String INVALID_JSON = "INVALID";
    Message m;

    public Protocol(String message) {
        try {
            m = mapper.readValue(message, Message.class);
        }
        catch (JsonProcessingException e){
            System.out.println("PROTOCOL ERROR: INVALID INPUT OBJECT");
        }
        catch (Exception e){
            //just in case
            System.out.println("PROTOCOL ERROR: INVALID INPUT OBJECT");
        }
    }
    public Protocol(Message m){
        this.m = m;
    }

    public String encodeJson() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        switch (m.getType()){
            case Message.TYPE_DELETE:
            case Message.TYPE_JOIN:
            case Message.TYPE_NEW_IDENTITY:
            case Message.TYPE_IDENTITY_CHANGE:
            case Message.TYPE_LIST:
            case Message.TYPE_QUIT:
            case Message.TYPE_ROOM_CONTENTS:
            case Message.TYPE_ROOM_CREATION:
            case Message.TYPE_ROOM_CHANGE:
            case Message.TYPE_ROOM_LIST:
            case Message.TYPE_MESSAGE:
                mapper.writeValue(out,m);
                return new String(out.toByteArray());
            default:
                return INVALID_JSON;
        }
    }


}
