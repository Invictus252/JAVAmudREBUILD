package mclamud;

import java.net.Socket;
import java.util.ArrayList;

public class Player {
    
    public String description = "A rather ordinary looking player.";
    public String name = "Nobody";
    public String password = "Password1";
    public int location = 1; 
    public Socket socket;
    public ArrayList<String> inventory = new ArrayList<>();
    
    private Player(){}
    
    public Player(Socket sock){
        this.socket = sock;
    }
}