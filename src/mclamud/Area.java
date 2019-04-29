package mclamud;

import java.util.HashMap;
import java.util.Map;

public class Area {
    public String title = "";
    public String description = "";
    public int[] exits = {0,0,0,0};
    public Map<String, Player> players = new HashMap<>();
}