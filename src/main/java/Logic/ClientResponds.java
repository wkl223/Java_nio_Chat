package Logic;

import Protocol.*;

import java.io.IOException;
import java.util.StringTokenizer;

/*
 * Construct Json message (Protocol) from Message object
 * */
public class ClientResponds {
    private static final String ALPHABETIC_PATTERN ="[A-Za-z]+";
    private static final String NUMERIC_PATTERN="[0-9]+";
    private static final String ALPHANUMERIC_PATTERN="[A-Za-z0-9]+";
    //since it is human typing command, so extra command checking function is required.
    public static String getCommand(String message){
        StringTokenizer tokenizer = new StringTokenizer(message," ");
        if(!(tokenizer.countTokens()<=2)){
            String firstArg =tokenizer.nextToken().substring(1);
            if(Protocol.isValidCommandType(firstArg))
                return firstArg;
        }
        return null;
    }
    public static String processMessage(String message) throws IOException {
        if(getCommand(message)!=null){
            StringTokenizer tokenizer = new StringTokenizer(message," ");
            String command = tokenizer.nextToken();
            String request = tokenizer.nextToken();
            switch(command){
                case Message.TYPE_IDENTITY_CHANGE:
                    Protocol answer =identityChange(request);
                    if(answer==null) return "Requested identity invalid or inuse";
                    return answer.encodeJson();
                case Message.TYPE_JOIN:
                    return joinRoom(request).encodeJson();
                case Message.TYPE_WHO:
                    return who(request).encodeJson();
                case Message.TYPE_LIST:
                    return list().encodeJson();
                case Message.TYPE_ROOM_CREATION:
                    return createRoom(request).encodeJson();
                case Message.TYPE_DELETE:
                    return deleteRoom(request).encodeJson();
                case Message.TYPE_QUIT:
                    return quit().encodeJson();
                default:
                    return "DEBUG - SOMETHING WRONG WITH THE PROCESS MESSAGE FUNCTION?";
            }
        }
        return message(message).encodeJson();
    }
    public static Protocol identityChange(String request){
        if(request.length()<3 || request.length() >16)
            return null;
        else if (String.valueOf(request.charAt(0)).matches(ALPHABETIC_PATTERN)&&request.matches(ALPHANUMERIC_PATTERN)){
            Message m = new Message();
            m.setType(Message.TYPE_IDENTITY_CHANGE);
            m.setIdentity(request);
            return new Protocol(m);
        }
        return null;
    }
    public static Protocol joinRoom(String request){
        Message m = new Message();
        m.setType(Message.TYPE_JOIN);
        m.setRoomId(request);
        return new Protocol(m);
    }
    public static Protocol who(String request){
        Message m = new Message();
        m.setType(Message.TYPE_WHO);
        m.setRoomId(request);
        return new Protocol(m);
    }
    public static Protocol list(){
        Message m = new Message();
        m.setType(Message.TYPE_LIST);
        return new Protocol(m);
    }
    public static Protocol createRoom(String request){
        if (request.length()<3 || request.length()>32) return null;
        if (!request.matches(ALPHABETIC_PATTERN)) return null;
        Message m = new Message();
        m.setType(Message.TYPE_ROOM_CREATION);
        m.setRoomId(request);
        return new Protocol(m);
    }
    public static Protocol deleteRoom(String request){
        Message m = new Message();
        m.setType(Message.TYPE_DELETE);
        m.setRoomId(request);
        return new Protocol(m);
    }
    public static Protocol message(String request){
        Message m = new Message();
        m.setType(Message.TYPE_MESSAGE);
        m.setContent(request);
        return new Protocol(m);
    }
    public static Protocol quit(){
        Message m = new Message();
        m.setType(Message.TYPE_QUIT);
        return new Protocol(m);
    }




}
