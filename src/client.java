import java.net.*;
import java.io.*;
public class client {
    DataOutputStream dos;
    DataInputStream dis;
    Socket s;
    FileOutputStream fos;
    FileInputStream fis;
    File f;
    client(){
        try {
            s = new Socket("localhost",9000);
            dis = new DataInputStream(s.getInputStream());
            dos = new DataOutputStream(s.getOutputStream());
//            fis = new FileInputStream(f);
//            fos = new FileOutputStream(f);
            send_message();
            receive_message();
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    void send_message(){
        try {
            dos.writeBytes("connection from: "+s+"\r\n");
        
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    void receive_message(){
        try {
            System.out.println(dis.readLine());
        } catch (Exception e) {
        }
    }
    public static void main(String[] args) {
        client obj = new client();
    }
          
    
}