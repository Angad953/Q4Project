import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerThread implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Manager manager;
    private String playerId;
    private boolean isReady;
    private boolean isAlive;
    private int playerX, playerY;
    private String playerColor;
    
    public ServerThread(Socket socket, Manager manager) {
        this.socket = socket;
        this.manager = manager;
        this.isReady = false;
        this.isAlive = true;
        this.playerId = null;
        this.playerX = 0;
        this.playerY = 0;
        this.playerColor = "255,255,255"; 
        
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public ServerThread(Socket socket) {
        this(socket, null);
    }
    
    @Override
    public void run() {
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null && isAlive) {
                processMessage(inputLine);
            }
        } catch (IOException e) {
            // Client disconnected
            if (manager != null) {
                manager.removeServerThread(this);
                if (playerId != null) {
                    manager.broadcastPlayerRemoval(playerId);
                }
            }
        }
    }
    
    private void processMessage(String message) {
        if (manager == null) return;
        
        String[] parts = message.split(":");
        
        if (parts[0].equals("CONNECT")) {
            playerId = parts[1];
            playerX = Integer.parseInt(parts[2]);
            playerY = Integer.parseInt(parts[3]);
            playerColor = parts[4];
            
            manager.addPlayer(playerId);
            System.out.println("Player connected: " + playerId);
            
            sendMessage(manager.getCurrentGameState());
            
            manager.broadcastPlayerPosition(playerId, playerX, playerY, playerColor);
        } else if (parts[0].equals("READY")) {
            playerId = parts[1];
            isReady = true;
            manager.playerReady();
        } else if (parts[0].equals("POS")) {
            playerId = parts[1];
            playerX = Integer.parseInt(parts[2]);
            playerY = Integer.parseInt(parts[3]);
            
            manager.broadcastPlayerPosition(playerId, playerX, playerY, playerColor);
            manager.checkFoodCollision(playerId, playerX, playerY);
        } else if (parts[0].equals("SCORE")) {
            String player = parts[1];
            int score = Integer.parseInt(parts[2]);
            manager.updateScore(player, score);
        } else if (parts[0].equals("ATTACK")) {
            String player = parts[1];
            String enemyId = parts[2];
            manager.removeEnemy(enemyId);
            manager.incrementAnimals();
        } else if (parts[0].equals("COLLISION")) {
            String player = parts[1];
            String enemyId = parts[2];
            manager.handlePlayerEnemyCollision(player, enemyId);
        } else if (parts[0].equals("ALIVE")) {
        } else if (parts[0].equals("GAMEOVER")) {
            manager.gameOver();
        } else if (parts[0].equals("RESET")) {
            isReady = false;
            manager.resetRequest();
        }
    }
    
    public void sendMessage(String message) {
        out.println(message);
    }
    
    public boolean isReady() {
        return isReady;
    }
    
    public void setReady(boolean ready) {
        isReady = ready;
    }
    
    public String getPlayerId() {
        return playerId;
    }
    
    public int getPlayerX() {
        return playerX;
    }
    
    public int getPlayerY() {
        return playerY;
    }
    
    public String getPlayerColor() {
        return playerColor;
    }
    
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }
    
    public void setPlayerPosition(int x, int y) {
        this.playerX = x;
        this.playerY = y;
    }
    
    public void setPlayerColor(String color) {
        this.playerColor = color;
    }
}