import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientMain {
    private static final ExecutorService USER_INPUT_HANDLER = Executors.newSingleThreadExecutor();
    public static final int port = 4456;
    public static final String host = "127.0.0.1";
    private String userName;
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
                            byteBuffer.clear();
                            String message = new Scanner(System.in).nextLine();
                            byteBuffer.put(String.format("[%s]: %s", userName, message).getBytes(StandardCharsets.UTF_8));
                            byteBuffer.flip();
                            try {
                                while (byteBuffer.hasRemaining()) {
                                    socketChannel.write(byteBuffer);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            //TODO: quit
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
                //e.printStackTrace();
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
                System.out.println("got message from server: "+message);
                if(isCommand(message)){
                    //TODO: command logic
                    String[] res = message.split(":");
                    setUserName(res[1]);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private boolean isCommand(String message){
        if (message.charAt(0)=='#')
            return true;
        else return false;
    }

    public static void main(String[] args) {
        ClientMain client = new ClientMain();
        client.handle();
    }

    private void setUserName(String name) {
        this.userName=name;
    }
}
