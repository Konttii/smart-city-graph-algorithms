package connectivity;

import graph.Edge;
import graph.Graph;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public class TopologicalSort {

    public static class Result {
        private final boolean isDAG;
        private final List<Integer> order;
        private final long executionTimeNanos;
        private final long vertexVisits;
        private final long edgeExaminations;

        public Result(boolean isDAG, List<Integer> order, long executionTimeNanos, long vertexVisits, long edgeExaminations) {
            this.isDAG = isDAG;
            this.order = Collections.unmodifiableList(order);
            this.executionTimeNanos = executionTimeNanos;
            this.vertexVisits = vertexVisits;
            this.edgeExaminations = edgeExaminations;
        }

        public boolean isDAG() {
            return isDAG;
        }

        public List<Integer> getOrder() {
            return order;
        }

        public long getExecutionTimeNanos() {
            return executionTimeNanos;
        }

        public long getVertexVisits() {
            return vertexVisits;
        }

        public long getEdgeExaminations() {
            return edgeExaminations;
        }

        public long getTotalOperations() {
            return vertexVisits + edgeExaminations;
        }
    }

    public static Result sort(Graph graph) {
        long startTime = System.nanoTime();
        int n = graph.getVertices();
        int[] inDegree = new int[n];
        long edgeExams = 0;
        long vertexVisits = 0;

        for (int u = 0; u < n; u++) {
            for (Edge e : graph.getNeighbors(u)) {
                inDegree[e.getTo()]++;
                edgeExams++;
            }
        }

        Deque<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < n; i++) {
            vertexVisits++;
            if (inDegree[i] == 0) {
                queue.offer(i);
            }
        }

        List<Integer> order = new ArrayList<>(n);
        while (!queue.isEmpty()) {
            int u = queue.poll();
            vertexVisits++;
            order.add(u);
            for (Edge e : graph.getNeighbors(u)) {
                edgeExams++;
                int v = e.getTo();
                inDegree[v]--;
                if (inDegree[v] == 0) {
                    queue.offer(v);
                }
            }
        }

        boolean isDAG = (order.size() == n);
        if (!isDAG) {
            order.clear();
        }
        long elapsed = System.nanoTime() - startTime;
        return new Result(isDAG, order, elapsed, vertexVisits, edgeExams);
    }

    public static boolean isValidTopologicalOrder(Graph graph, List<Integer> order) {
        int n = graph.getVertices();
        if (order.size() != n) {
            return false;
        }
        int[] pos = new int[n];
        boolean[] seen = new boolean[n];
        for (int i = 0; i < n; i++) {
            int v = order.get(i);
            if (v < 0 || v >= n || seen[v]) {
                return false;
            }
            seen[v] = true;
            pos[v] = i;
        }
        for (int u = 0; u < n; u++) {
            for (Edge e : graph.getNeighbors(u)) {
                if (pos[u] >= pos[e.getTo()]) {
                    return false;
                }
            }
        }
        return true;
    }
}
