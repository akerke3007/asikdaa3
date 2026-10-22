package graph;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class GraphGenerator {
    public static Graph connectedUndirected(int vertices, int edges, long seed) {
        checkSize(vertices, edges, (long) vertices * (vertices - 1) / 2, vertices - 1);
        Graph graph = new Graph(vertices, false);
        Random random = new Random(seed);
        Set<Long> used = new HashSet<>();
        for (int vertex = 1; vertex < vertices; vertex++) {
            add(graph, used, random.nextInt(vertex), vertex, random);
        }
        while (graph.edgeCount() < edges) {
            int a = random.nextInt(vertices);
            int b = random.nextInt(vertices);
            if (a != b) {
                add(graph, used, a, b, random);
            }
        }
        return graph;
    }

    public static Graph dag(int vertices, int edges, long seed) {
        checkSize(vertices, edges, (long) vertices * (vertices - 1) / 2, 0);
        Graph graph = new Graph(vertices, true);
        Random random = new Random(seed);
        Set<Long> used = new HashSet<>();
        while (graph.edgeCount() < edges) {
            int a = random.nextInt(vertices);
            int b = random.nextInt(vertices);
            if (a != b) {
                add(graph, used, Math.min(a, b), Math.max(a, b), random);
            }
        }
        return graph;
    }

    public static Graph cyclicBlocks(int vertices, int edges, long seed) {
        if (vertices < 5 || vertices % 5 != 0) {
            throw new IllegalArgumentException("Cyclic graphs need a multiple of five vertices.");
        }
        long blocks = vertices / 5;
        long maximum = blocks * 20 + blocks * (blocks - 1) / 2 * 25;
        checkSize(vertices, edges, maximum, vertices);
        Graph graph = new Graph(vertices, true);
        Random random = new Random(seed);
        Set<Long> used = new HashSet<>();
        for (int base = 0; base < vertices; base += 5) {
            for (int offset = 0; offset < 5; offset++) {
                add(graph, used, base + offset, base + (offset + 1) % 5, random);
            }
        }
        while (graph.edgeCount() < edges) {
            int a = random.nextInt(vertices);
            int b = random.nextInt(vertices);
            if (a == b) {
                continue;
            }
            if (a / 5 > b / 5) {
                int temporary = a;
                a = b;
                b = temporary;
            }
            add(graph, used, a, b, random);
        }
        return graph;
    }

    private static void add(Graph graph, Set<Long> used, int a, int b, Random random) {
        if (!graph.directed() && a > b) {
            int temporary = a;
            a = b;
            b = temporary;
        }
        long key = (long) a * graph.vertices() + b;
        if (used.add(key)) {
            graph.addEdge(a, b, 1 + random.nextInt(100));
        }
    }

    private static void checkSize(int vertices, int edges, long maximum, int minimum) {
        if (vertices < 1 || edges < minimum || edges > maximum) {
            throw new IllegalArgumentException("Impossible graph size.");
        }
    }
}
