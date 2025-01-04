package DSPinger;

import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static ExecutorService EService = Executors.newFixedThreadPool(12);
    private static Map<String,Socket> userPool = new HashMap<String,Socket>();
    public static void main(String[] args) throws Exception{
        try(ServerSocket serverSocket = new ServerSocket(8081)){
            while(true){
                Socket socket = serverSocket.accept();
                new Thread(new MyServerReader(socket)).start();

            }

        }




    }

    public static synchronized boolean addUserToPool(String username, Socket userSocket){
        boolean isExist = userPool.containsKey(username);
        if (!username.contains(" ")) {
            if (!isExist) {
                userPool.put(username, userSocket);
                return true;
            } else {
                return false;
            }
        } else {
            Server.EService.execute(new BackMessage(userSocket,"There are spaces in your username"));
            return false;
        }
    }

    public static synchronized Socket getUserFromPool(String username){
        return userPool.get(username);
    }

    public static synchronized  boolean isUserExist(String username){
        if (userPool.containsKey(username)){
            return true;
        }else {
            return false;
        }
    }

    public static synchronized void removeUserFromPool(String username){
        userPool.remove(username);
    }

    
}

class MyServerReader implements Runnable{
    String userName;
    boolean isAuthorizeStage = true;
    Socket socket;
    public MyServerReader(Socket socket){
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            System.out.println("Ready to read");
            Scanner in = new Scanner(socket.getInputStream());
            Server.EService.execute(new BackMessage(socket,"Enter your name WITHOUT SPACES"));
            while (in.hasNextLine()) {
                while (isAuthorizeStage) {
                    String authorizedata = in.nextLine();
                    if (Server.addUserToPool(authorizedata,socket)){
                        isAuthorizeStage = false;
                        userName = authorizedata;
                        Server.EService.execute(new BackMessage(socket,"You have successfully registered"));
                    }else {
                        System.out.println("Username error");
                        Server.EService.execute(new BackMessage(socket,"Username allready exist or incorrect, try again"));
                    }


                }
                String str = in.nextLine();
                System.out.println(str);
                if (str.equals("exit")) {
                    break;
                }
                if (str.contains("PING")) {
                    String[] splitedCommand = str.split(" ");
                    if (splitedCommand.length<3){
                        Pinger.pingUser(splitedCommand[1],socket);
                    }else {
                        Server.EService.execute(new BackMessage(socket,"Incorrect command"));
                    }
                }
            }

                System.out.println(userName + " refused connection");
                Server.removeUserFromPool(userName);
                in.close();


        } catch (Exception e){
            System.out.println(e);
        }
    }
}




class BackMessage implements Runnable{
    String message;
    Socket socket;
    public BackMessage(Socket socket, String message){
        this.socket = socket;
        this.message = message;
    }

    @Override
    public void run() {
        try {
            PrintWriter pw = new PrintWriter(socket.getOutputStream(),true);
            pw.println(message);
        }catch (Exception e){
            System.out.println(e);
        }
    }
}

class Pinger{

    public synchronized static int pingUser(String username, Socket socket){
        if (Server.isUserExist(username)){
            Socket uSocket = Server.getUserFromPool(username);
            Server.EService.execute(new BackMessage(uSocket,"sound"));
            return 1;
        } else {
            Server.EService.execute(new BackMessage(socket,"User does not exist"));
            return 0;
        }
    }
}