/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sunny_boy;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import me.kutrumbos.DdpClient;
/**
 *
 * @author marcus
 */
public class Sunny_boy implements Observer {
    
    static Sunny_boy sunnyboy = new Sunny_boy();
    
    private static void log(String arg){
        System.out.println(arg);
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws UnknownHostException, URISyntaxException {
        DdpClient client = null;
        System.out.println(args.length);
        boolean forceLoad = false;
        if(args.length < 1){
            System.out.println("Using ip: 127.0.0.1");
            client = new DdpClient("127.0.0.1", 80);
        }
        else if(args.length == 1){
            System.out.println("Using ip: "+args[0]);
            client = new DdpClient(args[0], 80);
        }
        else if(args.length == 3){
            if(args[1].equals("force")){
                forceLoad = true;
                client = new DdpClient(args[0], 80);
            }
        }
        else{
            System.out.println("Usage: java -jar "+"Sunny_boy.jar"+" X.X.X.X");
            System.out.println("Or: java -jar "+"Sunny_boy.jar"+" X.X.X.X force <filename>");
            System.out.println("Where X.X.X.X is replaced with the server IP adress. The last row forces"
                    + "sending the file <filename> to the server.");
            return;
        }
        client.addObserver(sunnyboy);
        
        if(client.getReadyState() == 3){
            client.connect();
        }else{
            client.reconnect();
        }
        
        
        Object[] methodArgs = new Object[3];
        ArrayList sumArgs = new ArrayList<Object[]>();
        
        if(forceLoad){
            try {
                //use buffering, reading one line at a time
                //FileReader always assumes default encoding is OK!
                BufferedReader input =  new BufferedReader(new FileReader(new File(args[2])));
                try {
                  String line = null; //not declared within while loop
                  /*
                  * readLine is a bit quirky :
                  * it returns the content of a line MINUS the newline.
                  * it returns null only for the END of the stream.
                  * it returns an empty String if two newlines appear in a row.
                  */
                  String roundTrip = "";

                  while (( line = input.readLine()) != null){
                    try {
                        byte[] utf8Bytes = line.getBytes("UTF-16");
                        ArrayList<Byte> parsed = new ArrayList<Byte>();
                        for(byte bb : utf8Bytes){                                         
                            if(bb>0) parsed.add(bb);                                            
                        }
                        Byte[] bar = parsed.toArray(new Byte[0]);
                        byte[] bytes = new byte[bar.length];
                        int i = 0;
                        for(Byte bb2 : bar){
                            bytes[i] = bb2.byteValue();
                            i++;
                        }
                        roundTrip = new String(bytes);


                    } 
                    catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    if(roundTrip.length() > 25 && !roundTrip.equals("") && !roundTrip.startsWith("dd") && !roundTrip.startsWith(";") && !roundTrip.startsWith("sep") && !roundTrip.startsWith("Version")){
                        String[] res = roundTrip.split(";");
    //                                  for(String ss : res){
    //                                      log(ss);
    //                                  }
                        DateFormat df = new SimpleDateFormat("dd.MM.yyy HH:mm:ss");
                        Date result =  df.parse(res[0]); 
                        methodArgs = new Object[3];
                        methodArgs[0] = result.getTime();
                        methodArgs[1] = res[1]; //kWh
                        methodArgs[2] = res[2]; //W   
                        sumArgs.add(methodArgs);
                        log(methodArgs[0]+" "+methodArgs[1]+" "+methodArgs[2]);                                        

                    }
                  }
                  Object[] param = new Object[1];
                  param[0] = sumArgs.toArray();
                  client.call("save_pv_records_forced", param);   
                }
                finally {
                  input.close();
                  return;
                }
              }
              catch (IOException ex){
                ex.printStackTrace();
              }
            return;
        }
        else{
        while(true){
        
            try{
                
                if(client.getReadyState() == 3){
                    client.connect();
                }else{
                    client.reconnect();
                }
                
                sumArgs = new ArrayList<Object[]>();
                FileSystem fileSystem = FileSystems.getDefault();
                WatchService watchService = fileSystem.newWatchService();
                Path directory = Paths.get(System.getProperty("user.dir"));

                WatchEvent.Kind<?>[] events = {
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.OVERFLOW
                };
                directory.register(watchService, events);


                    log("Current time: "+new Date().toString());
                    log("Waiting for file change");
                    WatchKey watchKey = watchService.take();                    
                    log("Path being watched: "+watchKey.watchable());
                    if(watchKey.isValid()){
                        for(WatchEvent<?> event : watchKey.pollEvents()){
                            //log("Kind: "+event.kind());
                            //log("Context: "+event.context());
                            String fileName = event.context().toString();
                            log(fileName);
                            if(event.kind().equals(StandardWatchEventKinds.ENTRY_CREATE) || event.kind().equals(StandardWatchEventKinds.ENTRY_MODIFY) && fileName.endsWith(".csv")){
                                Path fullPath = directory.resolve(fileName);
                                try {
                                    //use buffering, reading one line at a time
                                    //FileReader always assumes default encoding is OK!
                                    BufferedReader input =  new BufferedReader(new FileReader(fullPath.toFile()));
                                    try {
                                      String line = null; //not declared within while loop
                                      /*
                                      * readLine is a bit quirky :
                                      * it returns the content of a line MINUS the newline.
                                      * it returns null only for the END of the stream.
                                      * it returns an empty String if two newlines appear in a row.
                                      */
                                      String roundTrip = "";

                                      while (( line = input.readLine()) != null){
                                        try {
                                            byte[] utf8Bytes = line.getBytes("UTF-16");
                                            ArrayList<Byte> parsed = new ArrayList<Byte>();
                                            for(byte bb : utf8Bytes){                                         
                                                if(bb>0) parsed.add(bb);                                            
                                            }
                                            Byte[] bar = parsed.toArray(new Byte[0]);
                                            byte[] bytes = new byte[bar.length];
                                            int i = 0;
                                            for(Byte bb2 : bar){
                                                bytes[i] = bb2.byteValue();
                                                i++;
                                            }
                                            roundTrip = new String(bytes);


                                        } 
                                        catch (UnsupportedEncodingException e) {
                                            e.printStackTrace();
                                        }
                                        if(roundTrip.length() > 25 && !roundTrip.equals("") && !roundTrip.startsWith("dd") && !roundTrip.startsWith(";") && !roundTrip.startsWith("sep") && !roundTrip.startsWith("Version")){
                                            String[] res = roundTrip.split(";");
          //                                  for(String ss : res){
          //                                      log(ss);
          //                                  }
                                            DateFormat df = new SimpleDateFormat("dd.MM.yyy HH:mm:ss");
                                            Date result =  df.parse(res[0]); 
                                            methodArgs = new Object[3];
                                            methodArgs[0] = result.getTime();
                                            methodArgs[1] = res[1]; //kWh
                                            methodArgs[2] = res[2]; //W   
                                            sumArgs.add(methodArgs);
                                            log(methodArgs[0]+" "+methodArgs[1]+" "+methodArgs[2]);                                        

                                        }
                                      }
                                      Object[] param = new Object[1];
                                      param[0] = sumArgs.toArray();
                                      client.call("save_pv_records", param);   
                                    }
                                    finally {
                                      input.close();
                                    }
                                  }
                                  catch (IOException ex){
                                    ex.printStackTrace();
                                  }
                                catch (ParseException ex){
                                    ex.printStackTrace();
                                  }

                            }
                            //log("Count: "+event.count());
                        }
                    }


            }
            catch(IOException ioe){
                log("IO-exception");
                client.disconnect();
            }
            catch(InterruptedException ie){
                log("Interrupted-exception");
                client.disconnect();
            }    
        }
        
        
        //client.disconnect();
      }
    }

    @Override
    public void update(Observable o, Object arg) {
        //log("update!! Args: "+arg+" Observable: "+o);
    }
    
    
}


