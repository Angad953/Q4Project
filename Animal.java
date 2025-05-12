import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Random;

public class Animal {
    private int x, y;
    private int width, height;
    private int speed;
    private Color color;
    private String id; // Unique ID for each player
    private boolean isPlayer;
    private String type;
    private int direction;
    private Random rand;
    
    // Constructor for enemy animals
    public Animal(String type) {
        rand = new Random();
        this.type = type;
        this.isPlayer = false;
        this.speed = 2;
        this.width = 50;
        this.height = 50; // Make it square
        this.id = "enemy_" + System.currentTimeMillis() % 10000 + "_" + (int)(Math.random() * 1000);
        
        // Enemies spawn at the edges
        int side = rand.nextInt(4);
        switch(side) {
            case 0: // Top
                this.x = rand.nextInt(800);
                this.y = -height;
                break;
            case 1: // Right
                this.x = 800;
                this.y = rand.nextInt(600);
                break;
            case 2: // Bottom
                this.x = rand.nextInt(800);
                this.y = 600;
                break;
            case 3: // Left
                this.x = -width;
                this.y = rand.nextInt(600);
                break;
        }
        
        this.direction = rand.nextInt(360);
        this.color = new Color(rand.nextInt(200), rand.nextInt(200), rand.nextInt(200));
    }
    
    // Constructor for player hippos
    public Animal(int x, int y, Color color, String id) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.id = id;
        this.type = "Hippo";
        this.isPlayer = true;
        this.speed = 5;
        this.width = 80;
        this.height = 50;
        this.rand = new Random();
    }
    
    public void move() {
        if (!isPlayer) {
            // Random movement for enemies with occasional direction changes
            if (rand.nextInt(100) < 5) {
                direction = rand.nextInt(360);
            }
            
            double radians = Math.toRadians(direction);
            x += Math.cos(radians) * speed;
            y += Math.sin(radians) * speed;
            
            // Bounce off walls
            if (x < 0 || x > 800 - width) {
                direction = 180 - direction;
            }
            if (y < 0 || y > 600 - height) {
                direction = 360 - direction;
            }
        }
    }
    
    public void movePlayer(int dx, int dy) {
        x += dx * speed;
        y += dy * speed;
        
        // Keep within bounds
        if (x < 0) x = 0;
        if (x > 800 - width) x = 800 - width;
        if (y < 0) y = 0;
        if (y > 600 - height) y = 600 - height;
    }
    
    public void draw(Graphics g) {
        g.setColor(color);
        
        if (isPlayer) {
            // Draw hippo as oval
            g.fillOval(x, y, width, height);
            
            // Draw eyes
            g.setColor(Color.BLACK);
            g.fillOval(x + width/4, y + height/4, 5, 5);
            g.fillOval(x + 3*width/4, y + height/4, 5, 5);
        } else {
            // Draw enemies as squares
            g.fillRect(x, y, width, height);
            
            // Draw eyes
            g.setColor(Color.BLACK);
            g.fillOval(x + width/4, y + height/4, 5, 5);
            g.fillOval(x + 3*width/4, y + height/4, 5, 5);
        }
    }
    
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
    
    public boolean isPlayer() {
        return isPlayer;
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public void setSize(int size) {
        this.width = size;
        
        if (isPlayer) {
            // For hippos, maintain aspect ratio
            this.height = size * 2 / 3;
        } else {
            // For enemies, make them square
            this.height = size;
        }
    }
    
    public String getId() {
        return id;
    }
    
    public void setColor(Color color) {
        this.color = color;
    }
    
    public Color getColor() {
        return color;
    }
    
    public String getType() {
        return type;
    }
    
    // Create a method to convert color to string representation for network
    public String getColorAsString() {
        return color.getRed() + "," + color.getGreen() + "," + color.getBlue();
    }
    
    // Create a method to convert string back to color
    public static Color stringToColor(String colorStr) {
        String[] parts = colorStr.split(",");
        return new Color(
            Integer.parseInt(parts[0]),
            Integer.parseInt(parts[1]),
            Integer.parseInt(parts[2])
        );
    }
    public void setId(String id) {
        this.id = id;
    }
}
