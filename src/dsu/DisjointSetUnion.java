package dsu;

public class DisjointSetUnion {
    private final int[] parent;
    private final int[] rank;
    private long operationCount;

    public DisjointSetUnion(int n) {
        parent = new int[n];
        rank = new int[n];
        operationCount = 0;
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            rank[i] = 0;
        }
    }

    public int find(int i) {
        operationCount++;
        if (parent[i] == i) {
            return i;
        }
        return parent[i] = find(parent[i]);
    }

    public boolean union(int i, int j) {
        operationCount++;
        int rootI = find(i);
        int rootJ = find(j);

        if (rootI != rootJ) {
            if (rank[rootI] < rank[rootJ]) {
                parent[rootI] = rootJ;
            } else if (rank[rootI] > rank[rootJ]) {
                parent[rootJ] = rootI;
            } else {
                parent[rootJ] = rootI;
                rank[rootI]++;
            }
            return true;
        }
        return false;
    }

    public long getOperationCount() {
        return operationCount;
    }

    public void resetOperations() {
        operationCount = 0;
    }
}