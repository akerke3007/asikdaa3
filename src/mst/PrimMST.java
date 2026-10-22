package mst;

import graph.Edge;
import graph.Graph;
import graph.Metrics;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public final class PrimMST {
    public MstResult compute(Graph graph) {
        if (graph.directed()) {
            throw new IllegalArgumentException("MST needs an undirected graph.");
        }
        Metrics metrics = new Metrics();
        PriorityQueue<Edge> queue = new PriorityQueue<>((first, second) -> {
            metrics.comparisons++;
            return Double.compare(first.weight(), second.weight());
        });
        boolean[] visited = new boolean[graph.vertices()];
        List<Edge> selected = new ArrayList<>();
        double weight = 0;
        int components = 0;
        for (int start = 0; start < graph.vertices(); start++) {
            if (visited[start]) {
                continue;
            }
            components++;
            visit(graph, start, visited, queue, metrics);
            while (!queue.isEmpty()) {
                Edge edge = queue.poll();
                metrics.queuePolls++;
                if (visited[edge.destination()]) {
                    continue;
                }
                selected.add(edge);
                weight += edge.weight();
                visit(graph, edge.destination(), visited, queue, metrics);
            }
        }
        return new MstResult(selected, weight, components, metrics);
    }

    private void visit(Graph graph, int vertex, boolean[] visited,
                       PriorityQueue<Edge> queue, Metrics metrics) {
        visited[vertex] = true;
        metrics.vertexVisits++;
        for (Edge edge : graph.neighbors(vertex)) {
            metrics.edgeExaminations++;
            if (!visited[edge.destination()]) {
                queue.add(edge);
                metrics.queueOffers++;
            }
        }
    }
}
