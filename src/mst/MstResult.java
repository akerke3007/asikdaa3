package mst;

import graph.Edge;
import graph.Metrics;
import java.util.List;

public record MstResult(List<Edge> edges, double weight, int components, Metrics metrics) {
    public MstResult {
        edges = List.copyOf(edges);
    }

    public boolean connected() {
        return components == 1;
    }
}
