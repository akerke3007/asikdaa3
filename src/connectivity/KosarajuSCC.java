package connectivity;

import graph.Edge;
import graph.Graph;
import graph.Metrics;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class KosarajuSCC {
    public record Result(List<List<Integer>> components, Metrics metrics) {
        public Result {
            components = components.stream().map(List::copyOf).toList();
        }
    }

    public Result compute(Graph graph) {
        if (!graph.directed()) {
            throw new IllegalArgumentException("SCC needs a directed graph.");
        }
        Metrics metrics = new Metrics();
        boolean[] visited = new boolean[graph.vertices()];
        int[] nextEdge = new int[graph.vertices()];
        List<Integer> finish = new ArrayList<>();
        ArrayDeque<Integer> stack = new ArrayDeque<>();
        for (int start = 0; start < graph.vertices(); start++) {
            if (visited[start]) {
                continue;
            }
            visited[start] = true;
            metrics.vertexVisits++;
            stack.push(start);
            while (!stack.isEmpty()) {
                int vertex = stack.peek();
                List<Edge> neighbors = graph.neighbors(vertex);
                if (nextEdge[vertex] == neighbors.size()) {
                    finish.add(stack.pop());
                } else {
                    int next = neighbors.get(nextEdge[vertex]++).destination();
                    metrics.edgeExaminations++;
                    if (!visited[next]) {
                        visited[next] = true;
                        metrics.vertexVisits++;
                        stack.push(next);
                    }
                }
            }
        }
        List<List<Integer>> reverse = new ArrayList<>();
        for (int i = 0; i < graph.vertices(); i++) {
            reverse.add(new ArrayList<>());
        }
        for (Edge edge : graph.edges()) {
            reverse.get(edge.destination()).add(edge.source());
            metrics.edgeExaminations++;
        }
        Arrays.fill(visited, false);
        List<List<Integer>> components = new ArrayList<>();
        for (int i = finish.size() - 1; i >= 0; i--) {
            int start = finish.get(i);
            if (visited[start]) {
                continue;
            }
            List<Integer> component = new ArrayList<>();
            visited[start] = true;
            stack.push(start);
            while (!stack.isEmpty()) {
                int vertex = stack.pop();
                metrics.vertexVisits++;
                component.add(vertex);
                for (int next : reverse.get(vertex)) {
                    metrics.edgeExaminations++;
                    if (!visited[next]) {
                        visited[next] = true;
                        stack.push(next);
                    }
                }
            }
            components.add(component);
        }
        return new Result(components, metrics);
    }
}
