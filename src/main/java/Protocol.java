import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    public String encodeJson(String type, List<String> message){
        switch (type){
            case Message.TYPE_DELETE:
                return Message.TYPE_DELETE;
            case Message.TYPE_JOIN:
                return clientRequestJoinRoom(message);
            case Message.TYPE_NEW_IDENTITY:
                return serverRespondidentityChange(message);
            case Message.TYPE_IDENTITY_CHANGE:
                return clientRequestIdentityChange(message);
            case Message.TYPE_ROOM_CHANGE:

            default:
                return INVALID_JSON;
        }
    }
    private String serverRespondidentityChange(List<String> message){
        ArrayList<String> msg = new ArrayList<>();
        //SERVER: prepare a new identity to client
        //example: type = newidentity, former = "former", identity = "identity"
        // former and identity are server message. Thus message size should be 2.
        if(message.size()!=2) return INVALID_JSON;
        else {
            msg.add(Message.transformMessagePairs(Message.TYPE_HEAD, Message.TYPE_NEW_IDENTITY));
            msg.add(Message.transformMessagePairs(Message.FORMER_HEAD, message.get(0)));
            msg.add(Message.transformMessagePairs(Message.IDENTITY_HEAD, message.get(1)));
        }
        return Message.jsonCompose(msg);
    }
    private String clientRequestIdentityChange(List<String> message){
        //CLIENT: prepare a new identity message to server
        //CLIENT: Change identity message:{"type":"identitychange","identity":"Guest111"}
        if(message.size()!=1) return INVALID_JSON;
        ArrayList<String> msg = new ArrayList<>();
        msg.add(Message.transformMessagePairs(Message.TYPE_HEAD,Message.TYPE_IDENTITY_CHANGE));
        msg.add(Message.transformMessagePairs(Message.IDENTITY_HEAD,message.get(0)));
        return Message.jsonCompose(msg);
    }
    private String clientRequestJoinRoom(List<String> message){
        //CLIENT: Join room message:{"type":"join","roomid":"HelloooooRoom"}
        //CLIENT: send join room message
        ArrayList<String> msg = new ArrayList<>();
        if(message.size()!=1) return INVALID_JSON;
        msg.add(Message.transformMessagePairs(Message.TYPE_HEAD,Message.TYPE_JOIN));
        msg.add(Message.transformMessagePairs(Message.ROOM_DESTINATION_HEAD,message.get(0)));
        return Message.jsonCompose(msg);
    }
    private String serverRespondRoomChange(List<String> message){
        //SERVER: Join room respond:{"type":"roomchange","identity":"Guest111","former":"MainHall","roomid":"HelloooooRoom"}
        ArrayList<String> msg = new ArrayList<>();
        if(message.size()!=3) return INVALID_JSON;
        msg.add(Message.transformMessagePairs(Message.TYPE_HEAD,Message.TYPE_ROOM_CHANGE));
        msg.add(Message.transformMessagePairs(Message.IDENTITY_HEAD,message.get(0)));
        msg.add(Message.transformMessagePairs(Message.FORMER_HEAD,message.get(1)));
        msg.add(Message.transformMessagePairs(Message.ROOM_DESTINATION_HEAD,message.get(2)));
        return Message.jsonCompose(msg);
    }
}
