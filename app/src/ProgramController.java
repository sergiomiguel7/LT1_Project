import java.time.Instant;
import java.util.Scanner;

public class ProgramController {


    public static void main(String[] args) {
        Communication communication = Communication.getInstance();

        Scanner scanner = new Scanner(System.in);
        int op = Integer.parseInt(scanner.next());
        if (op == 1) {
            System.out.println("Sending file...");
            communication.sendFile();

        }

        while (true) {
            communication.stringToByte(communication.readUserMessage());
        }
    }

}
