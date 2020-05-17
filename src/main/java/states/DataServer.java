package states;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Random;

public class DataServer {
    public static void main(String[] args) throws IOException {
        try( ServerSocket listener = new ServerSocket(9090)) { // intantiated listener in try for automatic resource management i.e. closing the listener
            Socket socket = listener.accept();
            System.out.println("Got new connection: " + socket.toString());
            try {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                Random rand = new Random();
                int count = 0;
                int tenSum = 0;
                Date d = new Date();
                for (int x = 1; x < 50; x++) { // generates 2 keys 1 and 2 along with incremental value
                    int key = (x % 2) + 1;
                    String s = key + "," + x;
                    System.out.println("s");
                    out.println(s);
                    Thread.sleep(50);
                }
            } finally {
                socket.close();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
