package dsu;

public class DisjointSetUnion {
    private final int[] parent;
    private final int[] rank;
    private final int[] size;
    private int components;
    private long findCalls;
    private long unionCalls;
    private long parentAccesses;

    public DisjointSetUnion(int n) {
        this.parent = new int[n];
        this.rank = new int[n];
        this.size = new int[n];
        this.components = n;
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            rank[i] = 0;
            size[i] = 1;
        }
    }

    public int find(int x) {
        findCalls++;
        parentAccesses++;
        if (parent[x] != x) {
            parent[x] = find(parent[x]);
        }
        return parent[x];
    }

    public boolean union(int x, int y) {
        unionCalls++;
        int rootX = find(x);
        int rootY = find(y);
        if (rootX == rootY) {
            return false;
        }
        if (rank[rootX] < rank[rootY]) {
            parent[rootX] = rootY;
            size[rootY] += size[rootX];
        } else if (rank[rootX] > rank[rootY]) {
            parent[rootY] = rootX;
            size[rootX] += size[rootY];
        } else {
            parent[rootY] = rootX;
            size[rootX] += size[rootY];
            rank[rootX]++;
        }
        parentAccesses++;
        components--;
        return true;
    }

    public boolean isConnected(int x, int y) {
        return find(x) == find(y);
    }

    public int getComponentSize(int x) {
        return size[find(x)];
    }

    public int getComponentsCount() {
        return components;
    }

    public long getFindCalls() {
        return findCalls;
    }

    public long getUnionCalls() {
        return unionCalls;
    }

    public long getParentAccesses() {
        return parentAccesses;
    }

    public long getTotalOperations() {
        return findCalls + unionCalls + parentAccesses;
    }
}
