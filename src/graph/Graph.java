package graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Graph {
    private final int vertices;
    private final boolean directed;
    private final List<List<Edge>> adjList;
    private final List<Edge> edges;
    private final List<String> districtNames;

    public Graph(int vertices, boolean directed) {
        this.vertices = vertices;
        this.directed = directed;
        this.adjList = new ArrayList<>(vertices);
        this.edges = new ArrayList<>();
        this.districtNames = new ArrayList<>(vertices);
        for (int i = 0; i < vertices; i++) {
            adjList.add(new ArrayList<>());
            districtNames.add("District-" + i);
        }
    }

    public void setDistrictName(int v, String name) {
        if (v >= 0 && v < vertices) {
            districtNames.set(v, name);
        }
    }

    public String getDistrictName(int v) {
        return districtNames.get(v);
    }

    public void addEdge(int from, int to, int weight) {
        if (from < 0 || from >= vertices || to < 0 || to >= vertices) {
            throw new IllegalArgumentException("Vertex index out of bounds");
        }
        Edge forward = new Edge(from, to, weight);
        adjList.get(from).add(forward);
        edges.add(forward);
        if (!directed && from != to) {
            adjList.get(to).add(new Edge(to, from, weight));
        }
    }

    public int getVertices() {
        return vertices;
    }

    public boolean isDirected() {
        return directed;
    }

    public List<Edge> getNeighbors(int v) {
        return Collections.unmodifiableList(adjList.get(v));
    }

    public List<Edge> getEdges() {
        return Collections.unmodifiableList(edges);
    }
}
