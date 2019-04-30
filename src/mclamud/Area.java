package mclamud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Area {
    public String title = "";
    public String description = "";
    
    public int[] exits = {0,0,0,0,0,0};
    public String ID = "";
    //couldn't quite understand Hashmap<>() went with what I know for items
    public ArrayList<String> items = new ArrayList<>(); 
    public Map<String, Player> players = new HashMap<>();
}