
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.table.AbstractTableModel;
import java.awt.Canvas;
import java.awt.Color;
import uk.co.caprica.vlcj.player.embedded.videosurface.CanvasVideoSurface;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import java.awt.Desktop;
import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;

public class clientGUI extends javax.swing.JFrame {

    Thread t;
    client obj;
    DataOutputStream dos;
    DataInputStream dis;
    int count;
    ArrayList<online_clients> oc = new ArrayList<>();
    TableModel tm;
    TableModel2 tm2;
    ArrayList<all_search_results> asr = new ArrayList<>();

    class all_search_results {

        String file_name;
        String file_size;
        String ip;
    }

    class mini_client implements Runnable {

        String client_ip;
        DataOutputStream dosmc;
        DataInputStream dismc;
        Socket smc;

        public mini_client(String ip) {
            try {
                progress.setVisible(false);
                progress.setBorderPainted(false);
                jLabel1.setVisible(false);
                client_ip = ip;
                smc = new Socket(client_ip, 9100);
                System.out.println("connection built...");
                dosmc = new DataOutputStream(smc.getOutputStream());
                dismc = new DataInputStream((smc.getInputStream()));
                new Thread(this).start();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                jLabel1.setVisible(true);
                progress.setVisible(true);
                progress.setBorderPainted(true);
                String s = dismc.readLine();
                System.out.println(s);
                if (s.equals("Sending response")) {
                    String size1 = dismc.readLine();
                    System.out.println(size1);
                    int size = Integer.parseInt(size1);
                    //System.out.println(size);
                    for (int i = 0; i < size; i++) {
                        all_search_results entry = new all_search_results();
                        entry.file_name = dismc.readLine();
                        entry.file_size = dismc.readLine();
                        entry.ip = dismc.readLine();
                        asr.add(entry);

                        tm2.fireTableDataChanged();
                    }
                } else if (s.equals("Sending file")) {
                    String sfn = dismc.readLine();
                    long sfs = Long.parseLong(dismc.readLine());
                    speed.setVisible(true);
                    FileOutputStream fos = new FileOutputStream("C:\\Users\\public\\shared\\" + sfn);
                    byte br[] = new byte[100000];
                    int r = 0, count = 0;
                    long start_time = System.nanoTime();
                    long current_time,end_time,time_taken;
                    while (true) {
                        try {
                            r = dismc.read(br, 0, 100000);
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(clientGUI.this, "HOST DISCONNECTED");
                            break;
                        }
                        System.out.println(r);
                        fos.write(br, 0, r);
                        progress.setValue((int) ((long) count * 100 / (long) (sfs)));
                        current_time = System.nanoTime();
                        progress.setStringPainted(true);
                        progress.setString((int) ((long) count * 100 / (long) (sfs)) + " %");
                        time_taken = (current_time-start_time)/1000000000;
                        float speed1;
                        if(time_taken!=0)
                        {
                        speed1 = count/time_taken;
                        String d_speed=speed1/1000+"";
                        d_speed.substring(0,5);
                        speed.setText("SPEED: "+d_speed+" Kbps");
                        }
                        count = count + r;

                        if (count == sfs) {
                            System.out.println(r);
                            fos.close();
                            progress.setValue(100);
                            progress.setStringPainted(true);
                            progress.setString("100 %");
                            speed.setVisible(false);
                            JOptionPane.showMessageDialog(clientGUI.this, "FILE RECEIVED");
                            break;
                        }
                    }
                    dosmc.writeBytes("FILE RECEIVED\r\n");
                    dosmc.flush();
                } else if (s.equals("streaming")) {
                    String port = dismc.readLine();
                    System.out.println("STREAMING PORT: " + port);
                    int connecting_port = Integer.parseInt(port);
                    String code = "rtsp://" + smc.getInetAddress().getHostAddress() + ":" + connecting_port + "/hello";
                    System.out.println(code);
                    StreamingFilePlayer obj1 = new StreamingFilePlayer(code);

                }

//                dosmc.writeBytes("CONNECTION REQUESTED\r\n");
//                if(dismc.readLine().equals("GRANTED")){
//                    dosmc.writeBytes("Connection requested from :"+smc.getLocalAddress()+"\r\n");
//                    String ack_msh= dismc.readLine();
//                    System.out.println(ack_msh);
//                }
//                else
//                    System.out.println("CONNECTION REFUSED");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    class mini_server implements Runnable {

        ServerSocket sk;
        Socket s;

        public mini_server() {

        }

        public void run() {
            try {
                sk = new ServerSocket(9100);
                while (true) {
                    s = sk.accept();
                    mini_client_handler mch = new mini_client_handler(s);
                    new Thread(mch).start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        class search_results {

            String f_name;
            String f_size;
        }

        class mini_client_handler implements Runnable {

            DataInputStream dismch;
            DataOutputStream dosmch;
            Socket current;
            ArrayList<search_results> sr;
            File f;

            public mini_client_handler(Socket s) {
                current = s;
                f = new File("C:\\Users\\Public\\shared");
                if (f.exists() == false) {
                    f.mkdir();
                }
                sr = new ArrayList<>();

                try {
                    dismch = new DataInputStream(s.getInputStream());
                    dosmch = new DataOutputStream(s.getOutputStream());

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void run() {
                try {
                    String request = dismch.readLine();
                    System.out.println(request);
//                    if(request.equals("CONNECTION REQUESTED")){

//                        dosmch.writeBytes("GRANTED\r\n");
//                        dosmch.writeBytes("Connection from mini_server_handler with ip :"+current.getLocalAddress()+
//                                " and port: "+current.getPort()+" started successfully\r\n");
//                        dosmch.flush();
//                    }
                    if (request.equals("SEARCH REQUESTED")) {
                        String search = dismch.readLine();

//                        System.out.println(search);
                        String files[] = f.list();
                        sr.clear();

                        for (int i = 0; i < files.length; i++) {
//                            System.out.println(files[i]);
                            if (files[i].contains(search)) {
                                String fn = files[i];
                                File fnew = new File("C:\\Users\\Public\\shared\\" + files[i]);
                                String fs = fnew.length() + "";
                                search_results obj = new search_results();
                                obj.f_name = fn;
                                obj.f_size = fs;
                                sr.add(obj);
                            }
                        }
                        dosmch.writeBytes("Sending response\r\n");
                        dosmch.writeBytes(sr.size() + "\r\n");
                        for (int j = 0; j < sr.size(); j++) {
                            System.out.println(sr.get(j).f_name);
                            dosmch.writeBytes(sr.get(j).f_name + "\r\n");
                            dosmch.writeBytes(sr.get(j).f_size + "\r\n");
                            dosmch.writeBytes(current.getLocalAddress().getHostAddress() + "\r\n");
                        }

                    } else if (request.equals("DOWNLOAD REQUESTED")) {
                        String fnd = dismch.readLine();
                        File fhere = new File("C:\\Users\\Public\\shared\\" + fnd);
                        long fsd = fhere.length();
                        dosmch.writeBytes("Sending file\r\n");
                        dosmch.writeBytes(fnd + "\r\n");
                        dosmch.writeBytes(fsd + "\r\n");
                        byte b[] = new byte[100000];
                        long count = 0;
                        FileInputStream fis = new FileInputStream(fhere);
                        if (fhere.exists()) {
                            System.out.println("file exists at mini server");
                        }
                        while (true) {
                            int r = fis.read(b, 0, 100000);
                            count = count + r;
                            dosmch.write(b, 0, r);
                            if (count == fsd) {
                                fis.close();
                                break;
                            }
                        }
                        String final_ack = dismch.readLine();
                        System.out.println(final_ack);
                    } else if (request.equals("CHAT REQUESTED")) {
                        String host = dismch.readLine();
                        String connector_ip = dismch.readLine();
                        String str = host + " :  " + dismch.readLine();
                        jTextArea1.append(str + "\n");

                    } else if (request.equals("STREAM REQUESTED")) {
                        String fileName = dismch.readLine();
                        String ip = dismch.readLine();
                        File fs = new File("C:\\Users\\Public\\shared\\" + fileName);
                        int s_port = (int) (Math.random() * 1000) + 20000;
                        if (fs.exists()) {
                            System.out.println(s_port);
                        }
                        StreamingServer obj = new StreamingServer(fs.toString(), s_port, ip);
                        dosmch.writeBytes("streaming\r\n");
                        dosmch.writeBytes(s_port + "\r\n");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    }

    class online_clients {

        String temp_client_IP;
        String temp_client_port;
    }

    public clientGUI() {
        tm = new TableModel();
        tm2 = new TableModel2();
        initComponents();
        progress.setVisible(false);
        progress.setBorderPainted(false);
        jLabel1.setVisible(false);
        chat_area.setEnabled(true);
        chat_area.setVisible(true);
        jTextArea1.setFocusable(false);
        jTextArea1.setEditable(false);
        jTextField3.requestFocus();
        setLocation(120,60);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        client_connect = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jTextField1 = new javax.swing.JTextField();
        jButton4 = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jButton5 = new javax.swing.JButton();
        progress = new javax.swing.JProgressBar();
        jLabel1 = new javax.swing.JLabel();
        chat_area = new javax.swing.JPanel();
        jTextField2 = new javax.swing.JTextField();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jLabel2 = new javax.swing.JLabel();
        jButton6 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jTextField3 = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        speed = new javax.swing.JLabel();
        jButton7 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        client_connect.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        client_connect.setText("CONNECT");
        client_connect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                client_connectActionPerformed(evt);
            }
        });

        jButton1.setText("GET LIST");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jTable1.setModel(tm);
        jScrollPane1.setViewportView(jTable1);

        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });

        jButton4.setText("SEARCH");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jTable2.setModel(tm2);
        jScrollPane2.setViewportView(jTable2);

        jButton5.setText("GET FILE");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("RECEIVING");

        jTextField2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField2ActionPerformed(evt);
            }
        });

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane3.setViewportView(jTextArea1);

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("LIVE CHAT");

        jButton6.setText("SEND");
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout chat_areaLayout = new javax.swing.GroupLayout(chat_area);
        chat_area.setLayout(chat_areaLayout);
        chat_areaLayout.setHorizontalGroup(
            chat_areaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(chat_areaLayout.createSequentialGroup()
                .addGroup(chat_areaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(chat_areaLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(chat_areaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, chat_areaLayout.createSequentialGroup()
                                .addComponent(jTextField2)
                                .addGap(18, 18, 18)
                                .addComponent(jButton6, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 474, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(chat_areaLayout.createSequentialGroup()
                        .addGap(184, 184, 184)
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        chat_areaLayout.setVerticalGroup(
            chat_areaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(chat_areaLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 226, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(chat_areaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton6, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jButton2.setText("STREAM");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setText("MY SHARED FOLDER");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jTextField3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField3ActionPerformed(evt);
            }
        });

        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 10)); // NOI18N
        jLabel3.setText("Enter SERVER IP:");

        jButton7.setText("ABOUT");
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane2))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(23, 23, 23)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, 247, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(client_connect, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(progress, javax.swing.GroupLayout.PREFERRED_SIZE, 177, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(speed, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 139, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(10, 10, 10))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(33, 33, 33)))))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(chat_area, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 507, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton7, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton7, javax.swing.GroupLayout.DEFAULT_SIZE, 28, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(client_connect, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(25, 25, 25)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(progress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(4, 4, 4)
                        .addComponent(speed, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, 36, Short.MAX_VALUE)
                            .addComponent(jButton5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(chat_area, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(16, 16, 16))))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void client_connectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_client_connectActionPerformed
        obj = new client();
        obj.server_ip=jTextField3.getText();
        t = new Thread(obj);
        t.start();
        mini_server ms = new mini_server();
        new Thread(ms).start();
        client_connect.setEnabled(false);
    }//GEN-LAST:event_client_connectActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        try {
            oc.clear();
            dos.writeBytes("GET LIST\r\n");
            tm.fireTableDataChanged();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(clientGUI.this,"NOT CONNECTED TO SERVER");
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        try {
            asr.clear();
            tm2.fireTableDataChanged();
            for (int i = 0; i < oc.size(); i++) {
                System.out.println("CLIENTS ONLINE: " + oc.size());
                mini_client obj1 = new mini_client(oc.get(i).temp_client_IP);
                System.out.println(oc.get(i).temp_client_IP);
                obj1.dosmc.writeBytes("SEARCH REQUESTED\r\n");
                obj1.dosmc.writeBytes(jTextField1.getText() + "\r\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        try {
            int n = jTable2.getSelectedRow();
            if (n == -1) {
                JOptionPane.showMessageDialog(this, "PLEASE SELECT A FILE");
            } else {
                mini_client mc = new mini_client(asr.get(n).ip);
                mc.dosmc.writeBytes("DOWNLOAD REQUESTED\r\n");
                mc.dosmc.writeBytes(asr.get(n).file_name + "\r\n");
            }
        } catch (Exception e) {
        }
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        try {
            int n = jTable1.getSelectedRow();
            if (n == -1) {
                JOptionPane.showMessageDialog(this, "Please Select a Client");
            } else {
                String client_ip = oc.get(n).temp_client_IP;
                mini_client mc = new mini_client(client_ip);
                mc.dosmc.writeBytes("CHAT REQUESTED\r\n");
                mc.dosmc.writeBytes(mc.smc.getLocalAddress().getHostName() + "\r\n");
                mc.dosmc.writeBytes(mc.smc.getLocalAddress().getHostAddress() + "\r\n");
                mc.dosmc.writeBytes(jTextField2.getText() + "\r\n");
                jTextArea1.append("Me: " + jTextField2.getText() + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(clientGUI.this,"Either:\n\n1)Not connected to Server OR\n2) HOST disconnected (Press GET LIST)");
        }
    }//GEN-LAST:event_jButton6ActionPerformed

    private void jTextField2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField2ActionPerformed
        try {
            int n = jTable1.getSelectedRow();
            if (n == -1) {
                JOptionPane.showMessageDialog(this, "Please Select a Client");
            } else {
                String client_ip = oc.get(n).temp_client_IP;
                mini_client mc = new mini_client(client_ip);
                mc.dosmc.writeBytes("CHAT REQUESTED\r\n");
                mc.dosmc.writeBytes(mc.smc.getLocalAddress().getHostName() + "\r\n");
                mc.dosmc.writeBytes(mc.smc.getLocalAddress().getHostAddress() + "\r\n");
                mc.dosmc.writeBytes(jTextField2.getText() + "\r\n");
                jTextArea1.append("Me: " + jTextField2.getText() + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        jTextField2.setText("");
    }//GEN-LAST:event_jTextField2ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        try {
            int d = jTable2.getSelectedRow();
            if (d == -1) {
                JOptionPane.showMessageDialog(this, "Please select a file");
            } else {
                mini_client mc1 = new mini_client(asr.get(d).ip);
                mc1.dosmc.writeBytes("STREAM REQUESTED\r\n");
                mc1.dosmc.writeBytes(asr.get(d).file_name + "\r\n");
                mc1.dosmc.writeBytes(mc1.smc.getInetAddress().getHostAddress() + "\r\n");

            }

            //Desktop.getDesktop().open(new File);
        } catch (Exception e) {
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        try {
            File myfile = new File("/users/public/shared/open");
            String path = myfile.getAbsolutePath();
            File dir = new File(path.substring(0, path.lastIndexOf(File.separator)));
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(dir);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(clientGUI.this,"FOLDER DOES NOT EXIST","folder not found",JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jTextField3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField3ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField3ActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        about obj = new about();
        obj.setVisible(true);
        
    }//GEN-LAST:event_jButton7ActionPerformed

    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(clientGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(clientGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(clientGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(clientGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new clientGUI().setVisible(true);
            }
        });
    }

    public class client implements Runnable {

        Socket s;
        FileOutputStream fos;
        FileInputStream fis;
        File f;
        String server_ip;
        client() {
        }

        public void run() {
            try {
                boolean connected_flag = false;
                try{
                
                s = new Socket(server_ip, 9001);
                dis = new DataInputStream(s.getInputStream());
                dos = new DataOutputStream(s.getOutputStream());
                connected_flag=true;
                }
                catch(Exception ex){
                    ex.printStackTrace();
                    connected_flag=false;
                    JOptionPane.showMessageDialog(clientGUI.this,"Either: \n1)INVALID IP\n2)SERVER NOT RUNNING\n3)INTERNET DISCONNECTED","ERROR",JOptionPane.OK_OPTION);
                    client_connect.setEnabled(true);
                }
                if(connected_flag==true){
                JOptionPane.showMessageDialog(clientGUI.this,"Connected to server! :). Press GET LIST to see ONLINE clients ");
                setTitle("THIS CLIENT's IP: "+s.getLocalAddress().getHostAddress());
                }
                while (true) {
                    String ack = dis.readLine();
                    System.out.println(s);
                    if (ack.equals("SENDING LIST")) {
                        count = Integer.parseInt(dis.readLine());
                        for (int n = 0; n < count; n++) {
                            online_clients temp = new online_clients();
                            temp.temp_client_IP = dis.readLine();
                            temp.temp_client_port = dis.readLine();
                            if (temp.temp_client_IP.equals(s.getLocalAddress().getHostAddress()) == false) {
                                oc.add(temp);
                                tm.fireTableDataChanged();
                            } else {
                                System.out.println("same ip");
                            }
                        }
                    }
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel chat_area;
    private javax.swing.JButton client_connect;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable2;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JProgressBar progress;
    private javax.swing.JLabel speed;
    // End of variables declaration//GEN-END:variables
    public class TableModel2 extends AbstractTableModel {

        String title[] = {"S.no", "File Name", "Size", "HOST IP"};

        public String getColumnName(int a) {
            return title[a];
        }

        public int getRowCount() {
            return asr.size();
        }

        public int getColumnCount() {
            return 4;
        }

        public Object getValueAt(int i, int j) {
            all_search_results temp = asr.get(i);
            if (j == 0) {
                return i + 1;
            }
            if (j == 1) {
                return temp.file_name;
            }
            if (j == 2) {
                return temp.file_size;
            }
            if (j == 3) {
                return temp.ip;
            }
            return 0;
        }

    }

    public class TableModel extends AbstractTableModel {

        String s[] = {"S.no", "IP", "Port"};

        public String getColumnName(int i) {
            return s[i];
        }

        @Override
        public int getRowCount() {
            return oc.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public Object getValueAt(int i, int j) {
            online_clients obj_temp = oc.get(i);
            if (j == 0) {
                return i + 1;
            }
            if (j == 1) {
                return obj_temp.temp_client_IP;
            }
            if (j == 2) {
                return obj_temp.temp_client_port;
            }
            return 0;
        }
    }
}

class StreamingServer implements Runnable {

    String ip1;
    int portno1;
    String id;
    EmbeddedMediaPlayer mediaPlayer1;
    String media1;
    Thread t;

    StreamingServer(String m, int p, String ip) {
        NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), "C:\\vlcj data");
        Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);
        id = "hello";
        ip1 = ip;
        portno1 = p;
        media1 = m;
        System.out.println(ip1 + "  " + portno1 + "  " + media1);
        t = new Thread(this);
        t.start();
    }

    public void run() {
        String options1 = formatRtspStream(ip1, portno1, id);

        MediaPlayerFactory mediaPlayerFactory1 = new MediaPlayerFactory();
        mediaPlayer1 = mediaPlayerFactory1.newEmbeddedMediaPlayer();
        mediaPlayer1.playMedia(media1, options1, ":no-sout-rtp-sap", ":no-sout-standard-sap", ":sout-all", ":sout-keep");
        System.out.println("RTPS Server started");
        try {
            Thread.currentThread().join();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    String formatRtspStream(String serverAddress, int serverPort, String id) {
        System.out.println("in format Rtsp stream");
        StringBuilder sb = new StringBuilder(60);
        sb.append(":sout=#rtp{sdp=rtsp://@");
        sb.append(serverAddress);
        sb.append(':');
        sb.append(serverPort);
        sb.append('/');
        sb.append(id);
        sb.append("}");
        return sb.toString();
    }

}

class StreamingFilePlayer extends JFrame {

    EmbeddedMediaPlayer mediaPlayer;
    Canvas canvas;
    String media;

    public StreamingFilePlayer(String media1) {
        setLayout(null);
        media = media1;
        System.out.println(media);

        /////////////////////////////// STEP-1/////////////////////////////////
        NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), "C:\\vlcj data\\");
        Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);

        /////////////////////////////// STEP-2/////////////////////////////////
        MediaPlayerFactory mediaPlayerFactory = new MediaPlayerFactory();

        /////////////////////////////// STEP-3/////////////////////////////////
        mediaPlayer = mediaPlayerFactory.newEmbeddedMediaPlayer();
        canvas = new Canvas();
        canvas.setBounds(10, 10, 500, 400);
        add(canvas);
        canvas.setBackground(Color.black);

        setSize(540, 450);
        setLocation(300, 200);
        setVisible(true);

        /////////////////////////////// STEP-4/////////////////////////////////
        CanvasVideoSurface videoSurface = mediaPlayerFactory.newVideoSurface(canvas);
        mediaPlayer.setVideoSurface(videoSurface);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        /////////////////////////////// STEP-5,6/////////////////////////////////
        mediaPlayer.playMedia(media1);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    }

    private void formWindowClosing(java.awt.event.WindowEvent evt) {
        try {
            remove(canvas);
            mediaPlayer.stop();

        } catch (Exception e) {
        }
    }

}
class about extends javax.swing.JFrame {

    /**
     * Creates new form about
     */
    public about() {
        initComponents();
        
        jTextArea1.setEditable(false);
        jTextArea1.setEnabled(true);
        setTitle("ABOUT");
        setResizable(false);
        setLocation(400,150);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jButton1 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jTextArea1.setText("APPLICATION: \tCLIENT\nTYPE: \tP2P CLIENT\nDeveloped by: \tGurkanwal Singh\nContact e-mail: kanwal.best@gmail.com\nQualification: \tUndergraduate(PEC, Chandigarh)\nDeveloped in: \tNetBeans IDE 8.0\n\nUnder supervision of VMM Education(JULY 2014)");
        jScrollPane1.setViewportView(jTextArea1);

        jButton1.setText("CLOSE");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jLabel1.setText("DETAILS: ");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 433, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>                        

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {                                         
        this.dispose();
    }                                        

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(about.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(about.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(about.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(about.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new about().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify                     
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextArea1;
    // End of variables declaration                   
}
