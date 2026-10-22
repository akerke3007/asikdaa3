package mst;

import graph.Edge;
import java.util.*;

public class PrimMST {
    private double totalWeight;
    private List<Edge> mstEdges;
    private long edgeExaminations;
    private boolean isConnected;

    public void compute(int vertices, List<List<Edge>> adjList) {
        mstEdges = new ArrayList<>();
        totalWeight = 0.0;
        edgeExaminations = 0;

        if (vertices == 0) {
            isConnected = true;
            return;
        }

        boolean[] visited = new boolean[vertices];
        PriorityQueue<Edge> pq = new PriorityQueue<>();

        visited[0] = true;
        pq.addAll(adjList.get(0));

        int visitedCount = 1;

        while (!pq.isEmpty()) {
            Edge edge = pq.poll();
            edgeExaminations++;

            int dest = edge.getDestination();
            int src = edge.getSource();

            int u = visited[src] ? dest : src;
            if (visited[u]) continue;

            visited[u] = true;
            visitedCount++;
            mstEdges.add(edge);
            totalWeight += edge.getWeight();

            for (Edge nextEdge : adjList.get(u)) {
                int nextDest = nextEdge.getDestination();
                int nextSrc = nextEdge.getSource();
                int v = visited[nextSrc] ? nextDest : nextSrc;
                if (!visited[v]) {
                    pq.add(nextEdge);
                }
            }
        }

        isConnected = (visitedCount == vertices);
    }

    public double getTotalWeight() { return totalWeight; }
    public List<Edge> getMstEdges() { return mstEdges; }
    public long getEdgeExaminations() { return edgeExaminations; }
    public boolean isConnected() { return isConnected; }
}