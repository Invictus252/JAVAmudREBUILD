package mclamud;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class PlayerThread implements Runnable {
    
    private final Socket psock;
    private final Player player;
    
    public PlayerThread(Socket sock){
        this.psock= sock;
        this.player = new Player(this.psock);
    }

    @Override
    public void run() {  
        Scanner in;
        PrintWriter out;
        String playerIn;
        boolean exit = false;
        try {
            in = new Scanner(psock.getInputStream(),"ISO-8859-1");
            out = new PrintWriter(psock.getOutputStream(), true);
            out.println("\u001B[31m"+"Entering Sh'aide");
            out.println("\u001B[33m"+"Pop: declining"+"\u001B[0m");
            if (!playerLogin(in, out)){ // Failed login
                exit = true;
                exitPlayer();
            } else { //Successful login.
                Server.curUsers.add(player);
                World.movePlayer(player, player.location);
                World.sendMessageToArea(player, player.name + " has arrived.");
                World.displayArea(player.location, player);
            }
            while(!exit){ //Main loop
                playerIn = in.nextLine();
                exit = commandDispatcher(playerIn, out);
            }
            //Player has left world
            World.sendMessageToArea(player, player.name + " has returned to reality.");
            exitPlayer();
        } catch (IOException | NoSuchElementException e) {
            //Connection has been lost
            World.sendMessageToArea(player, player.name + " crumbles into dust.");
            exitPlayer();
        }
    }
    
    private void exitPlayer(){
        try {
            String ip = psock.getInetAddress().toString();
            System.out.println(ip + " disconnected.");
            psock.close();
        } catch (IOException ex){ 
            System.out.println("An IOException occurred when a player exited.");
        }
        finally{
            World.removePlayer(player);
        }
    }
    
    private boolean commandDispatcher(String command, PrintWriter out){
        boolean exitFlag = false;
        command = command.trim();
        String[] tokens= {};
        if (command.length() > 0){
            if(command.length() > 2)
                tokens = command.split(" ", 3);
            else if(command.length() <= 2)
                tokens = command.split(" ",2);
            else
                tokens[0] = command;
            switch(tokens[0].toLowerCase()){
                case "walk":
                case "go":
                case "w":
                case "g":
                    if (tokens.length > 1){
                        if (World.doWalk(player, tokens[1])){
                            World.displayArea(player.location, player);
                        } else {
                            out.println("You can't go in that direction.");
                        }
                    } else {
                        out.println("Command format is: walk/go <direction>");
                    }
                    break;
                case "describe":
                    if (tokens.length > 1){
                        player.description = "";
                        for(int i = 1; i < tokens.length; i++){
                            player.description += tokens[i] + " ";
                        }
                        World.writePlayer(player);
                    }
                    else
                        World.helpMe(out,tokens[0],true);
                    break;  
                case "look":
                case "/l":
                    switch (tokens.length) {
                        case 1:
                            World.displayArea(player.location, player);
                            break;
                        case 2:
                            switch (tokens[1]){
                                case "north":
                                case "n":
                                case "south":
                                case "s":
                                case "west":
                                case "w":
                                case "east":
                                case "e":
                                case "up":
                                case "u":
                                case "down":
                                case "d":
                                    World.doLook(out, player,tokens[1]);
                                    break ;
                                default:
                                    Area a = World.areaMap.get(player.location);
                                    for (Player x : a.players.values()){
                                        if (x.name.equalsIgnoreCase(tokens[1])){
                                            out.println(x.description);
                                        }
                                        else
                                            World.helpMe(out,tokens[0],true);
                                    }
                                    break;
                            }
                        default:
                            World.helpMe(out,tokens[0],true);
                            break;
                    }
                    break;
                case "inventory":
                case "/i":
                    World.listInventory(out, player);
                    break;
                case "say":
                    //System.out.println(tokens.length);
                    switch (tokens.length) {
                         case 2:
                            World.sendMessageToArea(player,tokens[1]);
                            break;
                        case 3:
                            World.sendMessageToArea(player,tokens[1] + " " + tokens[2]);
                            break;
                        default:
                            break;
                    }
                case "whisper":
                case "/w":
                    if(tokens.length > 2)
                        World.sendMessageToPlayer(player,tokens[1],tokens[2]);
                    break;  
                case "emote":
                case "/e":
                    if(tokens.length > 2)
                        World.sendEmotetoPlayer(player,tokens[1],tokens[2]);
                    break; 
                case "get":
                case "take":
                    if(tokens.length > 1 )
                        World.getItem(tokens[1], player);
                    World.writePlayer(player);
                    World.displayArea(player.location, player);
                    break;    
                case "drop":
                    if(tokens.length > 1 && player.inventory.contains(tokens[1]))
                        World.dropItem(tokens[1], player);
                    World.writePlayer(player);
                    World.displayArea(player.location, player);
                    break; 
                case "save":
                    World.writePlayer(player);
                    out.println("Player Saved...");
                    World.displayArea(player.location, player);
                    break; 
                case "exit":
                case "quit":
                    World.writePlayer(player);
                    exitFlag = true;
                    break;
                default:
                    out.println("Command \"" + command + "\" is not valid.");
            }
        }
        return exitFlag;
    }
    
    private boolean playerLogin(Scanner in, PrintWriter out){
        boolean outcome = false;
        boolean exit = false;
        boolean validUsername;
        boolean validPassword;
        String playerName = null;
        String playerPassword = null;
        TelnetFilter filter = new TelnetFilter();
        while(!exit){
            validUsername = false;
            validPassword = false;
            while(!validUsername){
                out.print("Enter your existing or desired player name: ");
                out.flush();
                playerName = filter.stripTelnet(in.nextLine());
                if (World.isValidPlayername(playerName)){
                    validUsername = true;
                } else {
                    out.println("The player name " + playerName + " was not valid.");
                    out.println("Your player name must be at least three characters " +
                        "and only use letters, numbers, and the underscore.");
                    if (!doAgain(in, out)){
                        outcome = false;
                        exit = true;
                        break;
                    }
                }
            }
            if (validUsername){
                if (World.doesPlayerExist(playerName)){
                    while(!validPassword){
                        out.print("Enter your password: ");
                        out.flush();
                        playerPassword = in.nextLine();
                        boolean goodPassword = true;  //Check password
                        if (goodPassword){
                            Player x = World.loadPlayer(playerName);
                            player.name = playerName;
                            player.inventory.addAll(x.inventory);
                            validPassword = true;
                            outcome = true;
                            exit = true;
                        } else {
                            out.println("The password was not correct.");
                            if (!doAgain(in, out)){
                                outcome = false;
                                exit = true;
                                break;
                            }
                        }
                    }
                } else {
                    out.println("The player name " + playerName + " does not exist.");
                    if (doAgain(in, out, "Would you like to use the name " + playerName + "?")){
                        while(!validPassword){
                            String passwordRequirements = "A valid password is at least eight characters long, " +
                                "contains at least one UPPERCASE letter, one lowercase letter, and one number.";
                            out.println(passwordRequirements);
                            out.print("Enter a password: ");
                            out.flush();
                            playerPassword = in.nextLine();
                            if (World.isValidPassword(playerPassword)){
                                Player x = World.loadPlayer(playerName);
                                player.name = playerName;
                                player.password = playerPassword;
                                player.inventory.addAll(x.inventory);
                                World.writePlayer(player);
                                validPassword = true;
                                outcome = true;
                                exit = true;
                            } else {
                                out.println("That password did not meet requirements.");
                                if (!doAgain(in, out)){
                                    if (doAgain(in, out, "Would you like to enter a new player name?")){
                                        exit = false;
                                    } else {
                                        outcome = false;
                                        exit = true;
                                    }
                                    break;
                                }
                            }
                        }
                    } else {
                        if (doAgain(in, out, "Would you like to enter a new player name?")){
                            exit = false;
                        } else {
                            outcome = false;
                            exit = true;
                        }
                    }
                }
            }
        }
        return outcome;
    }
     
    private boolean doAgain(Scanner in, PrintWriter out){
        String defaultPrompt = "Do you want to try again?";
        return doAgain(in, out, defaultPrompt);
    }
    
    private boolean doAgain(Scanner in, PrintWriter out, String prompt){
        String choice;
        boolean valid = false;
        boolean outcome = true;
        while(!valid){
            out.print(prompt + " Yes or no? ");
            out.flush();
            choice = in.nextLine().toLowerCase();
            if (choice.equals("yes") || choice.equals("y")){
                outcome = true;
                valid = true;
            } else if (choice.equals("no") || choice.equals("n")){
                outcome = false;
                valid = true;
            }
        }
        return outcome;
    }
}