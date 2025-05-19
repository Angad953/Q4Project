import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.net.Socket;
import java.util.*;
import javax.swing.JPanel;

public class ClientScreen extends JPanel implements KeyListener, Runnable {
    private int score;
    private int time;
    private int numAnimals;
    private boolean gameStarted;
    private boolean gameOver;
    private boolean loss;
    private String gameStatus;
    private Animal playerHippo;
    private MyArrayList<Food> foods;
    private MyArrayList<Animal> enemies;
    private HashMap<String, Integer> playerScores;
    private HashMap<String, Animal> otherPlayers;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isReady;
    private Thread gameThread;
    private int energyLevel;
    private String playerId;

    public ClientScreen() {
        this.setPreferredSize(new Dimension(800, 600));
        this.setBackground(Color.WHITE);
        this.setFocusable(true);
        this.addKeyListener(this);

        score = 0;
        time = 120;
        numAnimals = 0;
        gameStarted = false;
        gameOver = false;
        loss = false;
        gameStatus = "PRESS SPACE TO READY UP";
        isReady = false;
        energyLevel = 100;
        playerId = "Player" + System.currentTimeMillis() % 10000;
        foods = new MyArrayList<>();
        enemies = new MyArrayList<>();
        playerScores = new HashMap<>();
        otherPlayers = new HashMap<>();

        int x = 100 + (int) (Math.random() * 600);
        int y = 100 + (int) (Math.random() * 400);
        Color[] hippoColors = { Color.YELLOW, Color.PINK };
        int colorIndex = (int) (Math.random() * hippoColors.length);
        playerHippo = new Animal(x, y, hippoColors[colorIndex], playerId);

        for (int i = 0; i < 20; i++) {
            foods.add(new Food());
        }
    }

    public void connect() {
        try {
            socket = new Socket("localhost", 1024);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            gameThread = new Thread(this);
            gameThread.start();
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
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.drawString("HUNGRY HUNGRY HIPPOS", 200, 200);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.drawString(gameStatus, 250, 250);
            g.drawString("Eat as much food as possible and fight off animals trying to steal the team's food!", 50,
                    300);
            g.drawString("To win, make sure that each individual score is over 50!", 150, 350);
            g.drawString("Press SPACE to ready up", 280, 500);
            if (isReady) {
                g.setColor(Color.GREEN);
                g.drawString("YOU ARE READY!", 310, 400);
            }
        } else if (gameOver && !loss) {
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.drawString("GAME OVER", 300, 200);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.drawString("Final Score: " + score, 320, 250);
            g.drawString("Press R to play again", 300, 300);
        } else if (loss) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.drawString("YOU LOSE", 300, 200);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.drawString("Final Score: " + score, 320, 250);
            g.drawString("Press R to play again", 300, 300);
        } else {
            g.setColor(new Color(200, 240, 200));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(100, 150, 255));
            g.fillOval(getWidth() / 2 - 150, getHeight() / 2 - 100, 300, 200);

            for (Food food : foods) {
                food.draw(g);
            }

            for (Animal enemy : enemies) {
                enemy.draw(g);
            }

            for (Animal otherPlayer : otherPlayers.values()) {
                otherPlayer.draw(g);
                g.setColor(Color.BLACK);
                g.setFont(new Font("Arial", Font.BOLD, 12));
                g.drawString(otherPlayer.getId(),
                        otherPlayer.getX() + 20, otherPlayer.getY() - 5);
            }

            playerHippo.draw(g);

            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            g.drawString(playerId, playerHippo.getX() + 20, playerHippo.getY() - 5);
            drawHUD(g);
        }
    }

    private void drawHUD(Graphics g) {
        g.setColor(new Color(0, 0, 0, 128));
        g.fillRect(0, 0, getWidth(), 100);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Score: " + score, 20, 20);
        g.drawString("Time: " + time, 20, 40);
        g.drawString("Animals Deflected: " + numAnimals, 20, 60);
        ;
        g.drawString("Energy: ", 20, 80);
        g.setColor(new Color(255, 50, 50));
        g.fillRect(90, 65, energyLevel, 15);
        g.setColor(Color.WHITE);
        g.drawRect(90, 65, 100, 15);

        int yPos = 20;
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        for (String player : playerScores.keySet()) {
            if (!player.equals(playerId)) {
                g.drawString(player + ": " + playerScores.get(player), 650, yPos);
                yPos += 20;
            }
        }

        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("Team Score: " + getTotalScore(), getWidth() / 2 - 70, 30);
    }

    private int getTotalScore() {
        int total = 0;
        for (Integer s : playerScores.values()) {
            total += s;
        }
        return total;
    }

    private void updateGame() {
        for (int i = 0; i < enemies.size(); i++) {
            Animal enemy = enemies.get(i);
            enemy.move();

            for (Food food : foods) {
                if (!food.isEaten() && enemy.getBounds().intersects(food.getBounds())) {
                    food.setEaten(true);
                }
            }

            if (enemy.getBounds().intersects(playerHippo.getBounds())) {
                out.println("COLLISION:" + playerId + ":" + enemy.getId());
                enemies.remove(i);
                i--;
            }
        }
        out.println("POS:" + playerId + ":" + playerHippo.getX() + ":" + playerHippo.getY());

        setLoss();

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
            new Thread(() -> {
                while (time > 0 && gameStarted && !gameOver) {
                    try {
                        Thread.sleep(1000);
                        time--;
                        energyLevel--;
                        out.println("ALIVE:" + playerId);

                        if (time <= 0 && !loss) {
                            gameOver = true;
                            out.println("GAMEOVER:" + playerId);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

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
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            boolean eaten = Boolean.parseBoolean(parts[3]);

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
            String id = parts[1];
            int x = Integer.parseInt(parts[2]);
            int y = Integer.parseInt(parts[3]);
            Color color = Animal.stringToColor(parts[4]);

            if (!id.equals(playerId)) {
                Animal otherPlayer = otherPlayers.get(id);
                if (otherPlayer == null) {
                    otherPlayer = new Animal(x, y, color, id);
                    otherPlayers.put(id, otherPlayer);
                } else {
                    otherPlayer.setPosition(x, y);
                }
            }

        } else if (parts[0].equals("REMOVE_PLAYER")) {
            String id = parts[1];
            otherPlayers.remove(id);

        } else if (parts[0].equals("ENEMY")) {
            String id = parts[1];
            int x = Integer.parseInt(parts[2]);
            int y = Integer.parseInt(parts[3]);
            int size = Integer.parseInt(parts[4]);

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
                enemy.setId(id);
                enemy.setPosition(x, y);
                enemy.setSize(size);
                enemies.add(enemy);
            }
        }
        if (parts[0].equals("REMOVE_ENEMY")) {
            String id = parts[1];
            boolean removed = false;

            for (int i = 0; i < enemies.size(); i++) {
                if (enemies.get(i).getId().equals(id)) {
                    enemies.remove(i);
                    removed = true;
                    break;
                }
            }

            if (!removed) {
            }

        } else if (parts[0].equals("SCORE")) {
            String player = parts[1];
            int playerScore = Integer.parseInt(parts[2]);
            playerScores.put(player, playerScore);

            if (player.equals(playerId)) {
                energyLevel++;
                score = playerScore;
            }

        } else if (parts[0].equals("ANIMALCOUNT")) {
            numAnimals = Integer.parseInt(parts[1]);

        } else if (parts[0].equals("READY")) {
            int readyCount = Integer.parseInt(parts[1]);
            int totalCount = Integer.parseInt(parts[2]);
            gameStatus = readyCount + " OF " + totalCount + " PLAYERS READY";

        } else if (parts[0].equals("GAMEOVER")) {
            gameOver = true;

        } else if (parts[0].equals("LOSS")) {
            loss = true;
            gameOver = true;
            gameStatus = "YOU LOSE";
            repaint();

        } else if (parts[0].equals("ALIVE")) {
        } else if (parts[0].equals("RESET")) {
            resetGame();

        } else if (parts[0].equals("SYNC")) {
            // Full game state sync from server
            syncGameState(parts);
        }
    }

    private void syncGameState(String[] parts) {
        try {
            int index = 1;
            int playerCount = Integer.parseInt(parts[index++]);
            otherPlayers.clear();

            for (int i = 0; i < playerCount && index + 3 < parts.length; i++) {
                String id = parts[index++];
                int x = Integer.parseInt(parts[index++]);
                int y = Integer.parseInt(parts[index++]);
                Color color = Animal.stringToColor(parts[index++]);

                if (!id.equals(playerId)) {
                    Animal player = new Animal(x, y, color, id);
                    otherPlayers.put(id, player);
                }
            }

            if (index < parts.length) {
                int foodCount = Integer.parseInt(parts[index++]);

                foods.clear();

                for (int i = 0; i < foodCount && index + 2 < parts.length; i++) {
                    int x = Integer.parseInt(parts[index++]);
                    int y = Integer.parseInt(parts[index++]);
                    boolean eaten = Boolean.parseBoolean(parts[index++]);

                    Food food = new Food(x, y);
                    food.setEaten(eaten);
                    foods.add(food);
                }
            }

            if (index < parts.length) {
                int enemyCount = Integer.parseInt(parts[index++]);
                enemies.clear();

                for (int i = 0; i < enemyCount && index + 3 < parts.length; i++) {
                    String id = parts[index++];
                    int x = Integer.parseInt(parts[index++]);
                    int y = Integer.parseInt(parts[index++]);
                    int size = Integer.parseInt(parts[index++]);

                    Animal enemy = new Animal("Enemy");
                    enemy.setId(id);
                    enemy.setPosition(x, y);
                    enemy.setSize(size);
                    enemies.add(enemy);
                }
            }

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
        foods.clear();
        enemies.clear();
        otherPlayers.clear();
        playerScores.clear();

        int x = 100 + (int) (Math.random() * 600);
        int y = 100 + (int) (Math.random() * 400);
        playerHippo.setPosition(x, y);

        for (int i = 0; i < 20; i++) {
            foods.add(new Food());
        }
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameStarted && !gameOver) {
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
            }
            if (oldX != playerHippo.getX() || oldY != playerHippo.getY()) {
                out.println("POS:" + playerId + ":" + playerHippo.getX() + ":" + playerHippo.getY());
            }
        } else if (!gameStarted) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE && !isReady) {
                isReady = true;
                out.println("READY:" + playerId);
                repaint();
            }
        } else if (gameOver) {
            if (e.getKeyCode() == KeyEvent.VK_R) {
                out.println("RESET:" + playerId);
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public void updateScore(int newScore) {
        score = newScore;
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

    public int getEnergyLevel() {
        return energyLevel;
    }

    public void setLoss(){
        if(gameStarted && energyLevel <= 0){
            loss = true;
            gameOver = true;
            gameStatus = "LOSS";
            out.println("LOSS:" + playerId);
            repaint();
        }
    }
}