import java.net.*;
import java.io.*;
public class server {
    DataOutputStream dos;
    DataInputStream dis;
    ServerSocket sock;
    Socket s;
    FileOutputStream fos;
    FileInputStream fis;
    File f;
    
    server(){
        try {
            sock=new ServerSocket(9000);
            while(true){
                s=sock.accept();
                System.out.println("connection built at :"+s);
                client_handler obj = new client_handler(s);
                Thread t = new Thread(obj);
                t.start();
            }
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    public static void main(String[] args) {
        server obj = new server();
    }
    class client_handler implements Runnable{

        public client_handler(Socket s) throws Exception{
            
                dis = new DataInputStream(s.getInputStream());
                dos = new DataOutputStream(s.getOutputStream());
//                fis = new FileInputStream(f);
//                fos = new FileOutputStream(f);
                
        }

        public void run() {
            try {
                String str = dis.readLine();
                if(str.contains("connection from")){
                    dos.writeBytes("WELCOME\r\n");
                }
                else
                    System.out.println("Some problem in connection");
            } 
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

