package mst;

import dsu.DisjointSetUnion;
import graph.Edge;
import graph.Graph;
import graph.Metrics;
import java.util.ArrayList;
import java.util.List;

public final class KruskalMST {
    public MstResult compute(Graph graph) {
        if (graph.directed()) {
            throw new IllegalArgumentException("MST needs an undirected graph.");
        }
        Metrics metrics = new Metrics();
        List<Edge> sorted = new ArrayList<>(graph.edges());
        sorted.sort((first, second) -> {
            metrics.comparisons++;
            return Double.compare(first.weight(), second.weight());
        });
        DisjointSetUnion dsu = new DisjointSetUnion(graph.vertices(), metrics);
        List<Edge> selected = new ArrayList<>();
        double weight = 0;
        for (Edge edge : sorted) {
            metrics.edgeExaminations++;
            if (dsu.union(edge.source(), edge.destination())) {
                selected.add(edge);
                weight += edge.weight();
                if (selected.size() == graph.vertices() - 1) {
                    break;
                }
            }
        }
        return new MstResult(selected, weight, dsu.components(), metrics);
    }
}
