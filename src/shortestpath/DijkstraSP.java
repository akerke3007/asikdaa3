package shortestpath;

import graph.Edge;
import graph.Graph;
import graph.Metrics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

public final class DijkstraSP {
    private record Node(int vertex, double distance) {}

    public static final class Result {
        private final double[] distances;
        private final int[] previous;
        private final Metrics metrics;

        private Result(double[] distances, int[] previous, Metrics metrics) {
            this.distances = distances;
            this.previous = previous;
            this.metrics = metrics;
        }

        public double distanceTo(int vertex) {
            return distances[vertex];
        }

        public List<Integer> pathTo(int vertex) {
            if (Double.isInfinite(distances[vertex])) {
                return List.of();
            }
            List<Integer> path = new ArrayList<>();
            for (int current = vertex; current != -1; current = previous[current]) {
                path.add(current);
            }
            Collections.reverse(path);
            return List.copyOf(path);
        }

        public Metrics metrics() {
            return metrics;
        }
    }

    public Result compute(Graph graph, int source) {
        graph.checkVertex(source);
        Metrics metrics = new Metrics();
        for (Edge edge : graph.edges()) {
            metrics.validationEdges++;
            if (edge.weight() < 0) {
                throw new IllegalArgumentException("Dijkstra needs non-negative weights.");
            }
        }
        double[] distances = new double[graph.vertices()];
        int[] previous = new int[graph.vertices()];
        Arrays.fill(distances, Double.POSITIVE_INFINITY);
        Arrays.fill(previous, -1);
        distances[source] = 0;
        PriorityQueue<Node> queue = new PriorityQueue<>((first, second) -> {
            metrics.comparisons++;
            return Double.compare(first.distance(), second.distance());
        });
        queue.add(new Node(source, 0));
        metrics.queueOffers++;
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            metrics.queuePolls++;
            if (current.distance() != distances[current.vertex()]) {
                continue;
            }
            metrics.vertexVisits++;
            for (Edge edge : graph.neighbors(current.vertex())) {
                metrics.edgeExaminations++;
                double candidate = current.distance() + edge.weight();
                if (!Double.isFinite(candidate)) {
                    throw new ArithmeticException("Path distance exceeds the double range.");
                }
                if (candidate < distances[edge.destination()]) {
                    distances[edge.destination()] = candidate;
                    previous[edge.destination()] = current.vertex();
                    queue.add(new Node(edge.destination(), candidate));
                    metrics.queueOffers++;
                    metrics.relaxations++;
                }
            }
        }
        return new Result(distances, previous, metrics);
    }
}
