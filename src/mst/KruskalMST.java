package mst;

import dsu.DisjointSetUnion;
import graph.Edge;
import java.util.*;

public class KruskalMST {
    private double totalWeight;
    private List<Edge> mstEdges;
    private long edgeExaminations;
    private boolean isConnected;

    public void compute(int vertices, List<Edge> edges) {
        Collections.sort(edges);
        DisjointSetUnion dsu = new DisjointSetUnion(vertices);
        mstEdges = new ArrayList<>();
        totalWeight = 0.0;
        edgeExaminations = 0;

        for (Edge edge : edges) {
            edgeExaminations++;
            if (dsu.union(edge.getSource(), edge.getDestination())) {
                mstEdges.add(edge);
                totalWeight += edge.getWeight();
                if (mstEdges.size() == vertices - 1) break;
            }
        }

        isConnected = (vertices <= 1) || (mstEdges.size() == vertices - 1);
    }

    public double getTotalWeight() { return totalWeight; }
    public List<Edge> getMstEdges() { return mstEdges; }
    public long getEdgeExaminations() { return edgeExaminations; }
    public boolean isConnected() { return isConnected; }
}