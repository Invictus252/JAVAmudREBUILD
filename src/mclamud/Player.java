package mclamud;

import java.net.Socket;

public class Player {
    
    public String description = "A rather ordinary looking player.";
    public String name = "Nobody";
    public String password = "Password1";
    public int location = 1; 
    public Socket socket;
    
    private Player(){}
    
    public Player(Socket sock){
        this.socket = sock;
    }
}