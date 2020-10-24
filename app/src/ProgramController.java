public class ProgramController {


    public static void main(String[] args) {
        Communication communication = Communication.getInstance();
        while (true){
            communication.stringToByte(communication.readUserMessage());
        }
    }

}
