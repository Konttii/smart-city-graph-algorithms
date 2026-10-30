package shortestpath;

import graph.Edge;
import graph.Graph;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

public class Dijkstra {

    public static class Result {
        private final int source;
        private final long[] distances;
        private final int[] predecessors;
        private final long executionTimeNanos;
        private final long edgeRelaxations;
        private final long comparisons;
        private final long heapOperations;

        public Result(int source, long[] distances, int[] predecessors, long executionTimeNanos,
                      long edgeRelaxations, long comparisons, long heapOperations) {
            this.source = source;
            this.distances = distances;
            this.predecessors = predecessors;
            this.executionTimeNanos = executionTimeNanos;
            this.edgeRelaxations = edgeRelaxations;
            this.comparisons = comparisons;
            this.heapOperations = heapOperations;
        }

        public int getSource() {
            return source;
        }

        public long getDistance(int target) {
            return distances[target];
        }

        public boolean isReachable(int target) {
            return distances[target] != Long.MAX_VALUE;
        }

        public List<Integer> getPathTo(int target) {
            if (!isReachable(target)) {
                return Collections.emptyList();
            }
            List<Integer> path = new ArrayList<>();
            for (int cur = target; cur != -1; cur = predecessors[cur]) {
                path.add(cur);
            }
            Collections.reverse(path);
            return path;
        }

        public long[] getDistances() {
            return Arrays.copyOf(distances, distances.length);
        }

        public long getExecutionTimeNanos() {
            return executionTimeNanos;
        }

        public long getEdgeRelaxations() {
            return edgeRelaxations;
        }

        public long getComparisons() {
            return comparisons;
        }

        public long getHeapOperations() {
            return heapOperations;
        }

        public long getTotalOperations() {
            return edgeRelaxations + comparisons + heapOperations;
        }
    }

    private static class NodeEntry {
        final int vertex;
        final long dist;

        NodeEntry(int vertex, long dist) {
            this.vertex = vertex;
            this.dist = dist;
        }
    }

    public static Result shortestPaths(Graph graph, int source) {
        long startTime = System.nanoTime();
        int n = graph.getVertices();
        if (source < 0 || source >= n) {
            throw new IllegalArgumentException("Source vertex out of range");
        }

        for (Edge e : graph.getEdges()) {
            if (e.getWeight() < 0) {
                throw new IllegalArgumentException("Dijkstra requires non-negative edge weights");
            }
        }

        long[] dist = new long[n];
        int[] prev = new int[n];
        Arrays.fill(dist, Long.MAX_VALUE);
        Arrays.fill(prev, -1);

        long[] comparisons = new long[]{0};
        long edgeRelaxations = 0;
        long heapOps = 0;

        PriorityQueue<NodeEntry> pq = new PriorityQueue<>((a, b) -> {
            comparisons[0]++;
            return Long.compare(a.dist, b.dist);
        });

        dist[source] = 0;
        pq.offer(new NodeEntry(source, 0));
        heapOps++;

        while (!pq.isEmpty()) {
            NodeEntry current = pq.poll();
            heapOps++;
            int u = current.vertex;

            comparisons[0]++;
            if (current.dist > dist[u]) {
                continue;
            }

            for (Edge e : graph.getNeighbors(u)) {
                edgeRelaxations++;
                int v = e.getTo();
                long newDist = dist[u] + e.getWeight();
                comparisons[0]++;
                if (newDist < dist[v]) {
                    dist[v] = newDist;
                    prev[v] = u;
                    pq.offer(new NodeEntry(v, newDist));
                    heapOps++;
                }
            }
        }

        long elapsed = System.nanoTime() - startTime;
        return new Result(source, dist, prev, elapsed, edgeRelaxations, comparisons[0], heapOps);
    }
}
