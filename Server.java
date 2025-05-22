import javax.swing.*;
public class Server {
    public static void main(String[] args){
        JFrame frame = new JFrame("Server");
        ServerScreen newServer = new ServerScreen();


        frame.add(newServer);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        newServer.listen();
    }
}

