import java.io.*;
import java.net.*;

public class Server {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(2121)) {
            System.out.println("Servidor FTP iniciado na porta 2121...");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Conexão recebida de: " + clientSocket.getInetAddress());
                
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ClientHandler extends Thread {
    private Socket controlSocket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean loggedIn = false;
    
    public ClientHandler(Socket socket) {
        this.controlSocket = socket;
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void run() {
        try {
            //resposta inicial ao abrir o servidor
            sendResponse("220 Bem-vindo ao servidor do Pedro");
            
            String command;//variável para definir o comando
            while ((command = in.readLine()) != null) {
                System.out.println("Comando recebido: " + command);
                //comando para entrar o nome de usuário
                if (command.startsWith("USER")) {
                    handleUSER(command);
                } 
                //comando para entrar a senha
                else if (command.startsWith("PASS")) {
                    handlePASS(command);
                } 
                //comando para sair
                else if (command.equalsIgnoreCase("QUIT")) {
                    sendResponse("221 Goodbye");
                    break;
                } 
                //comando para a lista
                else if (command.equalsIgnoreCase("LIST") && loggedIn) {
                    handleLIST();
                } 
                //caso de erro
                else {
                    sendResponse("500 Command not understood");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                controlSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    //comando USER
    private void handleUSER(String command) {
        if (command.equalsIgnoreCase("USER user")) {
            sendResponse("331 Password required");
        } else {
            sendResponse("530 Invalid username");
        }
    }
    

    //comando PASS
    private void handlePASS(String command) {
        if (command.equalsIgnoreCase("PASS pass")) {
            loggedIn = true;
            sendResponse("230 User logged in");
        } else {
            sendResponse("530 Login incorrect");
        }
    }
    
    //comando LIST
    private void handleLIST() {
        try {
            ServerSocket dataSocketServer = new ServerSocket(0);
            int port = dataSocketServer.getLocalPort();
            sendResponse(String.format("227 Entering Passive Mode (127,0,0,1,%d,%d)", port / 256, port % 256));
            
            Socket dataSocket = dataSocketServer.accept();
            PrintWriter dataOut = new PrintWriter(dataSocket.getOutputStream(), true);
            
            String[] fileList = {"file1.txt", "file2.txt", "file3.txt"};
            for (String file : fileList) {
                dataOut.println(file);
            }
            
            dataOut.close();
            dataSocket.close();
            dataSocketServer.close();
            
            sendResponse("226 Transfer complete");
        } catch (IOException e) {
            e.printStackTrace();
            sendResponse("550 Failed to open data connection");
        }
    }
    
    private void sendResponse(String response) {
        System.out.println("Enviando resposta: " + response);
        out.println(response);
    }
}
