package mclamud;

import java.io.*;
import java.net.Socket; //new
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class World {
    private final static int MIN_PASSWORD_LENGTH = 8;
    private final static int MIN_PLAYERNAME_LENGTH = 3;
    private final static int MAX_PLAYERNAME_LENGTH = 15;
    // +2 for up and down from original setting @ 4
    private final static int NUMBER_DIRECTIONS = 6;
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
                String[] portals = {"north","south","east","west","up","down"};   
                out.println(Ansi.BLUE + a.title.trim() + Ansi.SANE);
                out.println(a.description);

                //List the available exits    
                out.print("Exits available: ");
                for (int i = 0; i < NUMBER_DIRECTIONS; i++){
                    if (a.exits[i] != 0){
                        out.print(portals[i] + " ");
                    }
                }
                out.print("\n");
                
                String itemsAvail ="Items available: ";
                itemsAvail = a.items.stream().filter((x) -> (!x.equals(""))).map((x) -> x + " ").reduce(itemsAvail, String::concat);
                out.println(itemsAvail);
                
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
                    case "items":
                        String[] items = value.trim().split("\\s*,\\s*");
                        a.items.addAll(Arrays.asList(items));
                        break;    
                    case "id":
                        a.ID = value.trim();
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
     * writePlayer saves the current Player object to a playername.player file.
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
                pw.print("[inventory] ");
                for(int i=0;i < p.inventory.size();i++){
                    pw.print(p.inventory.get(i));
                    if(i != p.inventory.size())
                        pw.print(",");
                }
            }
        } catch(FileNotFoundException | UnsupportedEncodingException e){
            outcome = false;
            System.out.println("An error occurred writing " + path + ".");
        }
        return outcome;
    }
    
    /************************************************************************
     * writeArea saves the current Area object to a areaID.area file.
     * @param a Area object being written to file
     * @return boolean indication failure or success
     ************************************************************************/
    private synchronized static boolean writeArea(Area a){
        boolean outcome = true;  
        String path = "areas/" + a.ID.concat(".area").toLowerCase();
        File f = new File(path);
        try{
            try (PrintWriter pw = new PrintWriter(f, "UTF-8")) {
                pw.println("[title]" + a.title);
                pw.println("[description]" + a.description);
                pw.println("[items]" + a.items);
                pw.println("[exits]" + a.exits);
            }
        } catch(FileNotFoundException | UnsupportedEncodingException e){
            outcome = false;
            System.out.println("An error occurred writing " + path + ".");
        }
        return outcome;
    }    
    
/********************************************************************
     * Reads the player file specified in pName and returns an Player object
     * @param pName name of Player being requested
     * @return Player object
     ********************************************************************/    
    private synchronized static Player readPlayerFromFile(String pName){
        Socket sock = null;
        Player p = new Player(sock);
        String contents = null;
        String playerFile = "players/" + pName + ".player";
        File file = new File(playerFile);
        try{    
            if (file.exists()){
                contents = new String(Files.readAllBytes(Paths.get(playerFile)));
            } else {
                System.out.println("NULL PLAYER LOADED");
                p = null;
                contents = null;
            }
        }
        catch(IOException e){
            System.out.println("An error occurred reading " + playerFile + ".");
        }
        if (p != null && contents != null){
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
                        p.description = value.trim();
                        break;
                    case "name":
                        p.name = value.trim();
                        break;    
                    case "password":
                        p.password = value.trim();
                        break;
                    case "location":
                        p.location = Integer.parseInt(value.trim());
                        break;
                    case "inventory":
                        String[] items = value.trim().split("\\s*,\\s*");
                        p.inventory.addAll(Arrays.asList(items));
                        break;
                    default:
                }
            } while(indexNext >= 0);
        }
    return p;
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
 * @return Boolean - true if direction points to valid area ID, false if not.
 */
    public synchronized static boolean doWalk(Player p, String direction){
        boolean outcome = false;
        String[] dirList = {"north","south","east","west","up","down"};
        String[] dirAbbr = {"n","s","e","w","u","d"};
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

/**************************************************************************
 * sendMessageToArea locates the Player objects current room and loads the 
 * other players sockets for communication. It then utilizes the message
 * parsed from the command line entry.All data needed is loaded from the area 
 * object generated by p.location.
 * @param p Player object
 * @param  message message entered by player
 * @return Boolean - true if executed properly, false if not.
 */    
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
    
/**************************************************************************
 * sendMessageToWorld utilizes a log of current users on the Server and uses their stored socket
 * info to direct a message towards them.
 * @param p Player object
 * @param message message entered by player
 * @return Boolean - true if executed properly, false if not.
 */      
    public synchronized static boolean sendMessageToWorld(Player p, String message){ //new
        boolean outcome = true;
        PrintWriter out;
        for (Player x : Server.curUsers){
                try {
                    Socket psock = x.socket;
                    out = new PrintWriter(psock.getOutputStream(), true);
                    out.println(p.name + " ]--> " + message);
                    } catch (IOException e) {
                        outcome = false;
                    }
        }
        return outcome;
    }

/**************************************************************************
 * sendMessageToPlayer invokes the whisper function which isolates the name of a player P
 * and send them a message directly provided in the message
 * @param p Player object
 * @param P Receiving Player's name
 * @param message intended message
 * @return outcome is true for success
 */    
    public synchronized static boolean sendMessageToPlayer(Player p,String P, String message){ //new
        boolean outcome = true;
        Area a = getArea(p.location);
        PrintWriter out;
        if (a != null){
            for (Player areaPlayers : a.players.values()){
                try {
                    if (!areaPlayers.name.equalsIgnoreCase(p.name)&& areaPlayers.name.equals(P)){
                        Socket psock = areaPlayers.socket;
                        out = new PrintWriter(psock.getOutputStream(), true);
                        out.println(p.name + " ]SHHHHH... " + message);
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
 
/**************************************************************************
 * sendEmotetoPlayer is essentially a build of the sendMessageToPlayer function
 * but uses a switch to interpret the emote and provide the proper syntax on delivery
 * @param p Player object
 * @param P Receiving Player's name
 * @param emote intended emote message
 * @return outcome is true for success
 */    
    public synchronized static boolean sendEmotetoPlayer(Player p,String P, String emote){ //new
        String emoteOut ="";
        boolean outcome = true;
        Area a = getArea(p.location);
        PrintWriter out;
        switch(emote){
            case "wink":
                emoteOut += "*" + p.name + " winks at you ;)";
                break;
            case "smile":
                emoteOut += "*" + p.name + " smiles at you :)";
                break;
            case "frown":
                emoteOut += "*" + p.name + " frowns at you :(";
                break;
            default:
                
                break;
        }
        if (a != null){
            for (Player areaPlayers : a.players.values()){
                try {
                    if (!areaPlayers.name.equalsIgnoreCase(p.name)&& areaPlayers.name.equals(P)){
                        Socket psock = areaPlayers.socket;
                        out = new PrintWriter(psock.getOutputStream(), true);
                        out.println(emoteOut);
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

/**************************************************************************
 * getItem retrieves an item from the room updating the player and room inventories
 * @param item Item retrieving
 * @param p Player object for storage of item
 */    
    public synchronized static void getItem(String item,Player p){
        Area a = getArea(p.location);
        if(a.items.contains(item)){
           p.inventory.add(item);
           a.items.remove(item);
           writeArea(a);
        }
    } 
    
/**************************************************************************
 * dropItem releases an item from the player into the current room
 * updating the player and room inventories
 * @param item Item released
 * @param p Player object 
 */     
    public synchronized static void dropItem(String item,Player p){
        Area a = getArea(p.location);
        if(p.inventory.contains(item)){
            p.inventory.remove(item);
            a.items.add(item);
            writeArea(a);
        }
        
            
    } 
    
/**************************************************************************
 * loadPlayer loads a copy of the player for info analysis
 * @param name name of Player object to be rendered
 * @return Player object copy
 */   
    public synchronized static Player loadPlayer(String name){
        Player x = readPlayerFromFile(name);
        return x;
    }    

/**************************************************************************
 * listInventory isolates the users out stream to display to them alone the 
 * inventory at the moment
 * @param out PrintWriter stream for user
 * @param p Player object
 */    
    public synchronized static void listInventory(PrintWriter out,Player p){
        out.print("Current Inventory ----[ | ");
        if(!p.inventory.isEmpty()){
            p.inventory.forEach((x) -> {
                out.print(x + " | ");
            }); 
            out.println(" ]");            
        }else{
            out.println("| ]");
        }
    }    

/**************************************************************************
 * helpMe allows for display of command help prompts based upon error or inquiry
 * @param out PrintWriter of user
 * @param command current command being queried
 * @param err Boolean determines inquiry or error 
 */    
    public synchronized static void helpMe(PrintWriter out,String command,boolean err){
        String[] helpLine = {"",""};
        switch(command){
            case "look":
                helpLine[0] = "Inspect surroundings/others/items";
                helpLine[1] = "Command format is: look <name> || look <direction> || look";
                break;
            case "walk":
                helpLine[0] = "Walk/go about";
                helpLine[1] = "Command format is: walk/go <direction>";
                break;    
            case "say":
                helpLine[0] = "Speak to room";
                helpLine[1] = "Command format is: say <message>";
                break;
            case "whisper":
                helpLine[0] = "Speak to person";
                helpLine[1] = "Command format is: whisper <name> <message>";
                break;
            case "describe":
                helpLine[0] = "Describes yourself for the others";
                helpLine[1] = "Command format is: describe <message>";
                break;
            case "take":
            case "get":
                helpLine[0] = "Retrieve item from room";
                helpLine[1] = "Command format is: take/get <item>";
                break;    
            case "drop":
                helpLine[0] = "Drop item in room";
                helpLine[1] = "Command format is: drop <item>";
                break;    
            case "emote":
                helpLine[0] = "Sends emote to person in room";
                helpLine[1] = "Command format is: emote <person> <emote>";
                break;    
            case "stats":
                helpLine[0] = "See player stats";
                helpLine[1] = "Command format is: stats|/s || stats|/s <person> ";
                break;    
            case "help":
                helpLine[0] = "See command info";
                helpLine[1] = "Command format is: help <command> ";
                break;    
        }
        if(err)
            out.println(helpLine[1]);
        else{
            for(String x : helpLine){
                out.println(x);
            }    
        }
    }  
    
/**************************************************************************
 * checkDirection checks for valid direction of movement
 * @param p Player object
 * @param direction Direction of inspection
 * @return True if valid direction
 */        
    public synchronized static boolean checkDirection(Player p, String direction){
        boolean outcome = false;
        String[] dirList = {"north","south","wet","east","up","down"};
        String[] dirAbbr = {"n","s","w","e","u","d"};
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
                outcome = true;
            }
        }
        return outcome;        
    }

/**************************************************************************
 * doLook inpects the surrounding areas and displays the results to the inquiring user
 * @param out PrintWriter of inquirer
 * @param p Player object
 * @param direction directio of interest
 */    
    public synchronized static void doLook(PrintWriter out,Player p,String direction) {
        String[] dirList = {"north","south","west","east","up","down"};
        String[] dirAbbr = {"n","s","w","e","u","d"};
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
            String curPlayers = "Current Players  -> | ";
            String[] portals = {"north","south","west","east","up","down"};
            String curExits = "Available Exits -> | ";
            if (a != null){
                if (a.exits[areaIndex] > 0){
                    Area b = getArea(a.exits[areaIndex]);
                    
                    out.println(b.description);
                    for(String name : b.players.keySet()){
                        curPlayers += name + " | ";
                    }
                    for (int i = 0; i < NUMBER_DIRECTIONS; i++){
                        if (b.exits[i] != 0){
                            curExits += portals[i] + " | ";
                        }
                    }
                    out.println(curExits);
                    out.println(curPlayers);
                    }
                else if(a.exits[areaIndex] == 0)
                    out.println("Nothing to see.");
            }

        }

    }   
    
    
}