import graph.Edge;
import dsu.DisjointSetUnion;
import mst.KruskalMST;
import mst.PrimMST;
import shortestpath.DijkstraSP;
import connectivity.TopologicalSort;
import connectivity.KosarajuSCC;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== smart city infrastructure & route planning ===");

        int vertices = 4;
        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge(0, 1, 1.0));
        edges.add(new Edge(1, 2, 2.0));
        edges.add(new Edge(2, 3, 1.0));
        edges.add(new Edge(0, 3, 4.0));
        edges.add(new Edge(0, 2, 3.0));

        KruskalMST kruskal = new KruskalMST();
        kruskal.compute(vertices, edges);
        System.out.println("Kruskal MST Weight: " + kruskal.getTotalWeight());

        List<List<Edge>> adjList = new ArrayList<>();
        for (int i = 0; i < vertices; i++) adjList.add(new ArrayList<>());
        for (Edge e : edges) {
            adjList.get(e.getSource()).add(e);
            adjList.get(e.getDestination()).add(new Edge(e.getDestination(), e.getSource(), e.getWeight()));
        }

        PrimMST prim = new PrimMST();
        prim.compute(vertices, adjList);
        System.out.println("Prim MST Weight: " + prim.getTotalWeight());

        List<List<Integer>> dagAdj = new ArrayList<>();
        for (int i = 0; i < 3; i++) dagAdj.add(new ArrayList<>());
        dagAdj.get(0).add(1);
        dagAdj.get(1).add(2);

        TopologicalSort topo = new TopologicalSort();
        List<Integer> topoOrder = topo.sort(3, dagAdj);
        System.out.println("Topological Order: " + topoOrder);

        KosarajuSCC scc = new KosarajuSCC();
        List<List<Integer>> sccResult = scc.findSCCs(3, dagAdj);
        System.out.println("SCCs count: " + sccResult.size());

        DijkstraSP dijkstra = new DijkstraSP();
        dijkstra.compute(vertices, adjList, 0);
        System.out.println("Dijkstra dist from 0 to 3: " + dijkstra.getDistTo()[3]);

        System.out.println("all works successfully");
    }
}