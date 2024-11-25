import java.util.ArrayList;
import java.util.List;

public class DistributedSystem {
    public static void main(String[] args) {
        int numberOfNodes = 5;
        int basePort = 5000;

        List<Integer> ports = new ArrayList<>();
        for (int i = 0; i < numberOfNodes; i++) {
            ports.add(basePort + i);
        }

        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < numberOfNodes; i++) {
            List<Integer> otherNodePorts = new ArrayList<>(ports);
            otherNodePorts.remove((Integer) (basePort + i));
            Node node = new Node(i, basePort + i, otherNodePorts);
            nodes.add(node);
            node.start();
        }
    }
    
}
