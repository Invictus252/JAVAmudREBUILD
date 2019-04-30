package mclamud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Area {
    public String title = "";
    public String description = "";
    public String ID = "";
    public int[] exits = {0,0,0,0,0,0};
    public ArrayList<String> items = new ArrayList<>();
    public Map<String, Player> players = new HashMap<>();
}