import java.io.IOException;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {
  private static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(10);
  private static final String host = "127.0.0.1";
  private static ConcurrentHashMap<String, ArrayList<String>> chatRoom; //<key=RoomName, value=list of clients>
  private static ConcurrentHashMap<String, String> roomProperty; //<key=RoomName, value=owner>
  private static final String DEFAULT_ROOM = "MainHall";
  private static ConcurrentHashMap clients; //<key=clientName, value=current room>
  private static List<String> freeName = new ArrayList<String>();//
  private static int ClientCount = 1;

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
  private void leave(String message, Selector selector, SelectionKey selectionKey) {
    String mesg = "client left";
    System.out.println(mesg);
//    broadCast(mesg,null);
//    chats.remove(connection);
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
  private synchronized String getAvailableClientName(){
    if(freeName.size()>1)
      return freeName.get(0);
    else{
      String name = "Guest"+ ClientCount;
      ClientCount+=1;
      return name;
    }
  }

  private synchronized int joinRoom(Object client, Object roomName, Selector selector, SelectionKey selectionKey) throws IOException {
    String name = (String)client;
    String room = (String)roomName;
    String former = (String) clients.get(name) == null ?"":(String) clients.get(name);
    ArrayList<String> curClients = chatRoom.get(room);
    if(curClients == null) return -1;
    clients.put(name, room);
    curClients.add(name);
    chatRoom.remove(room);
    chatRoom.put(room,curClients);
    System.out.println("Debug: New client: "+ name+ " has joined to room: "+room);
    //Server respond and broadcast
    Message m = new Message();
    m.setType(Message.TYPE_JOIN);
    m.setIdentity(name);
    m.setFormer(former);
    m.setRoomId(room);
    broadCast(new Protocol(m),curClients,selector,selectionKey);
    m = new Message();
    // room content
    //m.setType(Message.TYPE_ROOM_CONTENTS);
    //m.set
    //singleResponse(new Protocol(m),name,selector);
    // room list
    m.setType(Message.TYPE_ROOM_LIST);
    ArrayList<RoomList> tempRoomList = new ArrayList<>();
    for(Map.Entry entry: chatRoom.entrySet()){
        tempRoomList.add(new RoomList((String)entry.getKey(),String.valueOf(((ArrayList<String>)entry.getValue()).size())));
    }
    m.setRooms(tempRoomList);
    singleResponse(new Protocol(m),name,selector);
    return 0;
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
      chatRoom = new ConcurrentHashMap<String, ArrayList<String>>();
      roomProperty = new ConcurrentHashMap<String,String>();
      //create default chatRoom
      chatRoom.put(DEFAULT_ROOM, new ArrayList<>());
      roomProperty.put(DEFAULT_ROOM,"");
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
          String clientName = getAvailableClientName();
          SelectionKey clientKey = socketChannel.register(selector, SelectionKey.OP_READ, clientName);
          // new identity assignment
          Message m = new Message();
          m.setType(Message.TYPE_NEW_IDENTITY);
          m.setIdentity(clientName);
          m.setFormer("");
          singleResponse(new Protocol(m), clientName, selector);
          // and assign it to the MainHall
          joinRoom(clientName,DEFAULT_ROOM, selector ,clientKey);

        }
      }catch(IOException e){
        e.printStackTrace();
      }
    }
    // otherwise, it is something the server can read.
    else if (selectionKey.isReadable()){
      //TODO: protocol implementation
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
          //TODO: quit logic implementation
          broadCast(new Protocol(message),chatRoom.get((String)clients.get(client)),selector,selectionKey);
          b.clear();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

}