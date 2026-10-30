import connectivity.TarjanSCC;
import connectivity.TopologicalSort;
import dsu.DisjointSetUnion;
import graph.Graph;
import graph.GraphGenerator;
import mst.KruskalMST;
import mst.MSTResult;
import mst.PrimMST;
import shortestpath.Dijkstra;
import java.util.List;

public class TestSuite {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=== Running Smart City Graph Algorithms Test Suite ===");

        testSingleVertexGraph();
        testSmallGraphs();
        testDisconnectedGraphs();
        testGraphsWithCycles();
        testMultipleValidMSTsAndDuplicateWeights();
        testUnreachableVerticesInDijkstra();
        testNegativeWeightRejectionInDijkstra();
        testKruskalVsPrimEquivalenceOnRandomConnectedGraphs();
        testDAGTopologicalValidityAndSCCPartitioning();
        testDSUPathCompressionAndRank();

        System.out.println("======================================================");
        System.out.println("Tests Passed: " + passed + ", Failed: " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void check(boolean condition, String testName) {
        if (condition) {
            passed++;
            System.out.println("[PASS] " + testName);
        } else {
            failed++;
            System.out.println("[FAIL] " + testName);
        }
    }

    private static void testSingleVertexGraph() {
        Graph gUndirected = new Graph(1, false);
        MSTResult kruskal = KruskalMST.compute(gUndirected);
        MSTResult prim = PrimMST.compute(gUndirected);
        check(kruskal.isConnected() && kruskal.getTotalWeight() == 0 && kruskal.getMstEdges().isEmpty(),
                "Single vertex undirected graph: Kruskal connected with 0 weight");
        check(prim.isConnected() && prim.getTotalWeight() == 0 && prim.getMstEdges().isEmpty(),
                "Single vertex undirected graph: Prim connected with 0 weight");

        Graph gDirected = new Graph(1, true);
        TopologicalSort.Result topo = TopologicalSort.sort(gDirected);
        check(topo.isDAG() && topo.getOrder().size() == 1 && topo.getOrder().get(0) == 0,
                "Single vertex directed graph: valid topological sort");

        TarjanSCC.Result scc = TarjanSCC.findSCCs(gDirected);
        check(scc.getComponents().size() == 1 && scc.getComponents().get(0).size() == 1,
                "Single vertex directed graph: exactly 1 SCC");

        Dijkstra.Result sp = Dijkstra.shortestPaths(gDirected, 0);
        check(sp.getDistance(0) == 0 && sp.getPathTo(0).equals(List.of(0)),
                "Single vertex graph: Dijkstra distance 0 to self");
    }

    private static void testSmallGraphs() {
        Graph g = new Graph(4, false);
        g.addEdge(0, 1, 10);
        g.addEdge(0, 2, 6);
        g.addEdge(0, 3, 5);
        g.addEdge(1, 3, 15);
        g.addEdge(2, 3, 4);

        MSTResult kruskal = KruskalMST.compute(g);
        MSTResult prim = PrimMST.compute(g);
        check(kruskal.isConnected() && kruskal.getTotalWeight() == 19,
                "Small graph Kruskal MST total weight == 19");
        check(prim.isConnected() && prim.getTotalWeight() == 19,
                "Small graph Prim MST total weight == 19");
    }

    private static void testDisconnectedGraphs() {
        Graph g = new Graph(5, false);
        g.addEdge(0, 1, 3);
        g.addEdge(1, 2, 4);
        g.addEdge(3, 4, 2);

        MSTResult kruskal = KruskalMST.compute(g);
        MSTResult prim = PrimMST.compute(g);
        check(!kruskal.isConnected(), "Disconnected graph detected by Kruskal");
        check(!prim.isConnected(), "Disconnected graph detected by Prim");
    }

    private static void testGraphsWithCycles() {
        Graph cyclic = new Graph(4, true);
        cyclic.addEdge(0, 1, 1);
        cyclic.addEdge(1, 2, 1);
        cyclic.addEdge(2, 0, 1);
        cyclic.addEdge(2, 3, 1);

        TopologicalSort.Result topo = TopologicalSort.sort(cyclic);
        check(!topo.isDAG() && topo.getOrder().isEmpty(),
                "Cyclic directed graph rejected by topological sort");

        TarjanSCC.Result scc = TarjanSCC.findSCCs(cyclic);
        check(scc.getComponents().size() == 2,
                "Cyclic graph with 3-cycle + 1 tail has 2 SCCs");
    }

    private static void testMultipleValidMSTsAndDuplicateWeights() {
        Graph square = new Graph(4, false);
        square.addEdge(0, 1, 5);
        square.addEdge(1, 2, 5);
        square.addEdge(2, 3, 5);
        square.addEdge(3, 0, 5);
        square.addEdge(0, 2, 5);

        MSTResult kruskal = KruskalMST.compute(square);
        MSTResult prim = PrimMST.compute(square);
        check(kruskal.isConnected() && prim.isConnected()
                        && kruskal.getTotalWeight() == 15
                        && prim.getTotalWeight() == 15
                        && kruskal.getMstEdges().size() == 3
                        && prim.getMstEdges().size() == 3,
                "Duplicate edge weights / multiple valid MSTs yield identical optimal weight (15)");
    }

    private static void testUnreachableVerticesInDijkstra() {
        Graph g = new Graph(5, true);
        g.addEdge(0, 1, 4);
        g.addEdge(0, 2, 1);
        g.addEdge(2, 1, 2);
        g.addEdge(1, 3, 5);

        Dijkstra.Result res = Dijkstra.shortestPaths(g, 0);
        check(res.getDistance(0) == 0
                        && res.getDistance(2) == 1
                        && res.getDistance(1) == 3
                        && res.getDistance(3) == 8,
                "Dijkstra computes exact shortest distances on reachable vertices");
        check(res.getPathTo(3).equals(List.of(0, 2, 1, 3)),
                "Dijkstra reconstructs optimal shortest path [0, 2, 1, 3]");
        check(!res.isReachable(4) && res.getDistance(4) == Long.MAX_VALUE && res.getPathTo(4).isEmpty(),
                "Dijkstra handles unreachable vertex properly");
    }

    private static void testNegativeWeightRejectionInDijkstra() {
        Graph g = new Graph(3, true);
        g.addEdge(0, 1, 4);
        g.addEdge(1, 2, -2);
        boolean threw = false;
        try {
            Dijkstra.shortestPaths(g, 0);
        } catch (IllegalArgumentException ex) {
            threw = true;
        }
        check(threw, "Dijkstra rejects negative edge weights");
    }

    private static void testKruskalVsPrimEquivalenceOnRandomConnectedGraphs() {
        int[] sizes = {10, 100, 1000};
        int[] edges = {30, 500, 5000};
        boolean allEqual = true;
        for (int i = 0; i < sizes.length; i++) {
            Graph g = GraphGenerator.generateConnectedUndirectedGraph(sizes[i], edges[i], 42L + i);
            MSTResult k = KruskalMST.compute(g);
            MSTResult p = PrimMST.compute(g);
            if (!k.isConnected() || !p.isConnected() || k.getTotalWeight() != p.getTotalWeight()) {
                allEqual = false;
            }
        }
        check(allEqual, "Kruskal and Prim return identical MST weight across Small, Medium, and Large graphs");
    }

    private static void testDAGTopologicalValidityAndSCCPartitioning() {
        int[] sizes = {10, 100, 1000};
        int[] edges = {30, 500, 5000};
        boolean validDAGs = true;
        boolean validSCCPartition = true;

        for (int i = 0; i < sizes.length; i++) {
            Graph dag = GraphGenerator.generateDAG(sizes[i], edges[i], 100L + i);
            TopologicalSort.Result topo = TopologicalSort.sort(dag);
            if (!topo.isDAG() || !TopologicalSort.isValidTopologicalOrder(dag, topo.getOrder())) {
                validDAGs = false;
            }

            Graph cyclic = GraphGenerator.generateDirectedCyclicGraph(sizes[i], edges[i], 200L + i);
            TopologicalSort.Result cyclicTopo = TopologicalSort.sort(cyclic);
            if (cyclicTopo.isDAG()) {
                validDAGs = false;
            }

            TarjanSCC.Result scc = TarjanSCC.findSCCs(cyclic);
            int[] counts = new int[sizes[i]];
            for (List<Integer> comp : scc.getComponents()) {
                for (int v : comp) {
                    counts[v]++;
                }
            }
            for (int c : counts) {
                if (c != 1) {
                    validSCCPartition = false;
                }
            }
        }

        check(validDAGs, "Topological ordering is valid for every generated DAG and detects cycles on cyclic graphs");
        check(validSCCPartition, "Every vertex belongs to exactly one SCC across all graph sizes");
    }

    private static void testDSUPathCompressionAndRank() {
        DisjointSetUnion dsu = new DisjointSetUnion(6);
        check(dsu.getComponentsCount() == 6, "DSU starts with n components");
        dsu.union(0, 1);
        dsu.union(1, 2);
        dsu.union(3, 4);
        check(dsu.isConnected(0, 2) && !dsu.isConnected(0, 3), "DSU connectivity checks after unions");
        check(dsu.getComponentSize(0) == 3 && dsu.getComponentSize(3) == 2, "DSU tracks component sizes accurately");
    }
}
