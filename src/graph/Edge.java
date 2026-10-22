package graph;

public record Edge(int source, int destination, double weight) {
    @Override
    public String toString() {
        return source + " -> " + destination + " (" + weight + ")";
    }
}
