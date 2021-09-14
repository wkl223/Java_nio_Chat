import Logic.ServerReception;
import Protocol.Entity.Room;
import Logic.ServerResponds;
import Protocol.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class ServerMain {
  private static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(10);
  private static final String host = "127.0.0.1";
//  private static ConcurrentHashMap<String, ArrayList<String>> chatRoom; //<key=RoomName, value=list of clients>
//  private static ConcurrentHashMap<String, String> roomProperty; //<key=RoomName, value=owner>
  private static List<Room> chatRoom;
  private static ConcurrentHashMap clients; //<key=clientName, value=current room>
  private static List<String> freeName = new ArrayList<String>();//
  public static final String DEFAULT_ROOM = "MainHall";
  private Room defaultRoom = new Room(DEFAULT_ROOM,"0","");

  public static void main(String[] args) {
    int port =4444;//default to port 4444
    try{
      if (args.length>2){
        throw new NumberFormatException();
      }
      else if (args.length==1) {
        port = Integer.parseInt(args[0]);
      }
      new ServerMain().handle(port);
    } catch (NumberFormatException e) {
      System.err.println("Invalid argument input, example:java -jar chatserver.jar <port>");
    }
  }

  private SelectionKey getKey(Selector selector,String clientName){
    Set<SelectionKey> keys = selector.keys();
    for(SelectionKey sk:keys) {
      if (sk.attachment() != null) {
        if (sk.attachment().equals(clientName))
          return sk;
      }
    }
    return null;
  }
  private synchronized void singleResponse(Protocol message, String name, Selector selector) throws IOException {
    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
    byteBuffer.put(message.encodeJson().getBytes(StandardCharsets.UTF_8));
    byteBuffer.flip();
    SocketChannel socketChannel = (SocketChannel) getKey(selector,name).channel();
    while (byteBuffer.hasRemaining()) {
      try {
            socketChannel.write(byteBuffer);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    byteBuffer.clear();
  }
  private synchronized void broadCast(Protocol message, ArrayList<String> multicastList, Selector selector, SelectionKey selectionKey) throws IOException {
    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
    byteBuffer.put(message.encodeJson().getBytes(StandardCharsets.UTF_8));
    byteBuffer.flip();
    byteBuffer.mark();// because we need to send this multiple times.
    Set<SelectionKey> keys = selector.keys();
    System.out.println("DEBUG - broadcast to users:"+multicastList.toString());
    for(SelectionKey k : keys){
      //ignore the sender + server itself
      if(selectionKey.equals(k) || !(k.channel() instanceof SocketChannel)){
        continue;
      }
      else if (multicastList.contains(k.attachment())) {
        SocketChannel socketChannel = (SocketChannel) k.channel();
        while (byteBuffer.hasRemaining()) {
          try {
            socketChannel.write(byteBuffer);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
        byteBuffer.reset();
      }
    }
    byteBuffer.clear();
  }


  public void handle(int port) {
    ServerSocketChannel serverSocketChannel;

    try {
      serverSocketChannel = ServerSocketChannel.open();
      serverSocketChannel.configureBlocking(false);
      serverSocketChannel.bind(new InetSocketAddress(host, port));
      Selector selector = Selector.open();

      serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
      System.out.println("server is up at port: "+ port);
      chatRoom = new ArrayList<>();
      //create default chatRoom
      chatRoom.add(defaultRoom);
      clients = new ConcurrentHashMap<String,SelectionKey>();
      while(true){
        int eventCountTriggered = selector.select();
        if (eventCountTriggered <=0){
          continue;
        }
        // if there is an event.
        Set<SelectionKey> selectionKeys = selector.selectedKeys();
        //TODO: since selector is a blocking service. We need to implement a thread pool for this!
        for(SelectionKey s: selectionKeys){
          //System.out.println("DEBUG: new event: "+s.toString());
          selectionKeyHandler(s,selector);
        }
        selectionKeys.clear();
      }
    } catch (IOException e) {
      // handle the exception
      System.out.println("connection reset by peer");
    }
  }
  private void selectionKeyHandler(SelectionKey selectionKey, Selector selector){
    // if a connection event
    if (selectionKey.isAcceptable()){
      try{
        SocketChannel socketChannel = ((ServerSocketChannel)selectionKey.channel()).accept();
        if(socketChannel!=null) {
          System.out.println("DEBUG: client connection established: " + socketChannel.socket().getPort());
          //set unblocking mode
          socketChannel.configureBlocking(false);
          // register the client to the selector for event monitoring. attach Username.
          Protocol respond = ServerResponds.newIdentity(clients,freeName, ServerResponds.NEW_USER_INDICATOR, ServerResponds.NEW_USER_INDICATOR);
          String clientName = respond.getMessage().getIdentity();
          SelectionKey clientKey = socketChannel.register(selector, SelectionKey.OP_READ, clientName);
          // add to default room, and add client to the clients name collection.
          clients.put(clientName,DEFAULT_ROOM);
          chatRoom.get(0).users.add(clientName);
          // new identity assignment
          singleResponse(respond, clientName, selector);
        }
      }catch(IOException e){
        e.printStackTrace();
      }
    }
    // otherwise, it is something the server can read.
    else if (selectionKey.isReadable()){
      SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
      if(socketChannel != null) {
        ByteBuffer b = ByteBuffer.allocate(1024); //1024 byte per read
        try {
          // while not reach to EOF
          while (socketChannel.read(b) > 0) {
          }
          b.flip(); //reset the cursor at 0.
          String message = String.valueOf(StandardCharsets.UTF_8.decode(b));
          String client = (String)selectionKey.attachment();
          System.out.println("DEBUG: received client message: " + message+" from client:"+client);
          processMessageAndRespond(message,client,selector,selectionKey);
          b.clear();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }
  public void processMessageAndRespond(String message, String client, Selector selector,SelectionKey selectionKey) throws IOException {
    //TODO: broadcast or single response logic per each message type.
    Protocol p = new Protocol(message);
    Protocol answer = null;
    List<String> affectedClients = null;
    switch (p.decodeJson().getType()){
        case Message.TYPE_DELETE: {
          Room defaultRoom = ServerResponds.findRoom(chatRoom, DEFAULT_ROOM);
          Room affectedRoom =ServerResponds.findRoom(chatRoom,p.getMessage().getRoomId());
          answer = ServerReception.deleteRoom(p, client, clients, chatRoom);
          if(affectedRoom != null) {
              affectedClients = affectedRoom.getUsers();
            }
            if (answer.getMessage().isSuccessed()){
                for(String c: affectedClients){
                    Protocol temp = ServerResponds.roomChange(chatRoom,c,affectedRoom.getRoomId(),DEFAULT_ROOM);
                    //as they are changing rooms, note that it is not a formal room change message
                    //so that no follow-up message in this case. e.g., ROOM CONTENTS and ROOM LIST.
                    singleResponse(temp,c,selector);
                    broadCast(temp, (ArrayList<String>) defaultRoom.users,selector,selectionKey);
                    defaultRoom.users.add(c);
                    clients.remove(c);
                    clients.put(c,DEFAULT_ROOM);
                }
            }
            singleResponse(answer,client,selector);
            break;
        }
        case Message.TYPE_JOIN: {
            Room formerRoom = ServerResponds.findRoom(chatRoom, (String) clients.get(client));
            answer = ServerReception.joinRoom(p, client, clients, chatRoom);
            if (answer.getMessage().getRoomId().equals(DEFAULT_ROOM) && !answer.getMessage().getFormer().equals(DEFAULT_ROOM)) {
                singleResponse(ServerResponds.roomContents(DEFAULT_ROOM, chatRoom), client, selector);
                singleResponse(ServerResponds.roomList(chatRoom), client, selector);
            }
            else if(answer.getMessage().isSuccessed()){
              Room targetRoom = ServerResponds.findRoom(chatRoom,p.getMessage().getRoomId());
              List<String> affectedUser;
              if((targetRoom.users !=null && formerRoom.users!=null)||(targetRoom.users.size()!=0&&formerRoom.users.size()!=0)){
                affectedUser = new ArrayList<>(formerRoom.users);
                affectedUser.addAll(targetRoom.users);
              }
              else{
                affectedUser = (targetRoom.users == null)||(targetRoom.users.size()==0)? formerRoom.users:targetRoom.users;
              }
              broadCast(answer, (ArrayList<String>) affectedUser,selector,selectionKey);
              singleResponse(answer,client,selector);
            }
            else singleResponse(answer,client,selector);
            break;
        }
      case Message.TYPE_IDENTITY_CHANGE: {
        Room affected = ServerResponds.findRoom(chatRoom, (String) clients.get(client));
        answer = ServerReception.identityChange(p, client, clients, freeName, chatRoom);
        String name = client;
        if(answer.getMessage().isSuccessed()){
          broadCast(answer, (ArrayList<String>) affected.users,selector,selectionKey);
          getKey(selector,answer.getMessage().getFormer()).attach(answer.getMessage().getIdentity());
          name = answer.getMessage().getIdentity();
        }
        singleResponse(answer,name,selector);
        break;
      }
      case Message.TYPE_WHO: {
        answer = ServerReception.who(p, client, chatRoom);
        if(answer.getMessage().isSuccessed()) singleResponse(answer,client,selector);
        break;
      }
      case Message.TYPE_QUIT: {
        Room affected = ServerResponds.findRoom(chatRoom, (String) clients.get(client));
        answer = ServerReception.quit(p, client, chatRoom, clients);
        broadCast(answer, (ArrayList<String>) affected.users,selector,selectionKey);
        singleResponse(answer,client,selector);
        break;
      }
      case Message.TYPE_LIST: {
        answer = ServerReception.list(p, client, chatRoom);
        singleResponse(answer,client,selector);
        break;
      }
      case Message.TYPE_ROOM_CREATION: {
        answer = ServerReception.createRoom(p, client, chatRoom);
        if(answer.getMessage().isSuccessed()) {
          //chatRoom.add(new Room(p.getMessage().getRoomId(), "0", client));
          System.out.println("room created: "+p.getMessage().getRoomId());
        }
        singleResponse(answer,client,selector);
        break;
      }
      case Message.TYPE_MESSAGE: {
        Room affected = ServerResponds.findRoom(chatRoom, (String) clients.get(client));
        System.out.println(client);
        System.out.println((String) clients.get(client));
        answer = ServerReception.message(p, client);
        broadCast(answer, (ArrayList<String>) affected.users,selector,selectionKey);
        break;
      }
      default:
        System.out.println("DEBUG - Something is wrong in reception service");
    }
  }

}