package mclamud;

public class TelnetFilter {
    public String stripTelnet(String stream){
        int flag = 0;
        String stripped = "";
        final int IAC = 255;
        for(int i = 0; i < stream.length(); i++){
            int c = (int)stream.charAt(i);     
            switch (flag) {
                case 0: //Interpret as command
                    if (c == IAC){
                        flag = 1;
                    } else {
                        stripped = stripped.concat(Character.toString((char) c));
                    }
                    break;
                case 1: //WILL WONT DO DONT
                    flag = 2;
                    break;
                case 2: //Option indicator
                    //TODO negotiate options here
                    flag = 0;
                    break;
            }
        }        
      return stripped;  
    }
    
}

