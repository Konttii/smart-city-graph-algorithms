package mst;

import graph.Edge;
import java.util.Collections;
import java.util.List;

public class MSTResult {
    private final String algorithmName;
    private final List<Edge> mstEdges;
    private final long totalWeight;
    private final boolean connected;
    private final long executionTimeNanos;
    private final long edgeExaminations;
    private final long comparisons;
    private final long dsuOrHeapOperations;

    public MSTResult(String algorithmName, List<Edge> mstEdges, long totalWeight, boolean connected,
                     long executionTimeNanos, long edgeExaminations, long comparisons, long dsuOrHeapOperations) {
        this.algorithmName = algorithmName;
        this.mstEdges = Collections.unmodifiableList(mstEdges);
        this.totalWeight = totalWeight;
        this.connected = connected;
        this.executionTimeNanos = executionTimeNanos;
        this.edgeExaminations = edgeExaminations;
        this.comparisons = comparisons;
        this.dsuOrHeapOperations = dsuOrHeapOperations;
    }

    public String getAlgorithmName() {
        return algorithmName;
    }

    public List<Edge> getMstEdges() {
        return mstEdges;
    }

    public long getTotalWeight() {
        return totalWeight;
    }

    public boolean isConnected() {
        return connected;
    }

    public long getExecutionTimeNanos() {
        return executionTimeNanos;
    }

    public long getEdgeExaminations() {
        return edgeExaminations;
    }

    public long getComparisons() {
        return comparisons;
    }

    public long getDsuOrHeapOperations() {
        return dsuOrHeapOperations;
    }

    public long getTotalOperations() {
        return edgeExaminations + comparisons + dsuOrHeapOperations;
    }
}
