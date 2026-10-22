package connectivity;

import java.util.*;

public class KosarajuSCC {
    private long operationCount;

    public List<List<Integer>> findSCCs(int vertices, List<List<Integer>> adj) {
        operationCount = 0;
        Stack<Integer> stack = new Stack<>();
        boolean[] visited = new boolean[vertices];

        for (int i = 0; i < vertices; i++) {
            if (!visited[i]) {
                fillOrder(i, visited, stack, adj);
            }
        }

        List<List<Integer>> transpose = getTranspose(vertices, adj);

        Arrays.fill(visited, false);
        List<List<Integer>> sccs = new ArrayList<>();

        while (!stack.isEmpty()) {
            int v = stack.pop();
            if (!visited[v]) {
                List<Integer> scc = new ArrayList<>();
                DFSUtil(v, visited, transpose, scc);
                sccs.add(scc);
            }
        }
        return sccs;
    }

    private void fillOrder(int v, boolean[] visited, Stack<Integer> stack, List<List<Integer>> adj) {
        visited[v] = true;
        operationCount++;
        for (int neighbor : adj.get(v)) {
            if (!visited[neighbor]) {
                fillOrder(neighbor, visited, stack, adj);
            }
        }
        stack.push(v);
    }

    private List<List<Integer>> getTranspose(int vertices, List<List<Integer>> adj) {
        List<List<Integer>> transpose = new ArrayList<>();
        for (int i = 0; i < vertices; i++) transpose.add(new ArrayList<>());
        for (int i = 0; i < vertices; i++) {
            for (int neighbor : adj.get(i)) {
                transpose.get(neighbor).add(i);
                operationCount++;
            }
        }
        return transpose;
    }

    private void DFSUtil(int v, boolean[] visited, List<List<Integer>> transpose, List<Integer> scc) {
        visited[v] = true;
        scc.add(v);
        operationCount++;
        for (int neighbor : transpose.get(v)) {
            if (!visited[neighbor]) {
                DFSUtil(neighbor, visited, transpose, scc);
            }
        }
    }

    public long getOperationCount() { return operationCount; }
}