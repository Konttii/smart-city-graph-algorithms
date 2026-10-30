package graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class GraphGenerator {

    public static Graph generateConnectedUndirectedGraph(int vertices, int targetEdges, long seed) {
        if (vertices <= 0) {
            return new Graph(0, false);
        }
        long maxEdges = (long) vertices * (vertices - 1) / 2;
        int edgesToGenerate = (int) Math.min(targetEdges, maxEdges);
        if (vertices > 1 && edgesToGenerate < vertices - 1) {
            edgesToGenerate = vertices - 1;
        }

        Graph graph = new Graph(vertices, false);
        Random rng = new Random(seed);
        Set<Long> existingEdges = new HashSet<>();

        List<Integer> order = new ArrayList<>(vertices);
        for (int i = 0; i < vertices; i++) {
            order.add(i);
        }
        Collections.shuffle(order, rng);

        for (int i = 1; i < vertices; i++) {
            int u = order.get(i);
            int v = order.get(rng.nextInt(i));
            int weight = 1 + rng.nextInt(100);
            graph.addEdge(u, v, weight);
            existingEdges.add(edgeKey(Math.min(u, v), Math.max(u, v)));
        }

        while (graph.getEdges().size() < edgesToGenerate) {
            int u = rng.nextInt(vertices);
            int v = rng.nextInt(vertices);
            if (u == v) {
                continue;
            }
            long key = edgeKey(Math.min(u, v), Math.max(u, v));
            if (!existingEdges.contains(key)) {
                existingEdges.add(key);
                int weight = 1 + rng.nextInt(100);
                graph.addEdge(u, v, weight);
            }
        }

        return graph;
    }

    public static Graph generateDAG(int vertices, int targetEdges, long seed) {
        if (vertices <= 0) {
            return new Graph(0, true);
        }
        long maxEdges = (long) vertices * (vertices - 1) / 2;
        int edgesToGenerate = (int) Math.min(targetEdges, maxEdges);

        Graph graph = new Graph(vertices, true);
        Random rng = new Random(seed);
        Set<Long> existingEdges = new HashSet<>();

        List<Integer> topoOrder = new ArrayList<>(vertices);
        for (int i = 0; i < vertices; i++) {
            topoOrder.add(i);
        }
        Collections.shuffle(topoOrder, rng);

        while (graph.getEdges().size() < edgesToGenerate) {
            int idx1 = rng.nextInt(vertices);
            int idx2 = rng.nextInt(vertices);
            if (idx1 == idx2) {
                continue;
            }
            int fromIdx = Math.min(idx1, idx2);
            int toIdx = Math.max(idx1, idx2);
            int u = topoOrder.get(fromIdx);
            int v = topoOrder.get(toIdx);
            long key = edgeKey(u, v);
            if (!existingEdges.contains(key)) {
                existingEdges.add(key);
                int weight = 1 + rng.nextInt(100);
                graph.addEdge(u, v, weight);
            }
        }

        return graph;
    }

    public static Graph generateDirectedCyclicGraph(int vertices, int targetEdges, long seed) {
        if (vertices <= 0) {
            return new Graph(0, true);
        }
        long maxEdges = (long) vertices * (vertices - 1);
        int edgesToGenerate = (int) Math.min(targetEdges, maxEdges);

        Graph graph = new Graph(vertices, true);
        Random rng = new Random(seed);
        Set<Long> existingEdges = new HashSet<>();

        if (vertices >= 3 && edgesToGenerate >= 3) {
            int[][] cycle = {{0, 1}, {1, 2}, {2, 0}};
            for (int[] pair : cycle) {
                long key = edgeKey(pair[0], pair[1]);
                existingEdges.add(key);
                graph.addEdge(pair[0], pair[1], 1 + rng.nextInt(100));
            }
        }

        while (graph.getEdges().size() < edgesToGenerate) {
            int u = rng.nextInt(vertices);
            int v = rng.nextInt(vertices);
            if (u == v) {
                continue;
            }
            long key = edgeKey(u, v);
            if (!existingEdges.contains(key)) {
                existingEdges.add(key);
                int weight = 1 + rng.nextInt(100);
                graph.addEdge(u, v, weight);
            }
        }

        return graph;
    }

    private static long edgeKey(int u, int v) {
        return (((long) u) << 32) | (v & 0xffffffffL);
    }
}
