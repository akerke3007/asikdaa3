package testing;

import connectivity.KosarajuSCC;
import connectivity.TopologicalSort;
import dsu.DisjointSetUnion;
import graph.Edge;
import graph.Graph;
import graph.GraphGenerator;
import mst.KruskalMST;
import mst.MstResult;
import mst.PrimMST;
import shortestpath.DijkstraSP;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class GraphTests {
    private static int assertions;
    private static int groups;

    public static void main(String[] args) {
        runAll();
    }

    public static void runAll() {
        assertions = 0;
        groups = 0;
        run("DSU union, size balancing and path compression", GraphTests::dsu);
        run("Known small MST", GraphTests::smallMst);
        run("Disconnected minimum spanning forest", GraphTests::disconnected);
        run("Multiple MSTs and duplicate weights", GraphTests::equalWeights);
        run("Single vertex in every algorithm", GraphTests::singleVertex);
        run("DAG ordering with isolated vertices", GraphTests::dag);
        run("Directed cycles and self-loops", GraphTests::cycles);
        run("Known SCC partition and membership", GraphTests::scc);
        run("Shortest distances, paths and unreachable vertices", GraphTests::shortestPaths);
        run("Zero weights, parallel edges and stale queue entries", GraphTests::zeroWeights);
        run("Negative weights and invalid inputs", GraphTests::invalidInputs);
        run("Fixed seeds, exact edge counts and generated graph properties", GraphTests::generators);
        run("Repeated calls and unchanged input graph", GraphTests::repeatCalls);
        run("Random MSTs against exhaustive search", GraphTests::randomMsts);
        run("Random SCCs and Dijkstra against Floyd-Warshall", GraphTests::randomDirected);
        run("Random DAGs with shuffled vertex labels", GraphTests::randomDags);
        run("Long directed chain without recursive DFS", GraphTests::longChain);
        System.out.println("All " + groups + " test groups passed (" + assertions + " assertions).");
    }

    private static void run(String name, Runnable test) {
        test.run();
        groups++;
        System.out.println("PASS: " + name);
    }

    private static void require(boolean condition, String message) {
        assertions++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void equal(double actual, double expected) {
        require(Double.compare(actual, expected) == 0, "Expected " + expected + ", got " + actual);
    }

    private static Graph graph(int vertices, boolean directed, double[][] edges) {
        Graph graph = new Graph(vertices, directed);
        for (double[] edge : edges) {
            graph.addEdge((int) edge[0], (int) edge[1], edge[2]);
        }
        return graph;
    }

    private static void dsu() {
        DisjointSetUnion dsu = new DisjointSetUnion(8);
        for (int width = 1; width < 8; width *= 2) {
            for (int i = 0; i < 8; i += 2 * width) {
                require(dsu.union(i, i + width), "Merge should succeed.");
            }
        }
        require(dsu.components() == 1, "One component expected.");
        long before = dsu.metrics().parentTraversals;
        dsu.find(7);
        long first = dsu.metrics().parentTraversals - before;
        before = dsu.metrics().parentTraversals;
        dsu.find(7);
        require(dsu.metrics().parentTraversals - before < first, "Compression should shorten the path.");
        require(!dsu.union(0, 7), "Repeated merge should fail.");
        for (int i = 0; i < 8; i++) {
            require(dsu.find(i) == dsu.find(0), "Roots should match.");
        }
    }

    private static void verifyMst(Graph graph, double expected, int components) {
        MstResult first = new KruskalMST().compute(graph);
        MstResult second = new PrimMST().compute(graph);
        equal(first.weight(), expected);
        equal(second.weight(), expected);
        for (MstResult result : List.of(first, second)) {
            require(result.components() == components, "Wrong component count.");
            require(result.connected() == (components == 1), "Wrong connectivity flag.");
            require(result.edges().size() == graph.vertices() - components, "Wrong forest size.");
            DisjointSetUnion dsu = new DisjointSetUnion(graph.vertices());
            double total = 0;
            for (Edge edge : result.edges()) {
                require(dsu.union(edge.source(), edge.destination()), "Forest contains a cycle.");
                boolean present = graph.edges().stream().anyMatch(original ->
                        original.weight() == edge.weight()
                        && ((original.source() == edge.source() && original.destination() == edge.destination())
                        || (original.source() == edge.destination() && original.destination() == edge.source())));
                require(present, "Selected edge is missing from input.");
                total += edge.weight();
            }
            equal(total, result.weight());
        }
    }

    private static void smallMst() {
        verifyMst(graph(4, false, new double[][]{
                {0, 1, 1}, {1, 2, 2}, {2, 3, 1}, {0, 3, 4}, {0, 2, 3}}), 4, 1);
    }

    private static void disconnected() {
        verifyMst(graph(6, false, new double[][]{
                {0, 1, 2}, {1, 2, 1}, {0, 2, 8}, {3, 4, 4}}), 7, 3);
        verifyMst(new Graph(4, false), 0, 4);
    }

    private static void equalWeights() {
        verifyMst(graph(4, false, new double[][]{
                {0, 1, 1}, {1, 2, 1}, {2, 3, 1}, {3, 0, 1}, {0, 2, 1}, {0, 1, 1}}), 3, 1);
        verifyMst(graph(3, false, new double[][]{
                {0, 1, -2}, {1, 2, 0}, {0, 2, 3}, {1, 1, -10}}), -2, 1);
    }

    private static void singleVertex() {
        verifyMst(new Graph(1, false), 0, 1);
        Graph graph = new Graph(1, true);
        validateOrder(graph, new TopologicalSort().compute(graph));
        require(new KosarajuSCC().compute(graph).components().equals(List.of(List.of(0))), "Wrong singleton SCC.");
        DijkstraSP.Result result = new DijkstraSP().compute(graph, 0);
        equal(result.distanceTo(0), 0);
        require(result.pathTo(0).equals(List.of(0)), "Wrong singleton path.");
    }

    private static void dag() {
        Graph graph = graph(6, true, new double[][]{{0, 2, 1}, {1, 2, 1}, {2, 4, 1}, {3, 4, 1}});
        validateOrder(graph, new TopologicalSort().compute(graph));
        require(new KosarajuSCC().compute(graph).components().size() == 6, "Every DAG SCC is a singleton.");
    }

    public static void validateOrder(Graph graph, TopologicalSort.Result result) {
        require(!result.hasCycle(), "DAG was rejected.");
        require(result.order().size() == graph.vertices(), "Missing ordered vertices.");
        int[] position = new int[graph.vertices()];
        Arrays.fill(position, -1);
        for (int i = 0; i < result.order().size(); i++) {
            int vertex = result.order().get(i);
            require(position[vertex] == -1, "Repeated ordered vertex.");
            position[vertex] = i;
        }
        for (Edge edge : graph.edges()) {
            require(position[edge.source()] < position[edge.destination()], "Invalid topological edge order.");
        }
    }

    private static void cycles() {
        Graph graph = graph(5, true, new double[][]{{0, 1, 1}, {1, 2, 1}, {2, 1, 1}, {3, 4, 1}});
        TopologicalSort.Result result = new TopologicalSort().compute(graph);
        require(result.hasCycle() && result.order().isEmpty(), "Cycle should have no valid full order.");
        Graph loop = graph(1, true, new double[][]{{0, 0, 0}});
        require(new TopologicalSort().compute(loop).hasCycle(), "Self-loop is a cycle.");
    }

    private static void scc() {
        Graph graph = graph(7, true, new double[][]{
                {0, 1, 1}, {1, 2, 1}, {2, 0, 1}, {2, 3, 1}, {3, 4, 1}, {4, 3, 1}, {4, 5, 1}});
        Set<Set<Integer>> expected = Set.of(Set.of(0, 1, 2), Set.of(3, 4), Set.of(5), Set.of(6));
        Set<Set<Integer>> actual = new HashSet<>();
        for (List<Integer> component : new KosarajuSCC().compute(graph).components()) {
            actual.add(new HashSet<>(component));
        }
        require(actual.equals(expected), "Wrong SCC partition.");
        verifyScc(graph, floyd(graph), new KosarajuSCC().compute(graph));
    }

    private static void shortestPaths() {
        Graph graph = graph(6, true, new double[][]{
                {0, 1, 4}, {0, 2, 1}, {2, 1, 2}, {1, 3, 1}, {2, 3, 5}, {3, 4, 3}});
        DijkstraSP.Result result = new DijkstraSP().compute(graph, 0);
        double[] expected = {0, 3, 1, 4, 7, Double.POSITIVE_INFINITY};
        for (int i = 0; i < expected.length; i++) {
            equal(result.distanceTo(i), expected[i]);
        }
        require(result.pathTo(4).equals(List.of(0, 2, 1, 3, 4)), "Wrong shortest path.");
        require(result.pathTo(5).isEmpty(), "Unreachable path should be empty.");
        equal(new DijkstraSP().compute(graph, 4).distanceTo(0), Double.POSITIVE_INFINITY);
    }

    private static void zeroWeights() {
        Graph graph = graph(4, true, new double[][]{
                {0, 1, 8}, {0, 1, 5}, {0, 2, 0}, {2, 1, 0}, {1, 2, 0}, {1, 3, 2}, {0, 0, 0}});
        verifyShortest(graph, floyd(graph));
    }

    private static void throwsArgument(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            require(true, "Expected rejection.");
            return;
        }
        require(false, "Invalid input should be rejected.");
    }

    private static void invalidInputs() {
        Graph negative = graph(4, true, new double[][]{{0, 1, 1}, {2, 3, -1}});
        throwsArgument(() -> new DijkstraSP().compute(negative, 0));
        throwsArgument(() -> new DijkstraSP().compute(negative, 4));
        throwsArgument(() -> new Graph(0, false));
        throwsArgument(() -> new Graph(2, false).addEdge(0, 2, 1));
        throwsArgument(() -> new Graph(2, false).addEdge(0, 1, Double.NaN));
        throwsArgument(() -> new Graph(2, false).addEdge(0, 1, Double.POSITIVE_INFINITY));
        throwsArgument(() -> new KruskalMST().compute(new Graph(2, true)));
        throwsArgument(() -> new PrimMST().compute(new Graph(2, true)));
        throwsArgument(() -> new TopologicalSort().compute(new Graph(2, false)));
        throwsArgument(() -> new KosarajuSCC().compute(new Graph(2, false)));
        throwsArgument(() -> GraphGenerator.dag(3, 4, 42));
    }

    private static void generators() {
        int[][] sizes = {{10, 30}, {100, 500}, {1000, 5000}};
        for (int[] size : sizes) {
            Graph first = GraphGenerator.connectedUndirected(size[0], size[1], 42);
            require(first.edges().equals(GraphGenerator.connectedUndirected(size[0], size[1], 42).edges()), "Seed mismatch.");
            for (Graph graph : List.of(first, GraphGenerator.dag(size[0], size[1], 43),
                    GraphGenerator.cyclicBlocks(size[0], size[1], 44))) {
                require(graph.edgeCount() == size[1], "Wrong generated edge count.");
                Set<Long> seen = new HashSet<>();
                for (Edge edge : graph.edges()) {
                    require(edge.source() != edge.destination(), "Generated self-loop.");
                    require(seen.add((long) edge.source() * graph.vertices() + edge.destination()), "Generated parallel edge.");
                    require(edge.weight() >= 1 && edge.weight() <= 100, "Wrong weight range.");
                }
            }
            require(new KruskalMST().compute(first).connected(), "Generated MST graph disconnected.");
            validateOrder(GraphGenerator.dag(size[0], size[1], 43),
                    new TopologicalSort().compute(GraphGenerator.dag(size[0], size[1], 43)));
            Graph cyclic = GraphGenerator.cyclicBlocks(size[0], size[1], 44);
            require(new TopologicalSort().compute(cyclic).hasCycle(), "Generated cycle missing.");
            KosarajuSCC.Result scc = new KosarajuSCC().compute(cyclic);
            require(scc.components().size() == size[0] / 5, "Wrong generated SCC count.");
            require(scc.metrics().edgeExaminations == 3L * size[1], "SCC must examine every edge three times.");
        }
    }

    private static void repeatCalls() {
        Graph graph = GraphGenerator.connectedUndirected(10, 30, 42);
        List<Edge> before = List.copyOf(graph.edges());
        KruskalMST kruskal = new KruskalMST();
        MstResult first = kruskal.compute(graph);
        MstResult second = kruskal.compute(graph);
        equal(first.weight(), second.weight());
        require(before.equals(graph.edges()), "Kruskal changed input order.");
        require(first.metrics().comparisons == second.metrics().comparisons, "Metrics did not reset.");
    }

    private static void randomMsts() {
        Random random = new Random(2026);
        for (int sample = 0; sample < 50; sample++) {
            int vertices = 2 + random.nextInt(5);
            int maximum = vertices * (vertices - 1) / 2;
            int edges = Math.min(maximum, vertices - 1 + random.nextInt(4));
            Graph graph = GraphGenerator.connectedUndirected(vertices, edges, random.nextLong());
            verifyMst(graph, exhaustiveMst(graph), 1);
        }
    }

    private static double exhaustiveMst(Graph graph) {
        double best = Double.POSITIVE_INFINITY;
        List<Edge> edges = graph.edges();
        for (int mask = 0; mask < (1 << edges.size()); mask++) {
            if (Integer.bitCount(mask) != graph.vertices() - 1) {
                continue;
            }
            DisjointSetUnion dsu = new DisjointSetUnion(graph.vertices());
            double weight = 0;
            for (int i = 0; i < edges.size(); i++) {
                if ((mask & (1 << i)) != 0) {
                    Edge edge = edges.get(i);
                    dsu.union(edge.source(), edge.destination());
                    weight += edge.weight();
                }
            }
            if (dsu.components() == 1) {
                best = Math.min(best, weight);
            }
        }
        return best;
    }

    private static double[][] floyd(Graph graph) {
        int vertices = graph.vertices();
        double[][] distances = new double[vertices][vertices];
        for (int i = 0; i < vertices; i++) {
            Arrays.fill(distances[i], Double.POSITIVE_INFINITY);
            distances[i][i] = 0;
            for (Edge edge : graph.neighbors(i)) {
                distances[i][edge.destination()] = Math.min(distances[i][edge.destination()], edge.weight());
            }
        }
        for (int middle = 0; middle < vertices; middle++) {
            for (int source = 0; source < vertices; source++) {
                for (int target = 0; target < vertices; target++) {
                    distances[source][target] = Math.min(distances[source][target],
                            distances[source][middle] + distances[middle][target]);
                }
            }
        }
        return distances;
    }

    private static void verifyScc(Graph graph, double[][] distances, KosarajuSCC.Result result) {
        int[] owner = new int[graph.vertices()];
        Arrays.fill(owner, -1);
        for (int group = 0; group < result.components().size(); group++) {
            require(!result.components().get(group).isEmpty(), "Empty SCC.");
            for (int vertex : result.components().get(group)) {
                require(owner[vertex] == -1, "Vertex belongs to two SCCs.");
                owner[vertex] = group;
            }
        }
        for (int a = 0; a < graph.vertices(); a++) {
            require(owner[a] != -1, "Vertex is missing from SCCs.");
            for (int b = 0; b < graph.vertices(); b++) {
                boolean mutual = Double.isFinite(distances[a][b]) && Double.isFinite(distances[b][a]);
                require((owner[a] == owner[b]) == mutual, "SCC membership disagrees with mutual reachability.");
            }
        }
    }

    private static void verifyShortest(Graph graph, double[][] expected) {
        for (int source = 0; source < graph.vertices(); source++) {
            DijkstraSP.Result result = new DijkstraSP().compute(graph, source);
            for (int target = 0; target < graph.vertices(); target++) {
                equal(result.distanceTo(target), expected[source][target]);
                List<Integer> path = result.pathTo(target);
                if (Double.isInfinite(expected[source][target])) {
                    require(path.isEmpty(), "Unreachable path should be empty.");
                    continue;
                }
                require(path.get(0) == source && path.get(path.size() - 1) == target, "Wrong path endpoints.");
                require(new HashSet<>(path).size() == path.size(), "Shortest path contains a cycle.");
                double sum = 0;
                for (int i = 1; i < path.size(); i++) {
                    int destination = path.get(i);
                    double minimum = graph.neighbors(path.get(i - 1)).stream()
                            .filter(edge -> edge.destination() == destination)
                            .mapToDouble(Edge::weight).min().orElse(Double.POSITIVE_INFINITY);
                    sum += minimum;
                }
                equal(sum, result.distanceTo(target));
            }
        }
    }

    private static void randomDirected() {
        Random random = new Random(731);
        for (int sample = 0; sample < 60; sample++) {
            int vertices = 1 + random.nextInt(8);
            Graph graph = new Graph(vertices, true);
            for (int a = 0; a < vertices; a++) {
                for (int b = 0; b < vertices; b++) {
                    if (random.nextDouble() < 0.25) {
                        graph.addEdge(a, b, random.nextInt(12));
                    }
                }
            }
            double[][] distances = floyd(graph);
            verifyScc(graph, distances, new KosarajuSCC().compute(graph));
            verifyShortest(graph, distances);
        }
        verifyShortest(GraphGenerator.connectedUndirected(7, 12, 13),
                floyd(GraphGenerator.connectedUndirected(7, 12, 13)));
    }

    private static void randomDags() {
        Random random = new Random(419);
        for (int sample = 0; sample < 40; sample++) {
            int vertices = 2 + random.nextInt(15);
            List<Integer> labels = new ArrayList<>();
            for (int i = 0; i < vertices; i++) {
                labels.add(i);
            }
            java.util.Collections.shuffle(labels, random);
            Graph graph = new Graph(vertices, true);
            for (int a = 0; a < vertices; a++) {
                for (int b = a + 1; b < vertices; b++) {
                    if (random.nextBoolean()) {
                        graph.addEdge(labels.get(a), labels.get(b), 1);
                    }
                }
            }
            validateOrder(graph, new TopologicalSort().compute(graph));
        }
    }

    private static void longChain() {
        Graph graph = new Graph(20000, true);
        for (int i = 1; i < graph.vertices(); i++) {
            graph.addEdge(i - 1, i, 1);
        }
        require(new KosarajuSCC().compute(graph).components().size() == graph.vertices(), "Wrong chain SCCs.");
        require(!new TopologicalSort().compute(graph).hasCycle(), "A chain is a DAG.");
    }
}
