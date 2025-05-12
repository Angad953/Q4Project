import java.io.IOException;
import javax.swing.JFrame;

public class Client {
    public static void main(String[] args) throws IOException{
        String serverIP = "localhost"; 
        int port = 1024;


        JFrame frame = new JFrame("Hungry Hungry Hippos");
        ClientScreen sc = new ClientScreen();


        frame.add(sc);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        sc.connect();
    }
}
