import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JPanel;

public class ClientScreen extends JPanel implements KeyListener, Runnable {
    private int score;
    private int time;
    private int numAnimals;
    private boolean gameStarted;
    private boolean gameOver;
    private String gameStatus;
    private Animal playerHippo;
    private ArrayList<Food> foods;
    private ArrayList<Animal> enemies;
    private HashMap<String, Integer> playerScores;
    private HashMap<String, Animal> otherPlayers; // Store other player hippos
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isReady;
    private Thread gameThread;
    private int energyLevel; // Energy for fighting off enemies
    private String playerId; // Unique ID for this player
    
    public ClientScreen() {
        this.setPreferredSize(new Dimension(800, 600));
        this.setBackground(Color.WHITE);
        this.setFocusable(true);
        this.addKeyListener(this);
        
        score = 0;
        time = 120; // 2-minute game
        numAnimals = 0;
        gameStarted = false;
        gameOver = false;
        gameStatus = "PRESS SPACE TO READY UP";
        isReady = false;
        energyLevel = 100;
        
        // Generate unique player ID
        playerId = "Player" + System.currentTimeMillis() % 10000;
        
        // Initialize game objects
        foods = new ArrayList<>();
        enemies = new ArrayList<>();
        playerScores = new HashMap<>();
        otherPlayers = new HashMap<>();
        
        // Create player hippo with random position
        int x = 100 + (int)(Math.random() * 600);
        int y = 100 + (int)(Math.random() * 400);
        Color[] hippoColors = {Color.YELLOW, Color.PINK};
        int colorIndex = (int)(Math.random() * hippoColors.length);
        playerHippo = new Animal(x, y, hippoColors[colorIndex], playerId);
        
        // Preload some food
        for (int i = 0; i < 20; i++) {
            foods.add(new Food());
        }
    }
    
    public void connect() {
        try {
            socket = new Socket("localhost", 1024);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Start the receiving thread
            gameThread = new Thread(this);
            gameThread.start();
            
            // Send initial connection message with position and color
            out.println("CONNECT:" + playerId + ":" + playerHippo.getX() + ":" + 
                      playerHippo.getY() + ":" + playerHippo.getColorAsString());
        } catch (IOException e) {
            e.printStackTrace();
            gameStatus = "CONNECTION FAILED";
        }
    }
    
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (!gameStarted) {
            // Draw start screen
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.drawString("HUNGRY HUNGRY HIPPOS", 200, 200);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.drawString(gameStatus, 250, 250);
            g.drawString("Press SPACE to ready up", 280, 300);
            if (isReady) {
                g.setColor(Color.GREEN);
                g.drawString("YOU ARE READY!", 310, 350);
            }
        } else if (gameOver) {
            // Draw game over screen
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.drawString("GAME OVER", 300, 200);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.drawString("Final Score: " + score, 320, 250);
            g.drawString("Press R to play again", 300, 300);
        } else {
            // Draw game background
            g.setColor(new Color(200, 240, 200)); // Light green for grass/field
            g.fillRect(0, 0, getWidth(), getHeight());
            
            // Draw water areas (pond)
            g.setColor(new Color(100, 150, 255));
            g.fillOval(getWidth()/2 - 150, getHeight()/2 - 100, 300, 200);
            
            // Draw food
            for (Food food : foods) {
                food.draw(g);
            }
            
            // Draw enemies
            for (Animal enemy : enemies) {
                enemy.draw(g);
            }
            
            // Draw other players
            for (Animal otherPlayer : otherPlayers.values()) {
                otherPlayer.draw(g);
                
                // Draw player ID above hippo
                g.setColor(Color.BLACK);
                g.setFont(new Font("Arial", Font.BOLD, 12));
                g.drawString(otherPlayer.getId(), 
                           otherPlayer.getX() + 20, otherPlayer.getY() - 5);
            }
            
            // Draw player hippo
            playerHippo.draw(g);
            
            // Draw player ID above hippo
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            g.drawString(playerId, playerHippo.getX() + 20, playerHippo.getY() - 5);
            
            // Draw HUD
            drawHUD(g);
        }
    }
    
    private void drawHUD(Graphics g) {
        // Draw semi-transparent background for HUD
        g.setColor(new Color(0, 0, 0, 128));
        g.fillRect(0, 0, getWidth(), 100);
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Score: " + score, 20, 20);
        g.drawString("Time: " + time, 20, 40);
        g.drawString("Animals Deflected: " + numAnimals, 20, 60);
        
        // Draw energy bar
        g.drawString("Energy: ", 20, 80);
        g.setColor(new Color(255, 50, 50));
        g.fillRect(90, 65, energyLevel, 15);
        g.setColor(Color.WHITE);
        g.drawRect(90, 65, 100, 15);
        
        // Draw other player scores
        int yPos = 20;
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        for (String player : playerScores.keySet()) {
            if (!player.equals(playerId)) { // Don't show our own score twice
                g.drawString(player + ": " + playerScores.get(player), 650, yPos);
                yPos += 20;
            }
        }
        
        // Draw team score
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("Team Score: " + getTotalScore(), getWidth()/2 - 70, 30);
    }
    
    private int getTotalScore() {
        int total = 0;
        for (Integer s : playerScores.values()) {
            total += s;
        }
        return total;
    }

    private void updateGame() {
        // Move enemies
        for (int i = 0; i < enemies.size(); i++) {
            Animal enemy = enemies.get(i);
            enemy.move();
            
            // Check collision with food
            for (Food food : foods) {
                if (!food.isEaten() && enemy.getBounds().intersects(food.getBounds())) {
                    food.setEaten(true);
                    // Enemy stole food!
                }
            }
            
            if (enemy.getBounds().intersects(playerHippo.getBounds())) {
                out.println("COLLISION:" + playerId + ":" + enemy.getId());
                enemies.remove(i);
                i--; 
            }
        }
        
        // Check player collision with food
        for (Food food : foods) {
            if (!food.isEaten() && playerHippo.getBounds().intersects(food.getBounds())) {
                food.setEaten(true);
                score++;
                out.println("SCORE:" + playerId + ":" + score);
            }
        }
        
        // Send position update to server
        out.println("POS:" + playerId + ":" + playerHippo.getX() + ":" + playerHippo.getY());
        
        // Update visual elements
        repaint();
    }
    
    @Override
    public void run() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                processMessage(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Handle disconnection
            gameStarted = false;
            gameOver = true;
            gameStatus = "DISCONNECTED FROM SERVER";
            repaint();
        }
    }
    
    private void processMessage(String message) {
        String[] parts = message.split(":");
        
        if (parts[0].equals("START")) {
            gameStarted = true;
            // Start game timer
            new Thread(() -> {
                while (time > 0 && gameStarted && !gameOver) {
                    try {
                        Thread.sleep(1000);
                        time--;
                        
                        // Send heartbeat
                        out.println("ALIVE:" + playerId);
                        
                        if (time <= 0) {
                            gameOver = true;
                            out.println("GAMEOVER:" + playerId);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            
            // Start game update loop
            new Thread(() -> {
                while (gameStarted && !gameOver) {
                    try {
                        Thread.sleep(50); // 20 FPS
                        updateGame();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            
        } else if (parts[0].equals("FOOD")) {
            // New format: FOOD:x:y:eaten
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            boolean eaten = Boolean.parseBoolean(parts[3]);
            
            // Add food if new, or update if existing
            boolean found = false;
            for (Food food : foods) {
                if (food.getX() == x && food.getY() == y) {
                    food.setEaten(eaten);
                    found = true;
                    break;
                }
            }
            
            if (!found && !eaten) {
                foods.add(new Food(x, y));
            }
            
        } else if (parts[0].equals("PLAYER")) {
            // Format: PLAYER:id:x:y:colorAsString
            String id = parts[1];
            int x = Integer.parseInt(parts[2]);
            int y = Integer.parseInt(parts[3]);
            Color color = Animal.stringToColor(parts[4]);
            
            if (!id.equals(playerId)) { // If not our own player
                Animal otherPlayer = otherPlayers.get(id);
                if (otherPlayer == null) {
                    // New player
                    otherPlayer = new Animal(x, y, color, id);
                    otherPlayers.put(id, otherPlayer);
                } else {
                    // Update existing player
                    otherPlayer.setPosition(x, y);
                }
            }
            
        } else if (parts[0].equals("REMOVE_PLAYER")) {
            // Player disconnected
            String id = parts[1];
            otherPlayers.remove(id);
            
        } else if (parts[0].equals("ENEMY")) {
            // Format: ENEMY:id:x:y:size
            String id = parts[1];
            int x = Integer.parseInt(parts[2]);
            int y = Integer.parseInt(parts[3]);
            int size = 50; // Default size
            
            if (parts.length > 4) {
                size = Integer.parseInt(parts[4]);
            }
            
            // Add enemy if new
            boolean found = false;
            for (Animal enemy : enemies) {
                if (enemy.getId().equals(id)) {
                    enemy.setPosition(x, y);
                    enemy.setSize(size);
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                Animal enemy = new Animal("Enemy");
                enemy.setPosition(x, y);
                enemy.setSize(size);
                enemies.add(enemy);
            }
            
        } else if (parts[0].equals("REMOVE_ENEMY")) {
            // Enemy was defeated
            String id = parts[1];
            for (int i = 0; i < enemies.size(); i++) {
                if (enemies.get(i).getId().equals(id)) {
                    enemies.remove(i);
                    break;
                }
            }
            
        } else if (parts[0].equals("SCORE")) {
            String player = parts[1];
            int playerScore = Integer.parseInt(parts[2]);
            playerScores.put(player, playerScore);
            
        } else if (parts[0].equals("ANIMALCOUNT")) {
            numAnimals = Integer.parseInt(parts[1]);
            
        } else if (parts[0].equals("READY")) {
            int readyCount = Integer.parseInt(parts[1]);
            int totalCount = Integer.parseInt(parts[2]);
            gameStatus = readyCount + " OF " + totalCount + " PLAYERS READY";
            
        } else if (parts[0].equals("GAMEOVER")) {
            gameOver = true;
            
        } else if (parts[0].equals("RESET")) {
            resetGame();
            
        } else if (parts[0].equals("SYNC")) {
            // Full game state sync from server
            syncGameState(parts);
        }
        
    }
    
    private void syncGameState(String[] parts) {
        int index = 1;
        
        // Clear existing data
        otherPlayers.clear();
        foods.clear();
        enemies.clear();
        
        try {
            // Read player count
            int playerCount = Integer.parseInt(parts[index++]);
            
            // Read each player
            for (int i = 0; i < playerCount; i++) {
                String id = parts[index++];
                int x = Integer.parseInt(parts[index++]);
                int y = Integer.parseInt(parts[index++]);
                Color color = Animal.stringToColor(parts[index++]);
                
                if (!id.equals(playerId)) {
                    Animal player = new Animal(x, y, color, id);
                    otherPlayers.put(id, player);
                }
            }
            
            // Read food count
            int foodCount = Integer.parseInt(parts[index++]);
            
            // Read each food
            for (int i = 0; i < foodCount; i++) {
                int x = Integer.parseInt(parts[index++]);
                int y = Integer.parseInt(parts[index++]);
                boolean eaten = Boolean.parseBoolean(parts[index++]);
                
                Food food = new Food(x, y);
                food.setEaten(eaten);
                foods.add(food);
            }
            
            // Read enemy count
            int enemyCount = Integer.parseInt(parts[index++]);
            
            // Read each enemy
            for (int i = 0; i < enemyCount; i++) {
                String id = parts[index++];
                int x = Integer.parseInt(parts[index++]);
                int y = Integer.parseInt(parts[index++]);
                int size = Integer.parseInt(parts[index++]);
                
                Animal enemy = new Animal("Enemy");
                enemy.setPosition(x, y);
                enemy.setSize(size);
                enemy.setId(id); // This is a new method we need to add to Animal class
                enemies.add(enemy);
            }
            
            // Read scores
            int scoresCount = Integer.parseInt(parts[index++]);
            playerScores.clear();
            for (int i = 0; i < scoresCount; i++) {
                String player = parts[index++];
                int score = Integer.parseInt(parts[index++]);
                playerScores.put(player, score);
                
                // If this is our score, update it
                if (player.equals(playerId)) {
                    this.score = score;
                }
            }
            
            // Read game state
            gameStarted = Boolean.parseBoolean(parts[index++]);
            time = Integer.parseInt(parts[index++]);
            
        } catch (Exception e) {
            System.out.println("Error syncing game state: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void resetGame() {
        score = 0;
        time = 120;
        numAnimals = 0;
        gameStarted = false;
        gameOver = false;
        gameStatus = "PRESS SPACE TO READY UP";
        isReady = false;
        energyLevel = 100;
        
        foods.clear();
        enemies.clear();
        otherPlayers.clear();
        playerScores.clear();
        
        // Randomize player position
        int x = 100 + (int)(Math.random() * 600);
        int y = 100 + (int)(Math.random() * 400);
        playerHippo.setPosition(x, y);
        
        // Preload some food
        for (int i = 0; i < 20; i++) {
            foods.add(new Food());
        }
        
        repaint();
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        if (gameStarted && !gameOver) {
            // Game controls
            int oldX = playerHippo.getX();
            int oldY = playerHippo.getY();
            
            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP:
                    playerHippo.movePlayer(0, -1);
                    break;
                case KeyEvent.VK_DOWN:
                    playerHippo.movePlayer(0, 1);
                    break;
                case KeyEvent.VK_LEFT:
                    playerHippo.movePlayer(-1, 0);
                    break;
                case KeyEvent.VK_RIGHT:
                    playerHippo.movePlayer(1, 0);
                    break;
                case KeyEvent.VK_SPACE:
                    // Fight off enemies if energy available
                    if (energyLevel >= 10) {
                        for (int i = 0; i < enemies.size(); i++) {
                            Animal enemy = enemies.get(i);
                            if (enemy.getBounds().intersects(playerHippo.getBounds())) {
                                out.println("COLLISION:" + playerId + ":" + enemy.getId());
                                energyLevel -= 10; // Energy penalty
                            }
                        }
                    }
                    break;
            }
            
            // Only send position update if player actually moved
            if (oldX != playerHippo.getX() || oldY != playerHippo.getY()) {
                out.println("POS:" + playerId + ":" + playerHippo.getX() + ":" + playerHippo.getY());
            }
        } else if (!gameStarted) {
            // Start screen controls
            if (e.getKeyCode() == KeyEvent.VK_SPACE && !isReady) {
                isReady = true;
                out.println("READY:" + playerId);
                repaint();
            }
        } else if (gameOver) {
            // Game over screen controls
            if (e.getKeyCode() == KeyEvent.VK_R) {
                out.println("RESET:" + playerId);
            }
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        // Not implemented but required by KeyListener interface
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
        // Not implemented but required by KeyListener interface
    }
    
    public void updateScore(int newScore) {
        score = newScore;
    }
    
    public void updateTime(int newTime) {
        time = newTime;
    }
    
    public void updateNumAnimals(int newNumAnimals) {
        numAnimals = newNumAnimals;
    }
    
    public int getScore() {
        return score;
    }
    
    public int getTime() {
        return time;
    }
    
    public int getNumAnimals() {
        return numAnimals;
    }
}
