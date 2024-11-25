import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Node extends Thread {
    private final int id;
    private final int port;
    private final List<Integer> otherNodePorts;
    private final ExecutorService executor;

    public Node(int id, int port, List<Integer> otherNodePorts) {
        this.id = id;
        this.port = port;
        this.otherNodePorts = otherNodePorts;
        this.executor = Executors.newFixedThreadPool(5); // Para lidar com múltiplas conexões
    }

    @Override
    public void run() {
        // Thread para ouvir mensagens
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("Nó " + id + " ouvindo na porta " + port);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    executor.submit(() -> handleClient(clientSocket));
                }
            } catch (IOException e) {
                System.out.println("Erro no nó " + id + ": " + e.getMessage());
            }
        }).start();

        // Thread para enviar mensagens periodicamente
        try {
            while (true) {
                for (int otherPort : otherNodePorts) {
                    try (Socket socket = new Socket("localhost", otherPort);
                         PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                        out.println("Mensagem do nó " + id + " para porta " + otherPort);
                    } catch (IOException e) {
                        System.out.println("Nó " + id + ": Falha ao enviar mensagem para porta " + otherPort);
                    }
                }
                Thread.sleep(2000); // Envia mensagens a cada 2 segundos
            }
        } catch (InterruptedException e) {
            System.out.println("Nó " + id + " encerrado.");
        }
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Nó " + id + " recebeu: " + message);
            }
        } catch (IOException e) {
            System.out.println("Erro ao processar mensagem no nó " + id + ": " + e.getMessage());
        }
    }
}