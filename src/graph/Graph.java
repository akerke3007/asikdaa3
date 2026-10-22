package graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Graph {
    private final boolean directed;
    private final List<Edge> edges = new ArrayList<>();
    private final List<List<Edge>> adjacency = new ArrayList<>();

    public Graph(int vertices, boolean directed) {
        if (vertices < 1) {
            throw new IllegalArgumentException("A graph needs at least one vertex.");
        }
        this.directed = directed;
        for (int i = 0; i < vertices; i++) {
            adjacency.add(new ArrayList<>());
        }
    }

    public void addEdge(int source, int destination, double weight) {
        checkVertex(source);
        checkVertex(destination);
        if (!Double.isFinite(weight)) {
            throw new IllegalArgumentException("Weight must be finite.");
        }
        Edge edge = new Edge(source, destination, weight);
        edges.add(edge);
        adjacency.get(source).add(edge);
        if (!directed) {
            adjacency.get(destination).add(new Edge(destination, source, weight));
        }
    }

    public int vertices() {
        return adjacency.size();
    }

    public int edgeCount() {
        return edges.size();
    }

    public boolean directed() {
        return directed;
    }

    public List<Edge> edges() {
        return Collections.unmodifiableList(edges);
    }

    public List<Edge> neighbors(int vertex) {
        checkVertex(vertex);
        return Collections.unmodifiableList(adjacency.get(vertex));
    }

    public void checkVertex(int vertex) {
        if (vertex < 0 || vertex >= vertices()) {
            throw new IllegalArgumentException("Invalid vertex: " + vertex);
        }
    }
}
