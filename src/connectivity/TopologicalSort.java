package connectivity;

import graph.Edge;
import graph.Graph;
import graph.Metrics;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class TopologicalSort {
    public record Result(List<Integer> order, boolean hasCycle, Metrics metrics) {
        public Result {
            order = List.copyOf(order);
        }
    }

    public Result compute(Graph graph) {
        if (!graph.directed()) {
            throw new IllegalArgumentException("Topological ordering needs a directed graph.");
        }
        Metrics metrics = new Metrics();
        int[] indegree = new int[graph.vertices()];
        for (Edge edge : graph.edges()) {
            indegree[edge.destination()]++;
            metrics.edgeExaminations++;
        }
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int vertex = 0; vertex < graph.vertices(); vertex++) {
            if (indegree[vertex] == 0) {
                queue.add(vertex);
                metrics.queueOffers++;
            }
        }
        List<Integer> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            int vertex = queue.remove();
            metrics.queuePolls++;
            metrics.vertexVisits++;
            order.add(vertex);
            for (Edge edge : graph.neighbors(vertex)) {
                metrics.edgeExaminations++;
                if (--indegree[edge.destination()] == 0) {
                    queue.add(edge.destination());
                    metrics.queueOffers++;
                }
            }
        }
        boolean hasCycle = order.size() != graph.vertices();
        return new Result(hasCycle ? List.of() : order, hasCycle, metrics);
    }
}
