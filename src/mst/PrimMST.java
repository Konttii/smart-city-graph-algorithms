package mst;

import graph.Edge;
import graph.Graph;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class PrimMST {

    public static MSTResult compute(Graph graph) {
        long startTime = System.nanoTime();
        int n = graph.getVertices();
        if (n == 0) {
            long elapsed = System.nanoTime() - startTime;
            return new MSTResult("Prim", new ArrayList<>(), 0, true, elapsed, 0, 0, 0);
        }

        boolean[] visited = new boolean[n];
        List<Edge> mstEdges = new ArrayList<>();
        long totalWeight = 0;
        long edgeExaminations = 0;
        long[] comparisons = new long[]{0};
        long heapOps = 0;

        PriorityQueue<Edge> pq = new PriorityQueue<>((a, b) -> {
            comparisons[0]++;
            return Integer.compare(a.getWeight(), b.getWeight());
        });

        visited[0] = true;
        int visitedCount = 1;
        for (Edge e : graph.getNeighbors(0)) {
            edgeExaminations++;
            if (!visited[e.getTo()]) {
                pq.offer(e);
                heapOps++;
            }
        }

        while (!pq.isEmpty() && visitedCount < n) {
            Edge minEdge = pq.poll();
            heapOps++;
            int nextVertex = minEdge.getTo();
            if (visited[nextVertex]) {
                continue;
            }
            visited[nextVertex] = true;
            visitedCount++;
            mstEdges.add(minEdge);
            totalWeight += minEdge.getWeight();

            for (Edge neighbor : graph.getNeighbors(nextVertex)) {
                edgeExaminations++;
                if (!visited[neighbor.getTo()]) {
                    pq.offer(neighbor);
                    heapOps++;
                }
            }
        }

        boolean connected = (visitedCount == n);
        long elapsed = System.nanoTime() - startTime;

        return new MSTResult(
                "Prim",
                mstEdges,
                totalWeight,
                connected,
                elapsed,
                edgeExaminations,
                comparisons[0],
                heapOps
        );
    }
}
