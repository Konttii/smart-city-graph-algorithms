import connectivity.TarjanSCC;
import connectivity.TopologicalSort;
import graph.Graph;
import graph.GraphGenerator;
import mst.KruskalMST;
import mst.MSTResult;
import mst.PrimMST;
import shortestpath.Dijkstra;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import javax.imageio.ImageIO;

public class Main {

    private static final long BASE_SEED = 20261031L;
    private static final int REPEATS = 5;

    private static final String[] CONFIG_NAMES = {"Small", "Medium", "Large"};
    private static final int[] VERTEX_COUNTS = {10, 100, 1000};
    private static final int[] EDGE_COUNTS = {30, 500, 5000};

    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.US);

        runCityScenarioDemo();
        TestSuite.main(new String[0]);
        runExperimentalBenchmarks();
    }

    private static void runCityScenarioDemo() {
        System.out.println("=== Smart City Infrastructure & Route Planning Demo ===");
        Graph cityUndirected = new Graph(6, false);
        String[] names = {
                "Downtown Hub", "Tech Park", "Residential North",
                "Industrial Zone", "University Campus", "Hospital District"
        };
        for (int i = 0; i < names.length; i++) {
            cityUndirected.setDistrictName(i, names[i]);
        }
        cityUndirected.addEdge(0, 1, 12);
        cityUndirected.addEdge(0, 2, 18);
        cityUndirected.addEdge(1, 2, 9);
        cityUndirected.addEdge(1, 3, 22);
        cityUndirected.addEdge(2, 4, 14);
        cityUndirected.addEdge(3, 4, 8);
        cityUndirected.addEdge(3, 5, 16);
        cityUndirected.addEdge(4, 5, 11);

        MSTResult kruskal = KruskalMST.compute(cityUndirected);
        MSTResult prim = PrimMST.compute(cityUndirected);
        System.out.println("MST Connected: " + kruskal.isConnected());
        System.out.println("Kruskal Total Cost: " + kruskal.getTotalWeight() + " | Edges: " + kruskal.getMstEdges());
        System.out.println("Prim Total Cost:    " + prim.getTotalWeight() + " | Edges: " + prim.getMstEdges());

        Graph cityDirected = new Graph(6, true);
        cityDirected.addEdge(0, 1, 5);
        cityDirected.addEdge(1, 2, 4);
        cityDirected.addEdge(2, 0, 6);
        cityDirected.addEdge(2, 3, 7);
        cityDirected.addEdge(3, 4, 3);
        cityDirected.addEdge(4, 5, 2);

        TarjanSCC.Result scc = TarjanSCC.findSCCs(cityDirected);
        System.out.println("Strongly Connected Components: " + scc.getComponents());

        TopologicalSort.Result topoCyclic = TopologicalSort.sort(cityDirected);
        System.out.println("Topological Sort on cyclic road network isDAG: " + topoCyclic.isDAG());

        Dijkstra.Result routes = Dijkstra.shortestPaths(cityDirected, 0);
        System.out.println("Shortest distance from Downtown Hub (0) to Hospital District (5): "
                + routes.getDistance(5) + " via path " + routes.getPathTo(5));
        System.out.println();
    }

    private static void runExperimentalBenchmarks() throws IOException {
        new File("results/tables").mkdirs();
        new File("results/plots").mkdirs();

        warmupJIT();

        System.out.println("\n=== Running Experimental Analysis (5 Repeats per Configuration, Seed=" + BASE_SEED + ") ===");

        double[] kruskalMs = new double[CONFIG_NAMES.length];
        double[] primMs = new double[CONFIG_NAMES.length];
        double[] topoMs = new double[CONFIG_NAMES.length];
        double[] sccMs = new double[CONFIG_NAMES.length];
        double[] dijkstraMs = new double[CONFIG_NAMES.length];
        long[] kruskalOps = new long[CONFIG_NAMES.length];
        long[] primOps = new long[CONFIG_NAMES.length];

        try (PrintWriter mstCsv = new PrintWriter(new FileWriter("results/tables/mst_comparison.csv"));
             PrintWriter connCsv = new PrintWriter(new FileWriter("results/tables/connectivity_results.csv"));
             PrintWriter dijkstraCsv = new PrintWriter(new FileWriter("results/tables/dijkstra_results.csv"));
             PrintWriter allSummaryCsv = new PrintWriter(new FileWriter("results/tables/benchmark_summary.csv"))) {

            mstCsv.println("Config,Vertices,Edges,Algorithm,AvgTimeNs,AvgTimeMs,MSTWeight,Connected,EdgeExams,Comparisons,DSUOrHeapOps,TotalOps");
            connCsv.println("Config,Vertices,Edges,GraphType,Algorithm,AvgTimeNs,AvgTimeMs,ValidOrSCCCount,PrimaryOps,EdgeExams,AuxOps,TotalOps");
            dijkstraCsv.println("Config,Vertices,Edges,Algorithm,AvgTimeNs,AvgTimeMs,ReachableCount,EdgeRelaxations,Comparisons,HeapOps,TotalOps");
            allSummaryCsv.println("Config,Vertices,Edges,Algorithm,AvgTimeNs,AvgTimeMs,TotalOps");

            for (int i = 0; i < CONFIG_NAMES.length; i++) {
                String cfg = CONFIG_NAMES[i];
                int v = VERTEX_COUNTS[i];
                int e = EDGE_COUNTS[i];
                long seed = BASE_SEED + i * 1000L;

                Graph undirected = GraphGenerator.generateConnectedUndirectedGraph(v, e, seed);
                Graph dag = GraphGenerator.generateDAG(v, e, seed + 1);
                Graph cyclic = GraphGenerator.generateDirectedCyclicGraph(v, e, seed + 2);

                long kruskalTimeSum = 0;
                MSTResult lastKruskal = null;
                for (int r = 0; r < REPEATS; r++) {
                    lastKruskal = KruskalMST.compute(undirected);
                    kruskalTimeSum += lastKruskal.getExecutionTimeNanos();
                }
                long kruskalAvgNs = kruskalTimeSum / REPEATS;

                long primTimeSum = 0;
                MSTResult lastPrim = null;
                for (int r = 0; r < REPEATS; r++) {
                    lastPrim = PrimMST.compute(undirected);
                    primTimeSum += lastPrim.getExecutionTimeNanos();
                }
                long primAvgNs = primTimeSum / REPEATS;

                mstCsv.printf("%s,%d,%d,Kruskal,%d,%.4f,%d,%b,%d,%d,%d,%d%n",
                        cfg, v, e, kruskalAvgNs, kruskalAvgNs / 1_000_000.0,
                        lastKruskal.getTotalWeight(), lastKruskal.isConnected(),
                        lastKruskal.getEdgeExaminations(), lastKruskal.getComparisons(),
                        lastKruskal.getDsuOrHeapOperations(), lastKruskal.getTotalOperations());

                mstCsv.printf("%s,%d,%d,Prim,%d,%.4f,%d,%b,%d,%d,%d,%d%n",
                        cfg, v, e, primAvgNs, primAvgNs / 1_000_000.0,
                        lastPrim.getTotalWeight(), lastPrim.isConnected(),
                        lastPrim.getEdgeExaminations(), lastPrim.getComparisons(),
                        lastPrim.getDsuOrHeapOperations(), lastPrim.getTotalOperations());

                long topoDagTimeSum = 0;
                TopologicalSort.Result lastTopoDag = null;
                for (int r = 0; r < REPEATS; r++) {
                    lastTopoDag = TopologicalSort.sort(dag);
                    topoDagTimeSum += lastTopoDag.getExecutionTimeNanos();
                }
                long topoDagAvgNs = topoDagTimeSum / REPEATS;

                long topoCyclicTimeSum = 0;
                TopologicalSort.Result lastTopoCyclic = null;
                for (int r = 0; r < REPEATS; r++) {
                    lastTopoCyclic = TopologicalSort.sort(cyclic);
                    topoCyclicTimeSum += lastTopoCyclic.getExecutionTimeNanos();
                }
                long topoCyclicAvgNs = topoCyclicTimeSum / REPEATS;

                long sccDagTimeSum = 0;
                TarjanSCC.Result lastSccDag = null;
                for (int r = 0; r < REPEATS; r++) {
                    lastSccDag = TarjanSCC.findSCCs(dag);
                    sccDagTimeSum += lastSccDag.getExecutionTimeNanos();
                }
                long sccDagAvgNs = sccDagTimeSum / REPEATS;

                long sccCyclicTimeSum = 0;
                TarjanSCC.Result lastSccCyclic = null;
                for (int r = 0; r < REPEATS; r++) {
                    lastSccCyclic = TarjanSCC.findSCCs(cyclic);
                    sccCyclicTimeSum += lastSccCyclic.getExecutionTimeNanos();
                }
                long sccCyclicAvgNs = sccCyclicTimeSum / REPEATS;

                connCsv.printf("%s,%d,%d,DAG,TopologicalSort,%d,%.4f,%s,%d,%d,0,%d%n",
                        cfg, v, e, topoDagAvgNs, topoDagAvgNs / 1_000_000.0,
                        "DAG=" + lastTopoDag.isDAG(),
                        lastTopoDag.getVertexVisits(), lastTopoDag.getEdgeExaminations(),
                        lastTopoDag.getTotalOperations());

                connCsv.printf("%s,%d,%d,Cyclic,TopologicalSort,%d,%.4f,%s,%d,%d,0,%d%n",
                        cfg, v, e, topoCyclicAvgNs, topoCyclicAvgNs / 1_000_000.0,
                        "DAG=" + lastTopoCyclic.isDAG(),
                        lastTopoCyclic.getVertexVisits(), lastTopoCyclic.getEdgeExaminations(),
                        lastTopoCyclic.getTotalOperations());

                connCsv.printf("%s,%d,%d,DAG,TarjanSCC,%d,%.4f,%s,%d,%d,%d,%d%n",
                        cfg, v, e, sccDagAvgNs, sccDagAvgNs / 1_000_000.0,
                        "SCCs=" + lastSccDag.getComponents().size(),
                        lastSccDag.getDfsCalls(), lastSccDag.getEdgeExaminations(),
                        lastSccDag.getStackOperations(), lastSccDag.getTotalOperations());

                connCsv.printf("%s,%d,%d,Cyclic,TarjanSCC,%d,%.4f,%s,%d,%d,%d,%d%n",
                        cfg, v, e, sccCyclicAvgNs, sccCyclicAvgNs / 1_000_000.0,
                        "SCCs=" + lastSccCyclic.getComponents().size(),
                        lastSccCyclic.getDfsCalls(), lastSccCyclic.getEdgeExaminations(),
                        lastSccCyclic.getStackOperations(), lastSccCyclic.getTotalOperations());

                long dijkstraTimeSum = 0;
                Dijkstra.Result lastDijkstra = null;
                for (int r = 0; r < REPEATS; r++) {
                    lastDijkstra = Dijkstra.shortestPaths(cyclic, 0);
                    dijkstraTimeSum += lastDijkstra.getExecutionTimeNanos();
                }
                long dijkstraAvgNs = dijkstraTimeSum / REPEATS;
                int reachable = 0;
                for (int node = 0; node < v; node++) {
                    if (lastDijkstra.isReachable(node)) {
                        reachable++;
                    }
                }

                dijkstraCsv.printf("%s,%d,%d,Dijkstra,%d,%.4f,%d,%d,%d,%d,%d%n",
                        cfg, v, e, dijkstraAvgNs, dijkstraAvgNs / 1_000_000.0,
                        reachable, lastDijkstra.getEdgeRelaxations(),
                        lastDijkstra.getComparisons(), lastDijkstra.getHeapOperations(),
                        lastDijkstra.getTotalOperations());

                allSummaryCsv.printf("%s,%d,%d,Kruskal,%d,%.4f,%d%n",
                        cfg, v, e, kruskalAvgNs, kruskalAvgNs / 1_000_000.0, lastKruskal.getTotalOperations());
                allSummaryCsv.printf("%s,%d,%d,Prim,%d,%.4f,%d%n",
                        cfg, v, e, primAvgNs, primAvgNs / 1_000_000.0, lastPrim.getTotalOperations());
                allSummaryCsv.printf("%s,%d,%d,TopologicalSort(DAG),%d,%.4f,%d%n",
                        cfg, v, e, topoDagAvgNs, topoDagAvgNs / 1_000_000.0, lastTopoDag.getTotalOperations());
                allSummaryCsv.printf("%s,%d,%d,TarjanSCC(Cyclic),%d,%.4f,%d%n",
                        cfg, v, e, sccCyclicAvgNs, sccCyclicAvgNs / 1_000_000.0, lastSccCyclic.getTotalOperations());
                allSummaryCsv.printf("%s,%d,%d,Dijkstra,%d,%.4f,%d%n",
                        cfg, v, e, dijkstraAvgNs, dijkstraAvgNs / 1_000_000.0, lastDijkstra.getTotalOperations());

                kruskalMs[i] = kruskalAvgNs / 1_000_000.0;
                primMs[i] = primAvgNs / 1_000_000.0;
                topoMs[i] = topoDagAvgNs / 1_000_000.0;
                sccMs[i] = sccCyclicAvgNs / 1_000_000.0;
                dijkstraMs[i] = dijkstraAvgNs / 1_000_000.0;
                kruskalOps[i] = lastKruskal.getTotalOperations();
                primOps[i] = lastPrim.getTotalOperations();

                System.out.printf("[%s: V=%d, E=%d] Kruskal=%d ns (w=%d), Prim=%d ns (w=%d), Topo(DAG)=%d ns, SCC=%d ns (%d SCCs), Dijkstra=%d ns%n",
                        cfg, v, e, kruskalAvgNs, lastKruskal.getTotalWeight(),
                        primAvgNs, lastPrim.getTotalWeight(),
                        topoDagAvgNs, sccCyclicAvgNs, lastSccCyclic.getComponents().size(), dijkstraAvgNs);
            }
        }
        System.out.println("Saved CSV tables to results/tables/");

        double[][] allTimes = {kruskalMs, primMs, topoMs, sccMs, dijkstraMs};
        String[] algNames = {"Kruskal MST", "Prim MST", "Topological Sort (DAG)", "Tarjan SCC", "Dijkstra"};

        saveLinePlot(
                "results/plots/execution_time_vs_vertices.png",
                "Execution Time vs. Number of Vertices (V)",
                "Number of Vertices (V)",
                "Average Execution Time (ms)",
                VERTEX_COUNTS,
                allTimes,
                algNames
        );
        saveLinePlot(
                "results/plots/execution_time_vs_edges.png",
                "Execution Time vs. Number of Edges (E)",
                "Number of Edges (E)",
                "Average Execution Time (ms)",
                EDGE_COUNTS,
                allTimes,
                algNames
        );
        saveBarComparisonPlot(
                "results/plots/kruskal_vs_prim_operations.png",
                "Kruskal vs. Prim Total Operation Counts Across Graph Sizes",
                CONFIG_NAMES,
                kruskalOps,
                primOps
        );
        System.out.println("Saved PNG plots to results/plots/");
    }

    private static void saveLinePlot(String filePath, String title, String xLabel, String yLabel,
                                     int[] xValues, double[][] series, String[] labels) throws IOException {
        int width = 900;
        int height = 560;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        int left = 90;
        int right = width - 40;
        int top = 65;
        int bottom = height - 75;
        int plotW = right - left;
        int plotH = bottom - top;

        double maxY = 0.001;
        for (double[] s : series) {
            for (double v : s) {
                if (v > maxY) {
                    maxY = v;
                }
            }
        }
        maxY *= 1.15;

        g.setColor(new Color(230, 234, 240));
        g.setStroke(new BasicStroke(1f));
        int yTicks = 5;
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i <= yTicks; i++) {
            int y = bottom - (int) ((double) i / yTicks * plotH);
            g.setColor(new Color(230, 234, 240));
            g.drawLine(left, y, right, y);
            double val = (maxY * i) / yTicks;
            String tickStr = String.format("%.2f", val);
            g.setColor(Color.DARK_GRAY);
            g.drawString(tickStr, left - fm.stringWidth(tickStr) - 10, y + 5);
        }

        int[] xPositions = new int[xValues.length];
        for (int i = 0; i < xValues.length; i++) {
            xPositions[i] = left + (int) ((double) i / (xValues.length - 1) * plotW);
            g.setColor(new Color(230, 234, 240));
            g.drawLine(xPositions[i], top, xPositions[i], bottom);
            String xStr = String.valueOf(xValues[i]);
            g.setColor(Color.DARK_GRAY);
            g.drawString(xStr, xPositions[i] - fm.stringWidth(xStr) / 2, bottom + 22);
        }

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1.5f));
        g.drawRect(left, top, plotW, plotH);

        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        fm = g.getFontMetrics();
        g.drawString(title, (width - fm.stringWidth(title)) / 2, 36);

        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        fm = g.getFontMetrics();
        g.drawString(xLabel, left + (plotW - fm.stringWidth(xLabel)) / 2, height - 22);

        Graphics2D gRot = (Graphics2D) g.create();
        gRot.rotate(-Math.PI / 2);
        gRot.drawString(yLabel, -(top + (plotH + fm.stringWidth(yLabel)) / 2), 26);
        gRot.dispose();

        Color[] palette = {
                new Color(31, 119, 180),
                new Color(255, 127, 14),
                new Color(44, 160, 44),
                new Color(214, 39, 40),
                new Color(148, 103, 189)
        };

        g.setStroke(new BasicStroke(2.6f));
        for (int sIdx = 0; sIdx < series.length; sIdx++) {
            g.setColor(palette[sIdx % palette.length]);
            for (int i = 0; i < xValues.length; i++) {
                int x1 = xPositions[i];
                int y1 = bottom - (int) (series[sIdx][i] / maxY * plotH);
                g.fillOval(x1 - 5, y1 - 5, 10, 10);
                if (i + 1 < xValues.length) {
                    int x2 = xPositions[i + 1];
                    int y2 = bottom - (int) (series[sIdx][i + 1] / maxY * plotH);
                    g.drawLine(x1, y1, x2, y2);
                }
            }
        }

        int legendX = left + 20;
        int legendY = top + 18;
        g.setColor(new Color(255, 255, 255, 235));
        g.fillRect(legendX - 8, legendY - 14, 210, series.length * 24 + 10);
        g.setColor(Color.LIGHT_GRAY);
        g.setStroke(new BasicStroke(1f));
        g.drawRect(legendX - 8, legendY - 14, 210, series.length * 24 + 10);

        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        for (int sIdx = 0; sIdx < series.length; sIdx++) {
            int rowY = legendY + sIdx * 24;
            g.setColor(palette[sIdx % palette.length]);
            g.setStroke(new BasicStroke(2.6f));
            g.drawLine(legendX, rowY, legendX + 24, rowY);
            g.fillOval(legendX + 8, rowY - 4, 8, 8);
            g.setColor(Color.BLACK);
            g.drawString(labels[sIdx], legendX + 34, rowY + 5);
        }

        g.dispose();
        ImageIO.write(image, "png", new File(filePath));
    }

    private static void saveBarComparisonPlot(String filePath, String title, String[] categories,
                                              long[] kruskalOps, long[] primOps) throws IOException {
        int width = 900;
        int height = 560;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        int left = 100;
        int right = width - 40;
        int top = 65;
        int bottom = height - 75;
        int plotW = right - left;
        int plotH = bottom - top;

        long maxVal = 1;
        for (long v : kruskalOps) maxVal = Math.max(maxVal, v);
        for (long v : primOps) maxVal = Math.max(maxVal, v);
        double maxY = maxVal * 1.15;

        int yTicks = 5;
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i <= yTicks; i++) {
            int y = bottom - (int) ((double) i / yTicks * plotH);
            g.setColor(new Color(230, 234, 240));
            g.drawLine(left, y, right, y);
            long val = Math.round((maxY * i) / yTicks);
            String tickStr = String.valueOf(val);
            g.setColor(Color.DARK_GRAY);
            g.drawString(tickStr, left - fm.stringWidth(tickStr) - 10, y + 5);
        }

        Color kColor = new Color(31, 119, 180);
        Color pColor = new Color(255, 127, 14);
        int groupW = plotW / categories.length;
        int barW = 55;

        for (int i = 0; i < categories.length; i++) {
            int centerX = left + i * groupW + groupW / 2;
            int kH = (int) (kruskalOps[i] / maxY * plotH);
            int pH = (int) (primOps[i] / maxY * plotH);

            g.setColor(kColor);
            g.fillRect(centerX - barW - 4, bottom - kH, barW, kH);
            g.setColor(Color.DARK_GRAY);
            String kLabel = String.valueOf(kruskalOps[i]);
            g.drawString(kLabel, centerX - barW - 4 + (barW - fm.stringWidth(kLabel)) / 2, bottom - kH - 6);

            g.setColor(pColor);
            g.fillRect(centerX + 4, bottom - pH, barW, pH);
            g.setColor(Color.DARK_GRAY);
            String pLabel = String.valueOf(primOps[i]);
            g.drawString(pLabel, centerX + 4 + (barW - fm.stringWidth(pLabel)) / 2, bottom - pH - 6);

            g.setColor(Color.BLACK);
            g.drawString(categories[i], centerX - fm.stringWidth(categories[i]) / 2, bottom + 24);
        }

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1.5f));
        g.drawRect(left, top, plotW, plotH);

        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        fm = g.getFontMetrics();
        g.drawString(title, (width - fm.stringWidth(title)) / 2, 36);

        int legendX = left + 20;
        int legendY = top + 18;
        g.setColor(new Color(255, 255, 255, 235));
        g.fillRect(legendX - 8, legendY - 14, 170, 58);
        g.setColor(Color.LIGHT_GRAY);
        g.drawRect(legendX - 8, legendY - 14, 170, 58);

        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(kColor);
        g.fillRect(legendX, legendY - 6, 16, 12);
        g.setColor(Color.BLACK);
        g.drawString("Kruskal MST Ops", legendX + 24, legendY + 5);

        g.setColor(pColor);
        g.fillRect(legendX, legendY + 18, 16, 12);
        g.setColor(Color.BLACK);
        g.drawString("Prim MST Ops", legendX + 24, legendY + 29);

        g.dispose();
        ImageIO.write(image, "png", new File(filePath));
    }

    private static void warmupJIT() {
        Graph gUndir = GraphGenerator.generateConnectedUndirectedGraph(200, 800, 999L);
        Graph gDag = GraphGenerator.generateDAG(200, 800, 999L);
        Graph gCyc = GraphGenerator.generateDirectedCyclicGraph(200, 800, 999L);
        for (int i = 0; i < 15; i++) {
            KruskalMST.compute(gUndir);
            PrimMST.compute(gUndir);
            TopologicalSort.sort(gDag);
            TopologicalSort.sort(gCyc);
            TarjanSCC.findSCCs(gCyc);
            Dijkstra.shortestPaths(gCyc, 0);
        }
    }
}
