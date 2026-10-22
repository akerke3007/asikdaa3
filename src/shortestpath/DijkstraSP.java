package shortestpath;

import graph.Edge;
import java.util.*;

public class DijkstraSP {
    private double[] distTo;
    private int[] edgeTo;
    private PriorityQueue<Node> pq;
    private long operationCount;

    private static class Node implements Comparable<Node> {
        int vertex;
        double distance;

        Node(int vertex, double distance) {
            this.vertex = vertex;
            this.distance = distance;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.distance, other.distance);
        }
    }

    public void compute(int vertices, List<List<Edge>> adj, int source) {
        distTo = new double[vertices];
        edgeTo = new int[vertices];
        Arrays.fill(distTo, Double.POSITIVE_INFINITY);
        Arrays.fill(edgeTo, -1);
        distTo[source] = 0.0;

        pq = new PriorityQueue<>();
        pq.add(new Node(source, 0.0));
        operationCount = 0;

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            int u = current.vertex;
            operationCount++;

            if (current.distance > distTo[u]) continue;

            for (Edge edge : adj.get(u)) {
                operationCount++;
                int v = edge.getDestination();
                double weight = edge.getWeight();

                if (weight < 0) {
                    throw new IllegalArgumentException("Dijkstra's algorithm does not support negative edge weights.");
                }

                if (distTo[u] + weight < distTo[v]) {
                    distTo[v] = distTo[u] + weight;
                    edgeTo[v] = u;
                    pq.add(new Node(v, distTo[v]));
                }
            }
        }
    }

    public double[] getDistTo() { return distTo; }
    public long getOperationCount() { return operationCount; }
}