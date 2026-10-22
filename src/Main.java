import connectivity.KosarajuSCC;
import connectivity.TopologicalSort;
import graph.Graph;
import mst.KruskalMST;
import mst.MstResult;
import mst.PrimMST;
import shortestpath.DijkstraSP;

public final class Main {
    public static void main(String[] args) throws java.io.IOException {
        String command = args.length == 0 ? "demo" : args[0];
        switch (command) {
            case "demo" -> demo(args.length > 1 ? Integer.parseInt(args[1]) : 0);
            case "test" -> testing.GraphTests.runAll();
            case "benchmark" -> benchmark.Benchmark.run();
            default -> throw new IllegalArgumentException("Use: Main [demo [source 0-5] | test | benchmark]");
        }
    }

    private static void demo(int source) {
        System.out.println("Assignment 3 - Smart City Infrastructure and Route Planning");
        String[] names = {"Central", "University", "Hospital", "Station", "Park", "Airport"};
        Graph city = new Graph(6, false);
        city.addEdge(0, 1, 4);
        city.addEdge(0, 2, 3);
        city.addEdge(1, 2, 1);
        city.addEdge(1, 3, 2);
        city.addEdge(2, 3, 4);
        city.addEdge(3, 4, 2);
        city.addEdge(4, 5, 3);
        city.addEdge(3, 5, 6);
        city.addEdge(0, 5, 10);
        MstResult kruskal = new KruskalMST().compute(city);
        MstResult prim = new PrimMST().compute(city);
        System.out.println("Districts: " + java.util.Arrays.toString(names));
        System.out.println("Kruskal cost: " + kruskal.weight());
        System.out.println("Prim cost: " + prim.weight());
        System.out.println("Connected: " + kruskal.connected());
        System.out.println("Costs match: " + (kruskal.weight() == prim.weight()));
        System.out.println("Selected roads: " + kruskal.edges());

        Graph services = new Graph(6, true);
        services.addEdge(0, 1, 1);
        services.addEdge(0, 2, 1);
        services.addEdge(1, 3, 1);
        services.addEdge(2, 3, 1);
        services.addEdge(3, 4, 1);
        System.out.println("Service order: " + new TopologicalSort().compute(services).order());
        services.addEdge(4, 0, 1);
        System.out.println("Cycle after adding 4 -> 0: "
                + new TopologicalSort().compute(services).hasCycle());
        System.out.println("Strongly connected components: "
                + new KosarajuSCC().compute(services).components());

        Graph routes = new Graph(6, true);
        routes.addEdge(0, 1, 4);
        routes.addEdge(0, 2, 1);
        routes.addEdge(2, 1, 2);
        routes.addEdge(1, 3, 1);
        routes.addEdge(2, 3, 5);
        routes.addEdge(3, 4, 3);
        DijkstraSP.Result paths = new DijkstraSP().compute(routes, source);
        System.out.println("Routes from " + names[source] + " (district " + source + "):");
        for (int vertex = 0; vertex < routes.vertices(); vertex++) {
            if (Double.isInfinite(paths.distanceTo(vertex))) {
                System.out.println(names[vertex] + ": unreachable, path []");
            } else {
                System.out.println(names[vertex] + ": distance " + paths.distanceTo(vertex)
                        + ", path " + paths.pathTo(vertex));
            }
        }
    }
}
