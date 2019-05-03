import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.Scanner;

// An AWT GUI program inherits from the top-level container java.awt.Frame
public class UI extends Frame implements KeyListener {
    // This class acts as KeyEvent Listener
    private static StringBuilder sb = new StringBuilder();

    private static User me;

    static {
        try {
            me = new User("Caleb");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String room = me.joinExistingRoom("Christian#234234");


    private TextField tfInput;  // Single-line TextField to receive tfInput key
    static TextArea taDisplay; // Multi-line TextArea to taDisplay result

    // Constructor to setup the GUI components and event handlers
    UI() {
        while (!me.isRoomReady(room)) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignore) {
            }
        }
        System.out.println("live chat enabled");
        Scanner scnr = new Scanner(System.in);
        new Thread(() -> {
            while (true) {
                String msg = scnr.nextLine();
                if (msg.equals("quit")) break;
                me.sendToRoom(room, msg);
            }
        }).start();

        setLayout(new FlowLayout()); // "super" frame sets to FlowLayout

        add(new Label("Enter Text: "));
        taDisplay = new TextArea(60, 180); // 5 rows, 40 columns
        add(taDisplay);
        tfInput = new TextField(180);
        add(tfInput);


        tfInput.addKeyListener(this);
        // tfInput TextField (source) fires KeyEvent.
        // tfInput adds "this" object as a KeyEvent listener.

        setTitle("KeyEvent Demo"); // "super" Frame sets title
        setSize(1920, 1080);         // "super" Frame sets initial size
        setVisible(true);          // "super" Frame shows
    }

    /**
     * KeyEvent handlers
     */
    // Called back when a key has been typed (pressed and released)
    @Override
    public void keyTyped(KeyEvent evt) {
//        taDisplay.append("You have typed " + evt.getKeyChar() + "\n");
        sb.append(evt.getKeyChar());
        if (sb.toString().endsWith("\n")){
            sendMessage(sb.toString());
            sb.setLength(0);
        }
    }

    private void sendMessage(String message) {
        me.sendToRoom(room, message);
        tfInput.setText("");
    }


    // Not Used, but need to provide an empty body for compilation
    @Override
    public void keyPressed(KeyEvent evt) {
    }

    @Override
    public void keyReleased(KeyEvent evt) {
    }
}