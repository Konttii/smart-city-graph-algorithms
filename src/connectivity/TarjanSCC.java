package connectivity;

import graph.Edge;
import graph.Graph;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public class TarjanSCC {

    public static class Result {
        private final List<List<Integer>> components;
        private final int[] componentId;
        private final long executionTimeNanos;
        private final long dfsCalls;
        private final long edgeExaminations;
        private final long stackOperations;

        public Result(List<List<Integer>> components, int[] componentId, long executionTimeNanos,
                      long dfsCalls, long edgeExaminations, long stackOperations) {
            this.components = Collections.unmodifiableList(components);
            this.componentId = componentId;
            this.executionTimeNanos = executionTimeNanos;
            this.dfsCalls = dfsCalls;
            this.edgeExaminations = edgeExaminations;
            this.stackOperations = stackOperations;
        }

        public List<List<Integer>> getComponents() {
            return components;
        }

        public int[] getComponentId() {
            return componentId;
        }

        public long getExecutionTimeNanos() {
            return executionTimeNanos;
        }

        public long getDfsCalls() {
            return dfsCalls;
        }

        public long getEdgeExaminations() {
            return edgeExaminations;
        }

        public long getStackOperations() {
            return stackOperations;
        }

        public long getTotalOperations() {
            return dfsCalls + edgeExaminations + stackOperations;
        }
    }

    private final Graph graph;
    private final int[] ids;
    private final int[] low;
    private final boolean[] onStack;
    private final Deque<Integer> stack;
    private final List<List<Integer>> components;
    private final int[] componentId;
    private int currentId;
    private long dfsCalls;
    private long edgeExaminations;
    private long stackOperations;

    private TarjanSCC(Graph graph) {
        this.graph = graph;
        int n = graph.getVertices();
        this.ids = new int[n];
        Arrays.fill(this.ids, -1);
        this.low = new int[n];
        this.onStack = new boolean[n];
        this.stack = new ArrayDeque<>();
        this.components = new ArrayList<>();
        this.componentId = new int[n];
        Arrays.fill(this.componentId, -1);
        this.currentId = 0;
    }

    public static Result findSCCs(Graph graph) {
        long startTime = System.nanoTime();
        TarjanSCC solver = new TarjanSCC(graph);
        int n = graph.getVertices();
        for (int i = 0; i < n; i++) {
            if (solver.ids[i] == -1) {
                solver.dfs(i);
            }
        }
        long elapsed = System.nanoTime() - startTime;
        return new Result(
                solver.components,
                solver.componentId,
                elapsed,
                solver.dfsCalls,
                solver.edgeExaminations,
                solver.stackOperations
        );
    }

    private void dfs(int at) {
        dfsCalls++;
        stack.push(at);
        onStack[at] = true;
        stackOperations++;
        ids[at] = low[at] = currentId++;

        for (Edge edge : graph.getNeighbors(at)) {
            edgeExaminations++;
            int to = edge.getTo();
            if (ids[to] == -1) {
                dfs(to);
                low[at] = Math.min(low[at], low[to]);
            } else if (onStack[to]) {
                low[at] = Math.min(low[at], ids[to]);
            }
        }

        if (ids[at] == low[at]) {
            List<Integer> scc = new ArrayList<>();
            int compIdx = components.size();
            while (true) {
                int node = stack.pop();
                stackOperations++;
                onStack[node] = false;
                componentId[node] = compIdx;
                scc.add(node);
                if (node == at) {
                    break;
                }
            }
            components.add(Collections.unmodifiableList(scc));
        }
    }
}
