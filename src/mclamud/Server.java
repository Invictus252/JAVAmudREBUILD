package mclamud;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static ArrayList<Player> curUsers = new ArrayList<>();
    public static void main(String[] args) throws IOException {
        final int PORT = 6666;
        final int THREADS = 500;
        boolean exit = false;
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            
            System.out.println("Java MUD 1.0");
            System.out.println("Server is Listening on port " + PORT);
            ServerSocket ssock = new ServerSocket(PORT);

            while(!exit){
                Socket psock;
                psock = ssock.accept();
                Runnable thread = new PlayerThread(psock);
                pool.execute(thread);
                String client = psock.getInetAddress().toString();
                System.out.println(client + " connected on local port " + psock.getPort() + ".");
             }
        } catch (Exception ex){
            System.out.println("Something went wrong.");
        }
        pool.shutdownNow();
    }
}
