import java.util.List;

public class Message {
    private String type;
    private String message;
    private String identity;
    private String room;
    private String former;
    // message head constant
    public static final String MESSAGE_HEAD = "content";
    public static final String TYPE_HEAD = "type";
    public static final String SENDER_HEAD = "sender";
    public static final String ROOM_HEAD ="room";
    public static final String ROOM_DESTINATION_HEAD ="roomid";
    public static final String COUNT_HEAD = "count";
    public static final String ROOM_LIST_HEAD ="rooms";
    // type constant
    public static final String TYPE_QUIT = "quit";
    public static final String TYPE_NEW_IDENTITY = "newidentity";
    public static final String TYPE_JOIN = "join";
    public static final String TYPE_ROOM_CONTENTS = "roomcontents";
    public static final String TYPE_OWNER = "owner";
    public static final String TYPE_WHO = "who";
    public static final String TYPE_ROOM_LIST = "roomlist";
    public static final String TYPE_ROOM_CREATION = "createroom";
    public static final String TYPE_DELETE = "delete";
    // delimiters and marks
    public static final String KV_DELIMITER = ":";
    public static final String TUPLE_DELIMITER =",";
    public static final String ROOM_OWNER_MARK ="*";

    public String getMessage(){
        return this.message;
    }
    public String getType(){
        return this.type;
    }
    public String getIdentity(){
        return this.identity;
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
