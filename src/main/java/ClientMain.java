import Logic.ClientReception;
import Logic.ClientResponds;
import Protocol.Message;
import Protocol.Protocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ClientMain {
    private static final ExecutorService USER_INPUT_HANDLER = Executors.newSingleThreadExecutor();
    public static final String DEFAULT_ROOM = "MainHall";
    public static final int port = 4456;
    public static final String host = "127.0.0.1";

    private String userName;
    private String currentRoom;
    private String prefix;
    private Map<String,String> request = new HashMap<>();

    public void handle() {
        try {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);// set to unblocking mode
            Selector selector = Selector.open();
            socketChannel.register(selector, SelectionKey.OP_CONNECT);//register channel to selector by connection event
            socketChannel.connect(new InetSocketAddress(host, port));
            while (true) {
                int eventCountTriggered = selector.select();
                if (eventCountTriggered <= 0) {
                    continue;
                }
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey selectionKey : selectionKeys) {
                    selectionKeyHandler(selectionKey, selector);
                }
                selectionKeys.clear();
            }
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void selectionKeyHandler(SelectionKey selectionKey, Selector selector) {
        if (selectionKey.isConnectable()) {
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
            try {
                if (socketChannel.isConnectionPending()) {
                    socketChannel.finishConnect();
                    System.out.println("Connection successd");
                    socketChannel.configureBlocking(false);
                    socketChannel.register(selector, SelectionKey.OP_READ);
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);//1024 bytes
                    USER_INPUT_HANDLER.execute(() -> {
                        while (!Thread.currentThread().isInterrupted()) {
                            System.out.print(prefix);
                            byteBuffer.clear();
                            String message = new Scanner(System.in).nextLine();
                            try {
                                String m =ClientResponds.processMessage(message,request);
                                System.out.println("DEBUG - client side: "+m);
                                if(m!=null) {
                                    byteBuffer.put(new Protocol(m).encodeJson().getBytes(StandardCharsets.UTF_8));
                                    byteBuffer.flip();
                                    while (byteBuffer.hasRemaining()) {
                                        socketChannel.write(byteBuffer);
                                    }
                                }
                            } catch (IOException e) {
                               //System.out.println("Invalid input");
                            } catch(NullPointerException e){
                                //System.out.println("Invalid input");
                            }
                            catch(Exception e){ System.out.println("Strange error?");
                            }
                        }
                        try {
                            selector.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        USER_INPUT_HANDLER.shutdown();
                        System.out.println("input handler is shutdown");
                    });
                }
            } catch (IOException e) {
                System.err.println("ERROR - Cannot connect to server!!");
                System.exit(-1);
            }
        } else if (selectionKey.isReadable()) {
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
            ByteBuffer b = ByteBuffer.allocate(1024);
            try {
                while (socketChannel.read(b) > 0) {
                }
                b.flip();
                String message = String.valueOf(StandardCharsets.UTF_8.decode(b));
                b.clear();
                System.out.println("DEBUG server msg: "+message);
                String serverRespond = processMessageAndRepresent(message,userName);
                System.out.println(prefix+serverRespond);
                System.out.print(prefix);// follow-up prefix as command prompt
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static void main(String[] args) {
        ClientMain client = new ClientMain();
        client.handle();
    }
    public String processMessageAndRepresent(String message, String name) throws IOException {
        Protocol p = new Protocol(message);
        String client = name;
        switch (p.decodeJson().getType()){
            case Message.TYPE_NEW_IDENTITY: {
                String former = p.getMessage().getFormer();
                String identity =p.getMessage().getIdentity();
                if((!former.equals(identity)) && (former.equals(client)||former.equals(""))){
                    System.out.println("DEBUG - NEW IDENTITY successed:"+identity);
                    this.userName=identity;
                    client = this.userName;
                    if(former.equals("")) {
                        this.currentRoom = DEFAULT_ROOM;
                    }
                    this.prefix =String.format("[%s]: %s> ", this.currentRoom, this.userName);
                }
                return ClientReception.newIdentity(p, client);
            }
            case Message.TYPE_ROOM_CHANGE: {
                String formerRoom = p.getMessage().getFormer();
                String roomId = p.getMessage().getRoomId();
                String identity = p.getMessage().getIdentity();
                if (!formerRoom.equals(roomId) && identity.equals(userName)){
                    this.currentRoom = roomId;
                    this.prefix = String.format("[%s]: %s> ", this.currentRoom, this.userName);
                    System.out.println("DEBUG- new prefix: "+prefix);
                }
                return ClientReception.roomChange(p, client);
            }
            case Message.TYPE_ROOM_CONTENTS:
                return ClientReception.roomContents(p);
            case Message.TYPE_ROOM_LIST: {
                return ClientReception.roomList(p,request);
            }
            case Message.TYPE_MESSAGE:
                return ClientReception.message(p);
            default:
                System.out.println("DEBUG: Server sends a message with incorrect type!");
                return null;
        }
    }
}
