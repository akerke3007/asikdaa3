package dsu;

import graph.Metrics;

public final class DisjointSetUnion {
    private final int[] parent;
    private final int[] size;
    private final Metrics metrics;
    private int components;

    public DisjointSetUnion(int vertices) {
        this(vertices, new Metrics());
    }

    public DisjointSetUnion(int vertices, Metrics metrics) {
        if (vertices < 1) {
            throw new IllegalArgumentException("DSU size must be positive.");
        }
        this.metrics = metrics;
        parent = new int[vertices];
        size = new int[vertices];
        components = vertices;
        for (int i = 0; i < vertices; i++) {
            parent[i] = i;
            size[i] = 1;
        }
    }

    public int find(int vertex) {
        if (vertex < 0 || vertex >= parent.length) {
            throw new IllegalArgumentException("Invalid DSU vertex.");
        }
        metrics.findCalls++;
        int root = vertex;
        while (root != parent[root]) {
            root = parent[root];
            metrics.parentTraversals++;
        }
        while (vertex != root) {
            int next = parent[vertex];
            if (parent[vertex] != root) {
                parent[vertex] = root;
                metrics.parentUpdates++;
            }
            vertex = next;
            metrics.parentTraversals++;
        }
        return root;
    }

    public boolean union(int first, int second) {
        metrics.unionCalls++;
        int a = find(first);
        int b = find(second);
        if (a == b) {
            return false;
        }
        if (size[a] < size[b]) {
            int temporary = a;
            a = b;
            b = temporary;
        }
        parent[b] = a;
        size[a] += size[b];
        metrics.parentUpdates++;
        components--;
        return true;
    }

    public int components() {
        return components;
    }

    public Metrics metrics() {
        return metrics;
    }
}
