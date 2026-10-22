package benchmark;

import connectivity.KosarajuSCC;
import connectivity.TopologicalSort;
import graph.Edge;
import graph.Graph;
import graph.GraphGenerator;
import graph.Metrics;
import mst.KruskalMST;
import mst.MstResult;
import mst.PrimMST;
import shortestpath.DijkstraSP;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public final class Benchmark {
    private static final int WARMUP_RUNS = 100;
    private static final int MEASURED_RUNS = 5;
    private static volatile Object sink;

    private record Measurement(Metrics metrics, String outcome) {}
    private record Experiment(String algorithm, String family, String size,
                              Graph graph, long seed, Supplier<?> task) {}
    private record Row(Experiment experiment, int trial, long nanoseconds, Measurement result) {}

    public static void main(String[] args) throws IOException {
        run();
    }

    public static void run() throws IOException {
        Locale.setDefault(Locale.US);
        Path directory = Path.of("results", "tables");
        Files.createDirectories(directory);
        List<Experiment> experiments = experiments();
        for (int warmup = 0; warmup < WARMUP_RUNS; warmup++) {
            for (int index = 0; index < experiments.size(); index++) {
                Experiment experiment = experiments.get((index + warmup) % experiments.size());
                sink = experiment.task().get();
            }
        }
        List<Row> rows = new ArrayList<>();
        for (int trial = 1; trial <= MEASURED_RUNS; trial++) {
            for (int index = 0; index < experiments.size(); index++) {
                Experiment experiment = experiments.get((index + trial - 1) % experiments.size());
                long start = System.nanoTime();
                Object result = experiment.task().get();
                long elapsed = System.nanoTime() - start;
                sink = result;
                rows.add(new Row(experiment, trial, elapsed, summarize(result, experiment.graph().vertices())));
            }
        }
        verifyResults(rows);
        writeRaw(directory.resolve("raw_runs.csv"), rows);
        writeAverages(directory.resolve("averages.csv"), experiments, rows);
        writeEnvironment(directory.resolve("environment.txt"), experiments);
        System.out.println("Saved " + rows.size() + " measured runs and " + experiments.size() + " averages.");
        System.out.println("Warm-ups: " + WARMUP_RUNS + " per case. Measured runs: " + MEASURED_RUNS + " per case.");
        System.out.println("MST equality, DAG order, cycle detection and SCC membership checks passed.");
    }

    private static List<Experiment> experiments() {
        List<Experiment> experiments = new ArrayList<>();
        int[][] sizes = {{10, 30}, {100, 500}, {1000, 5000}};
        String[] labels = {"Small", "Medium", "Large"};
        for (int i = 0; i < sizes.length; i++) {
            Graph undirected = GraphGenerator.connectedUndirected(sizes[i][0], sizes[i][1], 42);
            Graph dag = GraphGenerator.dag(sizes[i][0], sizes[i][1], 43);
            Graph cyclic = GraphGenerator.cyclicBlocks(sizes[i][0], sizes[i][1], 44);
            String size = labels[i];
            experiments.add(new Experiment("Kruskal", "Undirected", size, undirected, 42,
                    () -> new KruskalMST().compute(undirected)));
            experiments.add(new Experiment("Prim", "Undirected", size, undirected, 42,
                    () -> new PrimMST().compute(undirected)));
            experiments.add(new Experiment("Kosaraju", "DAG", size, dag, 43,
                    () -> new KosarajuSCC().compute(dag)));
            experiments.add(new Experiment("Kosaraju", "Cyclic", size, cyclic, 44,
                    () -> new KosarajuSCC().compute(cyclic)));
            experiments.add(new Experiment("Topological", "DAG", size, dag, 43,
                    () -> new TopologicalSort().compute(dag)));
            experiments.add(new Experiment("Topological", "Cyclic", size, cyclic, 44,
                    () -> new TopologicalSort().compute(cyclic)));
            experiments.add(new Experiment("Dijkstra", "Undirected", size, undirected, 42,
                    () -> new DijkstraSP().compute(undirected, 0)));
            experiments.add(new Experiment("Dijkstra", "Cyclic", size, cyclic, 44,
                    () -> new DijkstraSP().compute(cyclic, 0)));
        }
        return experiments;
    }

    private static Measurement summarize(Object result, int vertices) {
        if (result instanceof MstResult mst) {
            return new Measurement(mst.metrics(), "weight=" + mst.weight() + ";components=" + mst.components());
        }
        if (result instanceof KosarajuSCC.Result scc) {
            return new Measurement(scc.metrics(), "components=" + scc.components().size());
        }
        if (result instanceof TopologicalSort.Result topological) {
            return new Measurement(topological.metrics(), "cycle=" + topological.hasCycle()
                    + ";ordered=" + topological.order().size());
        }
        if (result instanceof DijkstraSP.Result shortest) {
            return shortest(shortest, vertices);
        }
        throw new IllegalArgumentException("Unknown algorithm result.");
    }

    private static Measurement shortest(DijkstraSP.Result result, int vertices) {
        int reachable = 0;
        double sum = 0;
        for (int vertex = 0; vertex < vertices; vertex++) {
            if (Double.isFinite(result.distanceTo(vertex))) {
                reachable++;
                sum += result.distanceTo(vertex);
            }
        }
        return new Measurement(result.metrics(), "reachable=" + reachable + ";distanceSum=" + sum);
    }

    private static void verifyResults(List<Row> rows) {
        for (Row row : rows) {
            Experiment experiment = row.experiment();
            Graph graph = experiment.graph();
            if (experiment.algorithm().equals("Kruskal")) {
                MstResult first = new KruskalMST().compute(graph);
                MstResult second = new PrimMST().compute(graph);
                check(first.connected() && second.connected() && first.weight() == second.weight(), "MST mismatch.");
                check(first.edges().size() == graph.vertices() - 1, "Wrong MST edge count.");
            } else if (experiment.algorithm().equals("Topological")) {
                TopologicalSort.Result result = new TopologicalSort().compute(graph);
                if (experiment.family().equals("DAG")) {
                    check(!result.hasCycle() && result.order().size() == graph.vertices(), "DAG was rejected.");
                    int[] position = new int[graph.vertices()];
                    for (int i = 0; i < result.order().size(); i++) {
                        position[result.order().get(i)] = i;
                    }
                    for (Edge edge : graph.edges()) {
                        check(position[edge.source()] < position[edge.destination()], "Invalid order.");
                    }
                } else {
                    check(result.hasCycle() && result.order().isEmpty(), "Cycle not detected.");
                }
            } else if (experiment.algorithm().equals("Kosaraju")) {
                KosarajuSCC.Result result = new KosarajuSCC().compute(graph);
                boolean[] seen = new boolean[graph.vertices()];
                int count = 0;
                for (List<Integer> component : result.components()) {
                    for (int vertex : component) {
                        check(!seen[vertex], "Repeated SCC vertex.");
                        seen[vertex] = true;
                        count++;
                    }
                }
                int expected = experiment.family().equals("DAG") ? graph.vertices() : graph.vertices() / 5;
                check(count == graph.vertices() && result.components().size() == expected, "Wrong SCC partition.");
            }
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static String counts(Metrics metrics) {
        return metrics.edgeExaminations + "," + metrics.comparisons + "," + metrics.findCalls
                + "," + metrics.unionCalls + "," + metrics.parentTraversals + "," + metrics.parentUpdates
                + "," + metrics.queueOffers + "," + metrics.queuePolls + "," + metrics.vertexVisits
                + "," + metrics.relaxations + "," + metrics.validationEdges;
    }

    private static String description(Experiment experiment) {
        return experiment.algorithm() + "," + experiment.family() + "," + experiment.size()
                + "," + experiment.graph().vertices() + "," + experiment.graph().edgeCount() + "," + experiment.seed();
    }

    private static void writeRaw(Path path, List<Row> rows) throws IOException {
        StringBuilder csv = new StringBuilder("algorithm,graph,size,vertices,edges,seed,trial,time_ns,"
                + "edge_examinations,comparisons,find_calls,union_calls,parent_traversals,parent_updates,"
                + "queue_offers,queue_polls,vertex_visits,relaxations,validation_edges,outcome\n");
        for (Row row : rows) {
            csv.append(description(row.experiment())).append(',').append(row.trial()).append(',')
                    .append(row.nanoseconds()).append(',').append(counts(row.result().metrics()))
                    .append(',').append(row.result().outcome()).append('\n');
        }
        Files.writeString(path, csv);
    }

    private static void writeAverages(Path path, List<Experiment> experiments, List<Row> rows) throws IOException {
        StringBuilder csv = new StringBuilder("algorithm,graph,size,vertices,edges,seed,runs,average_ns,min_ns,max_ns,stddev_ns,"
                + "edge_examinations,comparisons,find_calls,union_calls,parent_traversals,parent_updates,"
                + "queue_offers,queue_polls,vertex_visits,relaxations,validation_edges,outcome\n");
        for (Experiment experiment : experiments) {
            List<Row> matching = rows.stream().filter(row -> row.experiment() == experiment).toList();
            double average = matching.stream().mapToLong(Row::nanoseconds).average().orElseThrow();
            long minimum = matching.stream().mapToLong(Row::nanoseconds).min().orElseThrow();
            long maximum = matching.stream().mapToLong(Row::nanoseconds).max().orElseThrow();
            double variance = matching.stream().mapToDouble(row -> Math.pow(row.nanoseconds() - average, 2))
                    .sum() / (matching.size() - 1);
            String operations = counts(matching.get(0).result().metrics());
            String outcome = matching.get(0).result().outcome();
            for (Row row : matching) {
                check(counts(row.result().metrics()).equals(operations) && row.result().outcome().equals(outcome),
                        "Repeated runs must have identical operations and results.");
            }
            csv.append(description(experiment)).append(',').append(matching.size()).append(',')
                    .append(String.format(Locale.US, "%.1f,%d,%d,%.1f,", average, minimum, maximum, Math.sqrt(variance)))
                    .append(operations).append(',').append(outcome).append('\n');
        }
        Files.writeString(path, csv);
    }

    private static void writeEnvironment(Path path, List<Experiment> experiments) throws IOException {
        StringBuilder text = new StringBuilder();
        text.append("Recorded at (UTC): ").append(Instant.now()).append('\n');
        text.append("Java: ").append(System.getProperty("java.runtime.version")).append('\n');
        text.append("VM: ").append(System.getProperty("java.vm.name")).append('\n');
        text.append("OS: ").append(System.getProperty("os.name")).append(' ')
                .append(System.getProperty("os.version")).append(' ').append(System.getProperty("os.arch")).append('\n');
        text.append("Available processors: ").append(Runtime.getRuntime().availableProcessors()).append('\n');
        text.append("Maximum heap bytes: ").append(Runtime.getRuntime().maxMemory()).append('\n');
        Path cpuInfo = Path.of("/proc/cpuinfo");
        if (Files.exists(cpuInfo)) {
            String model = Files.readAllLines(cpuInfo).stream().filter(line -> line.startsWith("model name"))
                    .findFirst().orElse("model name: unavailable");
            text.append("CPU: ").append(model).append('\n');
        }
        text.append("Timer: System.nanoTime(); all algorithms are single-threaded\n");
        text.append("Warm-up runs per case: ").append(WARMUP_RUNS).append('\n');
        text.append("Measured runs per case: ").append(MEASURED_RUNS).append('\n');
        text.append("Sources for Dijkstra: 0; weights: integers from 1 to 100 stored as double\n");
        text.append("Graph fingerprints (SHA-256 of insertion-order Edge.toString() lines):\n");
        for (Experiment experiment : experiments) {
            if (experiment.algorithm().equals("Kruskal") || experiment.algorithm().equals("Kosaraju")) {
                text.append(experiment.size()).append(' ').append(experiment.family()).append(" seed=")
                        .append(experiment.seed()).append(' ').append(fingerprint(experiment.graph())).append('\n');
            }
        }
        Files.writeString(path, text);
    }

    private static String fingerprint(Graph graph) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Edge edge : graph.edges()) {
                digest.update((edge.toString() + "\n").getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
