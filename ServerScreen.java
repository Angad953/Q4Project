import java.awt.*;
import java.net.*;
import javax.swing.*;

public class ServerScreen extends JPanel {
    private int numUsers;
    private String displayString;
    private String ip;
    private Manager manager;
    
    public ServerScreen() {
        this.setLayout(null);
        this.setFocusable(true);
        this.setPreferredSize(new Dimension(800, 600));
        manager = new Manager();
        
        displayString = "";
        numUsers = 0;
        
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            System.out.println("Could not find IP address for this host");
        }
    }
    
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // Draw background
        g.setColor(new Color(240, 240, 240));
        g.fillRect(0, 0, getWidth(), getHeight());
        
        // Draw title and server info
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("HUNGRY HUNGRY HIPPOS SERVER", 200, 50);
        
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        g.drawString("Server IP: " + ip, 50, 100);
        g.drawString("Port: 1024", 50, 120);
        g.drawString("Connected Users: " + numUsers, 50, 140);
        
        // Draw game statistics
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("Game Statistics", 50, 180);
        
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        g.drawString("Total Score: " + manager.totalScore, 50, 210);
        g.drawString("Animals Deflected: " + manager.numAnimals, 50, 230);
        g.drawString("Game Status: " + (manager.isGameStarted() ? "RUNNING" : "WAITING"), 50, 250);
        if (manager.isGameStarted()) {
            g.drawString("Time Remaining: " + manager.getGameTime() + " seconds", 50, 270);
        }
        
        // Draw connected players list
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("Connected Players:", 50, 320);
        
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        int yPos = 350;
        for (ServerThread thread : manager.getServerThreads()) {
            String playerInfo = thread.getPlayerId();
            if (playerInfo != null) {
                if (thread.isReady()) {
                    playerInfo += " (Ready)";
                } else {
                    playerInfo += " (Not Ready)";
                }
                g.drawString(playerInfo, 50, yPos);
                yPos += 20;
            }
        }
        
        // Draw player scores
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("Player Scores:", 400, 320);
        
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        yPos = 350;
        MyHashMap<String, Integer> scores = manager.getPlayerScores();
        for (String player : scores.keySet()) {
            g.drawString(player + ": " + scores.get(player), 400, yPos);
            yPos += 20;
        }
    }
    
    public Dimension getPreferredSize() {
        return new Dimension(800, 600);
    }
    
    public void listen() {
        try {
            int portNumber = 1024;
            ServerSocket server = new ServerSocket(portNumber);
            System.out.println("Server started on port " + portNumber);
            
            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(100); 
                        numUsers = manager.getServerThreads().size();
                        repaint();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            
            while (true) {
                Socket socket = server.accept();
                
                ServerThread newServer = new ServerThread(socket, manager);
                Thread thread = new Thread(newServer);
                thread.start();
                manager.addServerThread(newServer);
                numUsers = manager.getServerThreads().size();
                repaint();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}