package mst;

import dsu.DisjointSetUnion;
import graph.Edge;
import graph.Graph;
import java.util.ArrayList;
import java.util.List;

public class KruskalMST {

    public static MSTResult compute(Graph graph) {
        long startTime = System.nanoTime();
        int n = graph.getVertices();
        if (n == 0) {
            long elapsed = System.nanoTime() - startTime;
            return new MSTResult("Kruskal", new ArrayList<>(), 0, true, elapsed, 0, 0, 0);
        }

        List<Edge> sortedEdges = new ArrayList<>(graph.getEdges());
        long[] comparisons = new long[]{0};
        sortedEdges.sort((a, b) -> {
            comparisons[0]++;
            return Integer.compare(a.getWeight(), b.getWeight());
        });

        DisjointSetUnion dsu = new DisjointSetUnion(n);
        List<Edge> mstEdges = new ArrayList<>();
        long totalWeight = 0;
        long edgeExaminations = 0;

        for (Edge edge : sortedEdges) {
            edgeExaminations++;
            if (dsu.union(edge.getFrom(), edge.getTo())) {
                mstEdges.add(edge);
                totalWeight += edge.getWeight();
                if (mstEdges.size() == n - 1) {
                    break;
                }
            }
        }

        boolean connected = (n <= 1) || (dsu.getComponentsCount() == 1 && mstEdges.size() == n - 1);
        long elapsed = System.nanoTime() - startTime;

        return new MSTResult(
                "Kruskal",
                mstEdges,
                totalWeight,
                connected,
                elapsed,
                edgeExaminations,
                comparisons[0],
                dsu.getTotalOperations()
        );
    }
}
