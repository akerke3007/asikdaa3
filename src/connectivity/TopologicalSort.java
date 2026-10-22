package connectivity;

import java.util.*;

public class TopologicalSort {
    private long operationCount;

    public List<Integer> sort(int vertices, List<List<Integer>> adj) {
        operationCount = 0;
        int[] inDegree = new int[vertices];

        for (int u = 0; u < vertices; u++) {
            for (int v : adj.get(u)) {
                inDegree[v]++;
                operationCount++;
            }
        }

        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < vertices; i++) {
            if (inDegree[i] == 0) {
                queue.add(i);
            }
        }

        List<Integer> topologicalOrder = new ArrayList<>();
        while (!queue.isEmpty()) {
            int u = queue.poll();
            topologicalOrder.add(u);
            operationCount++;

            for (int v : adj.get(u)) {
                operationCount++;
                if (--inDegree[v] == 0) {
                    queue.add(v);
                }
            }
        }

        if (topologicalOrder.size() != vertices) {
            throw new IllegalStateException("Graph contains a cycle; topological ordering is impossible.");
        }

        return topologicalOrder;
    }

    public long getOperationCount() { return operationCount; }
}