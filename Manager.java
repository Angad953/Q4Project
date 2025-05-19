import java.awt.*;
import java.util.*;

public class Manager {
    public int totalScore;
    public int numAnimals;
    public MyArrayList<ServerThread> serverThreads;
    private HashMap<String, Integer> playerScores;
    private MyArrayList<Food> foods;
    private MyArrayList<Enemy> enemies;
    private boolean gameStarted;
    private int readyCount;
    private int resetRequests;
    private int gameTime;
    private Timer gameTimer;
    private long nextEnemySpawnTime = 0;
    private static final long ENEMY_SPAWN_INTERVAL = 5000;

    public class Food {
        int x, y;
        boolean eaten;
        String id;

        public Food(int x, int y) {
            this.x = x;
            this.y = y;
            this.eaten = false;
            this.id = "food_" + System.currentTimeMillis() % 10000 + "_" + (int) (Math.random() * 1000);
        }
    }

    public class Enemy {
        String id;
        int x, y;
        int size;

        public Enemy() {
            this.id = "enemy_" + System.currentTimeMillis() % 10000 + "_" + (int) (Math.random() * 1000);
            this.size = 50;
            Random rand = new Random();
            int side = rand.nextInt(4);
            switch (side) {
                case 0:
                    this.x = rand.nextInt(800);
                    this.y = -this.size;
                    break;
                case 1:
                    this.x = 800;
                    this.y = rand.nextInt(600);
                    break;
                case 2:
                    this.x = rand.nextInt(800);
                    this.y = 600;
                    break;
                case 3:
                    this.x = -this.size;
                    this.y = rand.nextInt(600);
                    break;
            }
        }
    }

    public Manager() {
        totalScore = 0;
        numAnimals = 0;
        serverThreads = new MyArrayList<>();
        playerScores = new HashMap<>();
        foods = new MyArrayList<>();
        enemies = new MyArrayList<>();
        gameStarted = false;
        readyCount = 0;
        resetRequests = 0;
        gameTime = 120;

        for (int i = 0; i < 20; i++) {
            Random rand = new Random();
            foods.add(new Food(rand.nextInt(700) + 50, rand.nextInt(500) + 50));
        }

        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(100); 
                    if (gameStarted) {
                        updateGameState();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void updateGameState() {
        Random rand = new Random();

        for (Enemy enemy : enemies) {
            int centerX = 400;
            int centerY = 300;

            double dx = centerX - enemy.x;
            double dy = centerY - enemy.y;
            double length = Math.sqrt(dx * dx + dy * dy);

            if (length > 0) {
                dx /= length;
                dy /= length;
            }

            dx += (rand.nextDouble() - 0.5) * 0.5;
            dy += (rand.nextDouble() - 0.5) * 0.5;

            enemy.x += dx * 3;
            enemy.y += dy * 3;

            broadcastEnemyPosition(enemy.id, enemy.x, enemy.y, enemy.size);
        }

        for (Enemy enemy : enemies) {
            Rectangle enemyBounds = new Rectangle(enemy.x, enemy.y, enemy.size, enemy.size);
            for (Food food : foods) {
                if (!food.eaten) {
                    Rectangle foodBounds = new Rectangle(food.x, food.y, 10, 10);
                    if (enemyBounds.intersects(foodBounds)) {
                        food.eaten = true;
                        broadcastNewFood(food.x, food.y, true);
                    }
                }
            }
        }

        //random food spawn logic
        if (rand.nextDouble() < 0.07) {
            Food newFood = new Food(rand.nextInt(700) + 50, rand.nextInt(500) + 50);
            foods.add(newFood);
            broadcastNewFood(newFood.x, newFood.y, false);
        }
        long currentTime = System.currentTimeMillis();

        if (gameStarted && currentTime >= nextEnemySpawnTime && enemies.size() < 5) {
            Enemy newEnemy = new Enemy();
            enemies.add(newEnemy);
            broadcastEnemyPosition(newEnemy.id, newEnemy.x, newEnemy.y, newEnemy.size);
            nextEnemySpawnTime = currentTime + ENEMY_SPAWN_INTERVAL;
        }

        if (foods.size() > 50) {
            foods.remove(0);
        }
    }

    public void addServerThread(ServerThread thread) {
        serverThreads.add(thread);
        broadcast("READY:" + readyCount + ":" + serverThreads.size());
    }

    public void removeServerThread(ServerThread thread) {
        serverThreads.remove(thread);
        if (thread.isReady()) {
            readyCount--;
        }

        if (thread.getPlayerId() != null) {
            playerScores.remove(thread.getPlayerId());
        }

        if (!gameStarted) {
            broadcast("READY:" + readyCount + ":" + serverThreads.size());
        }
    }

    public void playerReady() {
        readyCount++;
        broadcast("READY:" + readyCount + ":" + serverThreads.size());

        if (readyCount == serverThreads.size() && serverThreads.size() > 0) {
            startGame();
        }
    }

    private void startGame() {
        gameStarted = true;
        broadcast("START");
        gameTime = 120;
        gameTimer = new Timer();
        gameTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                gameTime--;
                if (gameTime <= 0) {
                    gameOver();
                    this.cancel();
                }
            }
        }, 1000, 1000);
    }

    public void updateScore(String player, int score) {
        playerScores.put(player, score);
        for (Integer s : playerScores.values()) {
            totalScore += s;
        }
        broadcast("SCORE:" + player + ":" + score);
    }

    public void incrementAnimals() {
        numAnimals++;
        broadcast("ANIMALCOUNT:" + numAnimals);
    }

    public void gameOver() {
        gameStarted = false;
        if (gameTimer != null) {
            gameTimer.cancel();
            gameTimer = null;
        }
        broadcast("GAMEOVER");
    }

    public void loss() {
        gameStarted = false;
        if (gameTimer != null) {
            gameTimer.cancel();
            gameTimer = null;
        }
        broadcast("DEAD");
    }

    public void resetRequest() {
        resetRequests++;
        if (resetRequests == serverThreads.size() && serverThreads.size() > 0) {
            resetGame();
        }
    }

    private void resetGame() {
        gameStarted = false;
        readyCount = 0;
        resetRequests = 0;
        totalScore = 0;
        numAnimals = 0;
        playerScores.clear();
        for (ServerThread thread : serverThreads) {
            thread.setReady(false);
        }
        foods.clear();
        enemies.clear();
        Random rand = new Random();
        for (int i = 0; i < 20; i++) {
            foods.add(new Food(rand.nextInt(700) + 50, rand.nextInt(500) + 50));
        }
        broadcast("RESET");
    }

    public void broadcast(String message) {
        for (ServerThread thread : serverThreads) {
            thread.sendMessage(message);
        }
    }

    public void broadcastExcept(String message, String excludePlayerId) {
        for (ServerThread thread : serverThreads) {
            if (thread.getPlayerId() != null && !thread.getPlayerId().equals(excludePlayerId)) {
                thread.sendMessage(message);
            }
        }
    }

    public void broadcastPlayerPosition(String playerId, int x, int y, String colorStr) {
        String message = "PLAYER:" + playerId + ":" + x + ":" + y + ":" + colorStr;
        broadcast(message);
    }

    public void broadcastPlayerRemoval(String playerId) {
        if (playerId != null) {
            String message = "REMOVE_PLAYER:" + playerId;
            broadcast(message);
        }
    }

    public void broadcastEnemyPosition(String enemyId, int x, int y, int size) {
        String message = "ENEMY:" + enemyId + ":" + x + ":" + y + ":" + size;
        broadcast(message);
    }

    public void broadcastNewFood(int x, int y, boolean eaten) {
        String message = "FOOD:" + x + ":" + y + ":" + eaten;
        broadcast(message);
    }

    public void removeEnemy(String enemyId) {
        for (int i = 0; i < enemies.size(); i++) {
            if (enemies.get(i).id.equals(enemyId)) {
                enemies.remove(i);
                break;
            }
        }
        broadcast("REMOVE_ENEMY:" + enemyId);
    }

    public void addPlayer(String player) {
        playerScores.put(player, 0);
        broadcast("SCORE:" + player + ":0");
    }

    public String getCurrentGameState() {
        StringBuilder sb = new StringBuilder();
        sb.append("SYNC:");

        int validPlayers = 0;
        for (ServerThread thread : serverThreads) {
            if (thread.getPlayerId() != null) {
                validPlayers++;
            }
        }

        sb.append(validPlayers).append(":");
        for (ServerThread thread : serverThreads) {
            String id = thread.getPlayerId();
            if (id != null) {
                sb.append(id).append(":");
                sb.append(thread.getPlayerX()).append(":");
                sb.append(thread.getPlayerY()).append(":");
                sb.append(thread.getPlayerColor()).append(":");
            }
        }

        sb.append(foods.size()).append(":");
        for (Food food : foods) {
            sb.append(food.x).append(":");
            sb.append(food.y).append(":");
            sb.append(food.eaten).append(":");
        }

        sb.append(enemies.size()).append(":");
        for (Enemy enemy : enemies) {
            sb.append(enemy.id).append(":");
            sb.append(enemy.x).append(":");
            sb.append(enemy.y).append(":");
            sb.append(enemy.size).append(":");
        }

        sb.append(playerScores.size()).append(":");
        for (String player : playerScores.keySet()) {
            sb.append(player).append(":");
            sb.append(playerScores.get(player)).append(":");
        }

        sb.append(gameStarted).append(":");
        sb.append(gameTime);

        return sb.toString();
    }

    public void checkFoodCollision(String playerId, int x, int y) {
        Rectangle playerBounds = new Rectangle(x, y, 80, 50);
        for (Food food : foods) {
            if (!food.eaten) {
                Rectangle foodBounds = new Rectangle(food.x, food.y, 10, 10);
                if (playerBounds.intersects(foodBounds)) {
                    food.eaten = true;
                    broadcastNewFood(food.x, food.y, true);

                    Integer score = playerScores.getOrDefault(playerId, 0);
                    score++;
                    updateScore(playerId, score);
                }
            }
        }
    }

    public void handlePlayerEnemyCollision(String playerId, String enemyId) {
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            if (enemy.id.equals(enemyId)) {
                enemies.remove(i);

                broadcast("REMOVE_ENEMY:" + enemyId);

                incrementAnimals();

                Integer score = playerScores.getOrDefault(playerId, 0);
                score++;
                updateScore(playerId, score);

                break;
            }
        }
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public int getGameTime() {
        return gameTime;
    }

    public MyArrayList<ServerThread> getServerThreads() {
        return serverThreads;
    }

    public HashMap<String, Integer> getPlayerScores() {
        return playerScores;
    }

    public MyArrayList<Food> getFoods() {
        return foods;
    }

    public MyArrayList<Enemy> getEnemies() {
        return enemies;
    }
}