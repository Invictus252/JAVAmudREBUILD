package mclamud;

import java.io.*;
import java.net.Socket; //new
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class World {
    private final static int MIN_PASSWORD_LENGTH = 8;
    private final static int MIN_PLAYERNAME_LENGTH = 3;
    private final static int MAX_PLAYERNAME_LENGTH = 15;
    private final static int NUMBER_DIRECTIONS = 4; //for example: N,S,E,W
    public static Map<Integer, Area>areaMap = new HashMap<>();
    
    /************************************************************************
     * Displays an area to the remote player.
     * @param areaId An integer containing the ID of the area
     * @param p Player to return output to
     * @return boolean true = succeeded, false if not.
     ************************************************************************/
    public synchronized static boolean displayArea(int areaId, Player p) {
        boolean outcome = true;
        PrintWriter out = null;
        Area a = getArea(areaId);
        if (a != null && p != null){
            try {
                out = new PrintWriter(p.socket.getOutputStream(), true);
            } catch (IOException ex) {
                outcome = false;
            }

            if (outcome && out != null){
                String[] portals = {"north","south","east","west"};   
                out.println(a.title.trim());
                out.println(a.description);

                //List the available exits    
                out.print("Exits available: ");
                for (int i = 0; i < NUMBER_DIRECTIONS; i++){
                    if (a.exits[i] != 0){
                        out.print(portals[i] + " ");
                    }
                }
                out.print("\n");
                
                //List the players in the area, excluding self
                for (Player areaPlayers : a.players.values()){
                    if (!areaPlayers.name.equalsIgnoreCase(p.name)){
                        out.println(areaPlayers.name + " is here.");
                    }
                }
                out.println("[" + areaId + "]"); //Remove this line after development
            }
        }
        return outcome;
    }
     
    /********************************************************************
     * Reads the area file specified in areaId and returns an area object
     * @param areaId An integer containing the ID of the area.
     * @return Area object
     ********************************************************************/
    // 
    private synchronized static Area readAreaFromFile(int areaId){
        Area a = new Area();
        String contents = null;
        String areaFile = "areas/" + String.valueOf(areaId) + ".area";
        File file = new File(areaFile);
        try{    
            if (file.exists()){
                contents = new String(Files.readAllBytes(Paths.get(areaFile)));
            } else {
                a = null;
                contents = null;
            }
        }
        catch(IOException e){
            System.out.println("An error occurred reading " + areaFile + ".");
        }
        if (a != null && contents != null){
            int indexNext;
            do {
                int indexStart = contents.indexOf("[");
                int indexEnd = contents.indexOf("]");
                String name = contents.substring(indexStart + 1, indexEnd).toLowerCase();
                indexNext = contents.indexOf("[", indexStart + 1);
                String value;
                if (indexNext < 0){
                    contents = contents.substring(indexEnd + 1);
                    value = contents;
                } else {
                    value = contents.substring(indexEnd + 1, indexNext);
                    contents = contents.substring(indexNext);
                }
                switch(name){
                    case "description":
                        a.description = value.trim();
                        break;
                    case "title":
                        a.title = value.trim();
                        break;
                    case "exits":
                        String[] exits = value.trim().split("\\s*,\\s*");
                        for (int i = 0; i < exits.length; i++){
                            a.exits[i] = Integer.parseInt(exits[i]);
                        }
                        break;
                    default:
                }
            } while(indexNext >= 0);
        }
    return a;
    }
    
    /**********************************************************************
     * Determines whether the requested area exists as an area object, or
     * needs to be read from disk and dispatches accordingly.
     * @param areaId Integer containing the ID of the area.
     * @return Area object
     **********************************************************************/
    private synchronized static Area getArea(int areaId){
        Area a;
        if (areaMap.containsKey(areaId)){
            a = areaMap.get(areaId);
        } else { 
            a = readAreaFromFile(areaId);
            areaMap.put(areaId, a);
        }
        return a;
    }
    
    /***********************************************************************
     * Moves a player from one area to another
     * @param p Player object representing the connected player
     * @param newArea An int containing the area to which the player is 
     * being moved to.
     ***********************************************************************/
    public synchronized static void movePlayer(Player p, int newArea){
        Area a = getArea(p.location);
        a.players.remove(p.name.toLowerCase());
        Area b = getArea(newArea);
        b.players.put(p.name.toLowerCase(), p);
        p.location = newArea;
    }
    
    /************************************************************************
     * Removes a player object from an area. The area the player is being
     * removed from is in player.location.
     * @param p Player object being removed
     ************************************************************************/
    public synchronized static void removePlayer(Player p){
        Area a = getArea(p.location);
        a.players.remove(p.name.toLowerCase());
    }
    
    /************************************************************************
     * writePlayer saves the current Player object to a playername.area file.
     * @param p Player object being written to file
     * @return boolean indication failure or success
     ************************************************************************/
    public synchronized static boolean writePlayer(Player p){
        boolean outcome = true;  
        String path = "players/" + p.name.concat(".player").toLowerCase();
        File f = new File(path);
        String passwordHash = Integer.toHexString(p.password.hashCode());
        try{
            try (PrintWriter pw = new PrintWriter(f, "UTF-8")) {
                pw.println("[description]" + p.description);
                pw.println("[name]" + p.name);
                pw.println("[password]" + passwordHash);
                pw.println("[location]" + String.valueOf(p.location));
            }
        } catch(FileNotFoundException | UnsupportedEncodingException e){
            outcome = false;
            System.out.println("An error occurred writing " + path + ".");
        }
        return outcome;
    }
    
    /************************************************************************
     * doesPlayerExist tests to see if an .area file exists for the player name.
     * @param playerName A string containing the player name
     * @return boolean - true if it exists and false if it does not.
     ************************************************************************/
    public synchronized static boolean doesPlayerExist(String playerName){
        File f = new File("players/" + playerName.concat(".player").toLowerCase());
        return f.exists();
    }
    
    /*************************************************************************
     * isValidPassword checks to see if password meets length and complexity
     * requirements, which are: 1 UPPER, 1 lower, and 1 number.
     * @param playerPassword
     * @return boolean - true if password meets requirements, false if not.
     *************************************************************************/
    public synchronized static boolean isValidPassword(String playerPassword){
        boolean outcome = true;
        int flags = 0;
        if (playerPassword.length() >= MIN_PASSWORD_LENGTH){
            for (int i = 0; i < playerPassword.length(); i++){
                if(Character.isUpperCase(playerPassword.charAt(i))){
                    flags = flags | 1;
                }
                if(Character.isLowerCase(playerPassword.charAt(i))){
                    flags = flags | 2;
                }
                if(Character.isDigit(playerPassword.charAt(i))){
                    flags = flags | 4;
                }
            }
            if (flags != 7){
                outcome = false;
            }
        } else {
            outcome = false;
        }
        return outcome;
    }
    
    /*************************************************************************
     * isValidPlayername checks to see if the player name meets length and 
     * character restrictions, which are: length, letters, numbers, and the
     * underscore _.
     * requirements, which are: 1 UPPER, 1 lower, and 1 number.
     * @param playerName String to be checked for validity
     * @return boolean - true if player name meets requirements, false if not.
     *************************************************************************/
    public synchronized static boolean isValidPlayername(String playerName){
        boolean outcome = true;
        int flags;
        if (playerName.length() >= MIN_PLAYERNAME_LENGTH && 
            playerName.length() <= MAX_PLAYERNAME_LENGTH){
            for (int i = 0; i < playerName.length(); i++){
                flags = 0;
                if(Character.isLetter(playerName.charAt(i))){
                    flags = flags | 1;
                }
                if(Character.isDigit(playerName.charAt(i))){
                    flags = flags | 2;
                }
                if(playerName.charAt(i) == '_'){
                    flags = flags | 4;
                }
                if (flags == 0){
                    outcome = false;
                    break;
                }
            }
        } else {
            outcome = false;
        }
        return outcome;
    }
// ************************************************************************
// Begin list of methods for performing commands
//*************************************************************************
    
/**************************************************************************
 * doWalk parses the direction from the players command line and calculates the 
 * correct area to move the player to. The areas IDs are within the area 
 * object pointed to by player.location.
 * @param p Player object
 * @param direction command line string entered by player
 * @return boolean - true if direction points to valid area ID, false if not.
 */
    public synchronized static boolean doWalk(Player p, String direction){
        boolean outcome = false;
        String[] dirList = {"north","south","east","west"};
        String[] dirAbbr = {"n","s","e","w"};
        int areaIndex = -1;
        direction = direction.trim().toLowerCase();
        for (int i = 0; i < NUMBER_DIRECTIONS; i++){
            if (dirList[i].equals(direction) || dirAbbr[i].equals(direction)){
                areaIndex = i;
                break;
            }
        }
        if (areaIndex >= 0){
            Area a = getArea(p.location);
            if (a != null){
                if (a.exits[areaIndex] > 0){
                    sendMessageToArea(p, p.name + " has exited " + 
                        dirList[areaIndex] + "."); //new
                    movePlayer(p, a.exits[areaIndex]);
                    sendMessageToArea(p, p.name + " has arrived."); //new
                    outcome = true;
                }
            }
        }
        return outcome;        
    }
    
    public synchronized static boolean sendMessageToArea(Player p, String message){ //new
        boolean outcome = true;
        Area a = getArea(p.location);
        PrintWriter out;
        if (a != null){
            for (Player areaPlayers : a.players.values()){
                try {
                    if (!areaPlayers.name.equalsIgnoreCase(p.name)){
                        Socket psock = areaPlayers.socket;
                        out = new PrintWriter(psock.getOutputStream(), true);
                        out.println(message);
                    }
                } catch (IOException e) {
                    outcome = false;
                }
            }
        } else {
            outcome = false;
        }
        return outcome;
    }
}