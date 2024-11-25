import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Node extends Thread {
    private final int id;
    private final int port;
    private final List<Integer> otherNodePorts;
    private final ExecutorService executor;
    private final Map<Integer, Boolean> acknowledgements;

    public Node(int id, int port, List<Integer> otherNodePorts) {
        this.id = id;
        this.port = port;
        this.otherNodePorts = otherNodePorts;
        this.executor = Executors.newFixedThreadPool(5); // Para lidar com múltiplas conexões
        this.acknowledgements = new ConcurrentHashMap<>();
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
                // Aguarda confirmação de todos os nós
                waitForAcknowledgements();

                // Após receber todas as confirmações, limpa o mapa de acknowledgements
                acknowledgements.clear();
                Thread.sleep(10000); // Envia mensagens a cada 10 segundos
            }
        } catch (InterruptedException e) {
            System.out.println("Nó " + id + " encerrado.");
        }
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Nó " + id + " recebeu: " + message);
                // Envia confirmação de recebimento
                out.println("ACK de nó " + id);
                // Marca que o nó recebeu a mensagem
                acknowledgements.put(id, true);
            }
        } catch (IOException e) {
            System.out.println("Erro ao processar mensagem no nó " + id + ": " + e.getMessage());
        }
    }

    private void waitForAcknowledgements() throws InterruptedException {
        // Aguarda até que todas as confirmações de recebimento sejam recebidas
        while (acknowledgements.size() < otherNodePorts.size()) {
            Thread.sleep(100); // Espera por um curto período antes de verificar novamente
        }
        System.out.println("Nó " + id + " recebeu confirmações de todos os nós.");
    }
}