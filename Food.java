import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Random;

public class Food {
    private int x, y;
    private int size;
    private Color color;
    private boolean isEaten;
    private String id; // Unique ID for synchronization
    
    public Food() {
        Random rand = new Random();
        this.x = rand.nextInt(700) + 50; // Random position within game area
        this.y = rand.nextInt(500) + 50;
        this.size = 10;
        this.color = new Color(rand.nextInt(100) + 155, rand.nextInt(100) + 155, 0); // Yellowish-green colors for food
        this.isEaten = false;
        this.id = "food_" + System.currentTimeMillis() % 10000 + "_" + (int)(Math.random() * 1000);
    }
    
    public Food(int x, int y) {
        this.x = x;
        this.y = y;
        this.size = 10;
        this.color = new Color(100, 200, 0);
        this.isEaten = false;
        this.id = "food_" + System.currentTimeMillis() % 10000 + "_" + (int)(Math.random() * 1000);
    }
    
    public void draw(Graphics g) {
        if (!isEaten) {
            g.setColor(color);
            g.fillOval(x, y, size, size);
        }
    }
    
    public Rectangle getBounds() {
        return new Rectangle(x, y, size, size);
    }
    
    public boolean isEaten() {
        return isEaten;
    }
    
    public void setEaten(boolean eaten) {
        this.isEaten = eaten;
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public String getId() {
        return id;
    }
}