import java.util.List;

public class Message {
    private String type;
    private String message;
    private String sender;
    private String room;
    public static final String MESSAGE_HEAD = "message";
    public static final String TYPE_HEAD = "type";
    public static final String SENDER_HEAD = "sender";
    public static final String ROOM_HEAD ="room";
    public static final String QUIT_COMMAND = "quit";
    public static final String KV_DELIMITER = ":";
    public static final String TUPLE_DELIMITER =",";


    public String getMessage(){
        return this.message;
    }
    public String getType(){
        return this.type;
    }
    public String getSender(){
        return this.sender;
    }
    public String getRoom(){
        return this.room;
    }

    public static String addHeadAndTail(String message, String head, String tail){
        String out = message;
        out = head+out+tail;
        return out;
    }
    public static String transformMessagePairs(String head, String content){
        String message = addHeadAndTail(head,"\"","\"")+KV_DELIMITER+addHeadAndTail(content,"\"","\"");
        return message;
    }
    public static String jsonCompose(String[] content){
        String out="";
        for(String s :content){
            out += s+TUPLE_DELIMITER;
        }
        return addHeadAndTail(out,"{","}");
    }
    public static String jsonCompose(List<String> content){
        String out="";
        for(String s :content){
            out += s+TUPLE_DELIMITER;
        }
        out = out.substring(0,out.length()-1);//ignore the last comma
        return addHeadAndTail(out,"{","}");
    }
}
