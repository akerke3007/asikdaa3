# Assignment 3 — Smart City Infrastructure and Route Planning

## Overview

This project is a Java application for a small smart city network. A vertex is a district, an edge is a road or a connection between services, and its weight is a construction cost or a travel distance.

The program has three main uses. Kruskal and Prim find a low-cost network connecting the districts. Topological ordering and strongly connected components help analyze directed connections. Dijkstra finds shortest routes from a selected district.

I used adjacency lists because the experimental graphs have much fewer edges than a complete graph. The graph also keeps one logical edge list. An undirected road appears once in that list and twice in the adjacency lists. Vertex numbers start at 0. The demo gives them names such as Central, University and Hospital. Separate graphs represent construction costs, service dependencies and travel distances, since these weights do not have to mean the same thing.

### Project structure

```text
assignment-3/
├── src/
│   ├── graph/          Edge, Graph, GraphGenerator, Metrics
│   ├── dsu/            DisjointSetUnion
│   ├── mst/            KruskalMST, PrimMST, MstResult
│   ├── connectivity/   KosarajuSCC, TopologicalSort
│   ├── shortestpath/   DijkstraSP
│   ├── benchmark/      Benchmark
│   ├── testing/        GraphTests
│   └── Main.java
├── results/
│   ├── tables/         Raw runs, averages, environment, test and demo output
│   └── plots/          Three PNG plots
├── scripts/
│   └── plot_results.py
├── sources.txt
├── requirements.txt
├── Makefile
└── README.md
```

### How to run

Use JDK 17 or newer. The algorithms, demo, tests and benchmark use only the Java standard library. Python 3.10+ and Matplotlib are needed only to recreate the plots and refresh the report tables.

Run these commands from the project root:

```bash
javac --release 17 -Xlint:all -Werror -d out "@sources.txt"
java -cp out Main demo
java -cp out Main demo 2
java -cp out Main test
java -Xms256m -Xmx256m -cp out Main benchmark
python -m pip install -r requirements.txt
python scripts/plot_results.py
```

`demo 2` selects district 2 as the shortest-path source. Valid demo sources are 0 to 5. Running `Main` without arguments also starts the demo. In IntelliJ IDEA, open the project, choose JDK 17+, and run `Main`. The existing module marks `src` as the source folder.

If `make` is installed, `make reproduce` runs compilation, tests, benchmarks and plotting. `make demo` runs the example. On a runtime that contains the compiler module but has no `javac` launcher, replace `javac` with `java --module jdk.compiler/com.sun.tools.javac.Main`.

The generator reproduces the same graphs and operation counts. Execution times will change between machines and runs. The plotting script rebuilds the tables, discussion and images in this README from `averages.csv`. Run the benchmark before that script when new measurements are needed.

## Implementation

### Graph and results

`Graph` stores directed or undirected weighted edges. It checks vertex numbers and rejects NaN and infinite weights. Graphs must have at least one vertex. Parallel edges and self-loops are supported by the algorithms, although the experimental generators do not create them. MST methods reject directed graphs, while SCC and topological ordering require directed graphs.

Each call creates fresh working arrays and counters. An algorithm does not change the input graph. Small Java records store edges and some result values. Returned edge lists, component lists and topological orders cannot be changed by the caller.

### DSU and Kruskal

DSU starts with one set for every vertex. `find` follows parent links and then compresses the path to its root. `union` joins two different roots and attaches the smaller set to the larger set. It returns false if both vertices are already in the same set.

Kruskal sorts a copy of the edge list by weight. It accepts an edge only when DSU joins two different components, so the chosen edges cannot form a cycle. For a connected graph it stops at V-1 chosen edges. The lightest available edge connecting separate components is safe to select, which is the main reason this greedy method gives an MST.

The result contains the selected edges, their total weight and the remaining component count. A disconnected graph returns a minimum spanning forest and `connected=false`.

### Prim

Prim uses Java's binary-heap `PriorityQueue`. It adds outgoing edges from a visited vertex and repeatedly chooses the lightest edge reaching an unvisited vertex. This edge is a cheapest crossing edge for the current visited set. Edges whose destination is already visited are skipped.

This is a lazy edge-based implementation: several candidate edges can stay in the heap. It does not use decrease-key. When one component finishes, it starts from the next unvisited vertex, so it also returns the full minimum spanning forest on disconnected graphs.

### SCC and topological ordering

Kosaraju first finds DFS finishing order, builds the reversed graph, and then runs DFS in reverse finishing order. Each second-pass search produces one SCC. It uses explicit stacks rather than recursive DFS, so a long chain does not depend on the Java call stack.

Topological ordering uses Kahn's algorithm. It counts indegrees, queues vertices with indegree zero, and removes them while updating their neighbors. An edge's source must be removed before its destination. If fewer than V vertices can be removed, some directed cycle remains. The result then has `hasCycle=true` and an empty order, so a partial order is not reported as a valid answer.

### Dijkstra

Dijkstra starts with distance 0 at the source and infinity elsewhere. It uses a priority queue of vertex-distance pairs. If a shorter route is found, it saves the new distance and predecessor and inserts a new pair. An old pair is skipped if its distance is no longer current. `pathTo` reconstructs a route by following predecessors backwards.

An unreachable district has distance `Infinity` and an empty path. The source has distance 0 and a one-vertex path. All logical edges are checked for negative weights before the search, including edges in unreachable components. The measured time includes this check. A path calculation that exceeds the finite `double` range is rejected rather than silently treated as unreachable.

Dijkstra needs non-negative weights because its greedy argument assumes that extending a path cannot make it shorter. For example, with 0→1 of weight 2, 0→2 of weight 5, and 2→1 of weight -4, the true shortest distance to 1 is 1. A method that finalizes vertex 1 at distance 2 too early gives a wrong answer. Bellman-Ford would be a suitable alternative for negative edges when there is no reachable negative cycle. This project rejects negative weights in Dijkstra; MST algorithms can still use them.

## Complexity Analysis

The table gives extra space used by each algorithm, excluding the input graph but including working structures and returned results. The adjacency-list graph itself uses O(V+E) space. The heap is Java's binary heap.

| Algorithm | Time | Extra space | Reason |
| --- | --- | --- | --- |
| DSU initialization | O(V) | O(V) | Parent and size arrays contain V entries. |
| DSU find and union | O(alpha(V)) amortized per call | O(1) per call, O(V) stored DSU | Path compression and union by size keep repeated searches short. |
| Kruskal | O(V + E log(E+1) + E alpha(V)) | O(V+E) | Copies and sorts E edges, then uses DSU. |
| Lazy Prim | O(V + E log(E+1)) | O(V+E) | Scans adjacency lists and can store O(E) candidate edges in the heap. |
| Kosaraju SCC | O(V+E) | O(V+E) | Two DFS passes, a reversed graph, stacks and the component output. |
| Kahn topological ordering | O(V+E) | O(V) | Indegrees, a queue and the returned order. |
| Lazy Dijkstra | O(V + E log(E+1)) | O(V+E) | Validates edges, initializes arrays and processes heap entries after successful relaxations. |

Here alpha is the inverse Ackermann function, which grows very slowly. The DSU bound is amortized over a sequence of calls; it does not mean every individual call takes exactly constant time. Union by size alone limits tree height to O(log V), and compression improves later finds.

For simple graphs, E is at most proportional to V², so log E is O(log V). Prim is then commonly written as O(V + E log V), and Dijkstra as O((V+E) log V). I kept log E in the general bounds because this graph class allows parallel edges and the lazy heaps can contain O(E) entries. An indexed decrease-key heap would give a different space bound.

Adding vertices increases the size of arrays, output lists and vertex traversals. Adding roads increases sorting, adjacency scans and possible heap updates. In Kruskal, scanning can stop early on a connected graph, but every edge still participates in sorting. SCC and topological ordering have linear worst-case work in V+E. Dijkstra only relaxes edges from reachable vertices, but its input validation still scans all E logical edges.

## Experimental Setup

| Graph size | Vertices (V) | Logical edges (E) |
| --- | --- | --- |
| Small | 10 | 30 |
| Medium | 100 | 500 |
| Large | 1000 | 5000 |

All graphs use `java.util.Random` with fixed seeds. Weights are integers from 1 to 100 stored as `double`. For these sizes the integer costs and distances fit exactly in this representation. Repeated weights are allowed. Each generator produces exactly the requested E, with no self-loops or repeated endpoint pairs.

- **Undirected, seed 42:** first connect each vertex i>0 to a random earlier vertex. This gives a spanning tree. Then add unused random unordered pairs until there are E roads. This guarantees connectivity for both MST methods and the undirected Dijkstra experiment.
- **DAG, seed 43:** choose random distinct endpoints and direct every edge from the smaller vertex number to the larger one. A cycle is impossible because vertex numbers strictly increase along every edge. Some districts can be isolated. This graph is used for SCC and successful topological ordering.
- **Cyclic directed graph, seed 44:** split vertices into blocks of five and create a directed ring inside each block. Additional edges can stay inside a block or go from an earlier block to a later block. They cannot return to an earlier block. Therefore each block is exactly one SCC, giving V/5 SCCs. This graph tests SCC, failed topological ordering and directed Dijkstra.

The three sizes use the same seed for each family. They are separate generated graphs, not repeated samples from different seeds. Dijkstra always starts at vertex 0. Graph fingerprints are saved in `environment.txt` to help check reproducibility.

Each of the 24 algorithm/graph/size cases receives 100 unmeasured warm-up calls, followed by exactly 5 measured calls. The order rotates between rounds to reduce a fixed ordering advantage. `System.nanoTime()` measures the complete algorithm call, including its working allocations and counters. Graph generation, correctness checks, result summaries, file writing and plotting are outside the timed region. Raw nanoseconds are retained; report times are arithmetic means in microseconds, where 1000 ns = 1 us. No measured run is discarded.

There are 120 raw measurements. The averages file also records minimum, maximum and sample standard deviation. Counters and results are checked to be identical across each case's five repetitions. The benchmark checks matching connected MST costs, valid DAG orders, cycle detection, SCC membership and expected SCC counts after measurement.

The checked-in run used OpenJDK 17.0.20 on Linux amd64 with an Intel Xeon Platinum 8573C CPU and a fixed 256 MiB Java heap. The runtime reported 8 available processors; the algorithms run on one thread. The exact timestamp, VM and environment are in [environment.txt](results/tables/environment.txt).

### What the operation counters mean

| Counter | Definition |
| --- | --- |
| Edge examinations | Kruskal's candidate edges; Prim/Dijkstra adjacency entries; SCC edges in both DFS passes and reversal; topological edges in indegree and removal passes. |
| Comparisons | Calls to the weight/distance comparator in sorting or a priority queue. It does not count every conditional statement. |
| Find and union calls | Public DSU method calls. Each union makes two finds. |
| Parent traversals | Each parent-link step in the root-finding loop and again in the compression loop. |
| Parent updates | Actual changed parent entries, including one when two roots are joined. |
| Queue offers and polls | Heap insertions/removals for Prim and Dijkstra; ordinary queue operations for Kahn. |
| Vertex visits | Vertices processed by the traversal; SCC counts both DFS passes. |
| Relaxations | Successful improvements to a Dijkstra distance. |
| Validation edges | Dijkstra's initial scan of all logical edges for negative weights, recorded separately from route scans. |

These counters describe different steps, so adding all of them would not give a fair universal operation total. The tables compare them separately. Operation counts are per run and are identical in all five repetitions, so their average equals the displayed integer count.

## Results

The following tables and plots come from the actual Java benchmark. Full data is available in [raw_runs.csv](results/tables/raw_runs.csv), [averages.csv](results/tables/averages.csv) and [summary.md](results/tables/summary.md).

<!-- RESULTS_START -->

### MST time and total cost

| V | E | Kruskal (us) | Prim (us) | MST cost | Costs match |
| --- | --- | --- | --- | --- | --- |
| 10 | 30 | 7.014 | 6.405 | 129.0 | Yes |
| 100 | 500 | 296.727 | 142.124 | 1170.0 | Yes |
| 1000 | 5000 | 929.478 | 1572.356 | 13123.0 | Yes |

### MST operation counts

| V | Algorithm | Edge scans | Comparisons | Heap offers | Heap polls |
| --- | --- | --- | --- | --- | --- |
| 10 | Kruskal | 14 | 107 | 0 | 0 |
| 10 | Prim | 60 | 203 | 30 | 30 |
| 100 | Kruskal | 164 | 3827 | 0 | 0 |
| 100 | Prim | 1000 | 7697 | 500 | 500 |
| 1000 | Kruskal | 3356 | 50369 | 0 | 0 |
| 1000 | Prim | 10000 | 110958 | 5000 | 5000 |

### DSU operations inside Kruskal

| V | Union calls | Find calls | Parent traversals | Parent updates |
| --- | --- | --- | --- | --- |
| 10 | 14 | 28 | 32 | 11 |
| 100 | 164 | 328 | 508 | 149 |
| 1000 | 3356 | 6712 | 12334 | 1671 |

### SCC and topological ordering

| V | Graph | SCC (us) | SCC scans | SCC count | Topological (us) | Topo scans | Cycle found |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 10 | DAG | 202.491 | 90 | 10 | 2.401 | 60 | No |
| 10 | Cyclic | 9.053 | 90 | 2 | 0.808 | 30 | Yes |
| 100 | DAG | 76.852 | 1500 | 100 | 12.029 | 1000 | No |
| 100 | Cyclic | 33.207 | 1500 | 20 | 1.703 | 500 | Yes |
| 1000 | DAG | 499.540 | 15000 | 1000 | 261.128 | 10000 | No |
| 1000 | Cyclic | 641.655 | 15000 | 200 | 14.973 | 5000 | Yes |

### Dijkstra from district 0

| V | Graph | Time (us) | Reachable | Edge scans | Relaxations | Comparisons | Heap offers | Validation scans |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 10 | Undirected | 6.621 | 10 | 60 | 17 | 82 | 18 | 30 |
| 10 | Cyclic | 2.534 | 10 | 30 | 10 | 21 | 11 | 30 |
| 100 | Undirected | 48.672 | 100 | 1000 | 199 | 2403 | 200 | 500 |
| 100 | Cyclic | 21.300 | 95 | 469 | 119 | 1020 | 120 | 500 |
| 1000 | Undirected | 649.715 | 1000 | 10000 | 1986 | 36698 | 1987 | 5000 |
| 1000 | Cyclic | 310.546 | 975 | 4766 | 1180 | 16842 | 1181 | 5000 |

### Timing variation across the five runs

| Algorithm | Graph | V | Mean (us) | Min (us) | Max (us) | Sample SD (us) |
| --- | --- | --- | --- | --- | --- | --- |
| Kruskal | Undirected | 10 | 7.014 | 5.741 | 7.988 | 0.807 |
| Prim | Undirected | 10 | 6.405 | 4.368 | 11.701 | 3.155 |
| Kosaraju | DAG | 10 | 202.491 | 10.539 | 966.148 | 426.898 |
| Kosaraju | Cyclic | 10 | 9.053 | 5.404 | 13.695 | 3.707 |
| Topological | DAG | 10 | 2.401 | 2.147 | 2.929 | 0.304 |
| Topological | Cyclic | 10 | 0.808 | 0.512 | 1.855 | 0.586 |
| Dijkstra | Undirected | 10 | 6.621 | 4.820 | 9.595 | 1.810 |
| Dijkstra | Cyclic | 10 | 2.534 | 1.469 | 6.030 | 1.962 |
| Kruskal | Undirected | 100 | 296.727 | 69.839 | 1085.086 | 443.259 |
| Prim | Undirected | 100 | 142.124 | 79.516 | 363.266 | 124.097 |
| Kosaraju | DAG | 100 | 76.852 | 38.588 | 218.410 | 79.177 |
| Kosaraju | Cyclic | 100 | 33.207 | 30.548 | 39.254 | 3.489 |
| Topological | DAG | 100 | 12.029 | 11.139 | 12.877 | 0.657 |
| Topological | Cyclic | 100 | 1.703 | 0.981 | 2.518 | 0.725 |
| Dijkstra | Undirected | 100 | 48.672 | 36.773 | 92.745 | 24.644 |
| Dijkstra | Cyclic | 100 | 21.300 | 20.501 | 23.133 | 1.057 |
| Kruskal | Undirected | 1000 | 929.478 | 725.810 | 1106.529 | 175.462 |
| Prim | Undirected | 1000 | 1572.356 | 1145.284 | 2106.762 | 482.793 |
| Kosaraju | DAG | 1000 | 499.540 | 405.768 | 681.536 | 127.986 |
| Kosaraju | Cyclic | 1000 | 641.655 | 314.663 | 1487.070 | 482.542 |
| Topological | DAG | 1000 | 261.128 | 129.450 | 570.776 | 179.089 |
| Topological | Cyclic | 1000 | 14.973 | 12.312 | 20.900 | 3.565 |
| Dijkstra | Undirected | 1000 | 649.715 | 493.257 | 938.127 | 194.010 |
| Dijkstra | Cyclic | 1000 | 310.546 | 246.860 | 396.911 | 63.375 |

### Plots

![Execution time versus vertices](results/plots/time_vs_vertices.png)

![Execution time versus edges](results/plots/time_vs_edges.png)

![Operation counts](results/plots/operation_counts.png)

## Comparison and Discussion

Both MST algorithms gave the same cost at every size. On the large graph, Kruskal took 929.478 us and Prim took 1572.356 us, so Kruskal was faster in this run. Kruskal stops once it has V-1 edges, although it still sorts the whole edge list. This lazy version of Prim scans both directions of every road and later removes old heap entries. The comparison counts show that the two methods do different amounts of heap and sorting work. These results describe this implementation and these sparse graphs; they do not establish a winner for every graph.

Union by size attaches the smaller DSU tree to the larger one, which keeps the height small. Path compression points vertices directly at the root after a find. A later find therefore follows fewer links. Without either improvement, a poor union order can create a chain and one find can take O(V). With both improvements, a sequence of operations takes almost constant time per operation: O(alpha(V)) amortized. The DSU table records the work inside Kruskal; it is not a separate comparison with an unoptimized DSU. The test suite also checks that a repeated find on a deeper tree uses fewer parent traversals.

Kosaraju examines exactly 3E edges and visits each vertex twice. On cyclic graphs, its mean time grew by about 70.9 times from small to large, while V+E grew from 40 to 6000 (150 times). The work count agrees with O(V+E); elapsed time does not have to follow the same exact ratio. Allocation costs, cache effects and timing variation also affect these small measurements. The DAG has V singleton components, while the cyclic graph has V/5 components, so the number of output lists differs.

Topological ordering scans 2E edges on the DAG: one pass for indegrees and one while removing vertices. Every vertex in the cyclic benchmark has a predecessor inside its five-vertex ring. The initial queue is empty, so it detects failure after the E-edge indegree pass. Its lower time is caused by this input structure. A different cyclic graph can allow a large acyclic prefix to be processed first.

Dijkstra scans 2E adjacency entries on the connected undirected graphs, plus E logical edges during weight validation. On the directed graphs it scans outgoing edges only from reachable districts, while validation still checks all E edges. The reachable counts explain some of the difference between the two graph families. Heap comparisons and successful relaxations grow with graph size. Old heap entries are skipped when their stored distance no longer matches the best distance.

These graphs grow in both V and E at the same time. The two required plots show the same measurements against different horizontal axes, so they cannot isolate the effect of V from the effect of E. Also, E/V is 3 for the small graph and 5 for the other two. A larger study could vary the number of roads while keeping V fixed.

Each average uses all five measured runs. The tables include the minimum, maximum and sample standard deviation, and the plots show the minimum-to-maximum range. These are short runs in one shared environment, so scheduling, garbage collection and JVM compilation can affect the result. Operation counts are easier to reproduce than exact times. Counters are enabled during timing, so the measured times also include their small instrumentation cost.

One example of timing variation is Kosaraju on the small DAG: its mean was 202.491 us but its minimum was 10.539 us and its maximum was 966.148 us. Its mean was higher than the medium DAG mean. This does not mean a larger graph needs less theoretical work; the scan counts still grow from 90 to 1500. No run was removed to make the results look smoother.

<!-- RESULTS_END -->

## Testing

The test program uses explicit checks that throw `AssertionError` on failure. It does not need JUnit or the `-ea` option. All 17 groups passed with 65,665 assertions. The saved output is in [test_results.txt](results/tables/test_results.txt).

| Test case | What was checked |
| --- | --- |
| Known small graph | Both MST costs equal the manually calculated value 4. |
| Disconnected graph | Both algorithms return a full forest with cost 7 and three components, including an isolated vertex. An edgeless graph is also tested. |
| Equal weights and multiple MSTs | Different valid edge choices are allowed; both methods return cost 3. Parallel edges are included. |
| Negative and zero MST weights | MST methods select the right cost and ignore a negative self-loop. |
| Single vertex | MST cost is 0, connectivity is true, SCC contains that vertex, topological order is valid, and Dijkstra gives distance 0 with path [0]. |
| DAG | Every vertex occurs once and every edge goes forward in the returned order. Isolated vertices are included. |
| Directed cycles | A normal cycle, a mixed graph with an acyclic part, and a self-loop make a full topological order impossible. |
| Known SCCs | The expected partition is {0,1,2}, {3,4}, {5}, {6}. Every vertex occurs exactly once. |
| Known shortest paths | From 0, distances are [0,3,1,4,7,Infinity], with path [0,2,1,3,4] to vertex 4. |
| Unreachable vertices | The distance stays infinite and the returned path is empty. A different source is also tested. |
| Zero weights and stale entries | Zero-cost cycles, parallel edges and old heap entries do not give wrong distances or cyclic reconstructed paths. |
| Invalid inputs | Reject negative Dijkstra weights even in an unreachable component, invalid vertices, nonfinite weights and the wrong directedness. |
| Generated graphs | Exact sizes, weight bounds, no repeated endpoint pairs, fixed-seed equality, connected MST input and expected directed behavior. |
| Repeated calls | Counters reset and Kruskal does not reorder the graph's original edges. |
| 50 random small MST graphs | Compare both methods against exhaustive enumeration of all candidate spanning trees; check chosen edges and absence of cycles. |
| 60 random directed graphs | Compare Dijkstra from every source with Floyd-Warshall. Compare SCC membership for every vertex pair with mutual reachability. Also verify path endpoints, edges and total costs. |
| 40 shuffled-label DAGs | Check edge order even when valid vertex order is not the numeric order. |
| 20,000-vertex chain | Iterative SCC and topological ordering handle the chain without recursive DFS. |

The city demo is another small example. Both MST methods return cost 11. Dijkstra prints each destination's distance and complete path, and marks Airport as unreachable from source 0. [demo_output.txt](results/tables/demo_output.txt) contains the actual console output.

## Conclusion

The program covers construction planning, directed connectivity and route planning using the required graph algorithms. Kruskal and Prim agree on the minimum costs. DSU makes Kruskal's connectivity checks efficient. Kosaraju finds groups of mutually reachable locations, while topological ordering works for service dependencies only when there is no directed cycle. Dijkstra gives routes and distances when road weights are non-negative.

For an edge-list construction problem, Kruskal is a straightforward choice. Prim is useful when the graph is already stored as adjacency lists. Their practical speed also depends on the heap implementation and graph structure, so I would compare them on the actual network rather than assume one is always faster. SCC and topological ordering are linear tools for directed connectivity. Dijkstra is suitable for repeated non-negative route calculations from a chosen source.

The tests support correctness on the required edge cases, and the raw measurements make the experiment checkable. The main limitation is that only three fixed graph sizes and five measured runs per case were used. More densities, seeds and repeated JVM sessions would give a stronger performance comparison.

### Criteria coverage

| Criterion | Where it is covered |
| --- | --- |
| DSU — 10 | Path compression, union by size, counters and tests. |
| Kruskal and Prim — 20 | Costs, selected edges, connectivity and forest handling; matched on all experiment sizes. |
| SCC and topological ordering — 15 | Iterative Kosaraju, Kahn, DAG validation and cycle detection. |
| Dijkstra — 15 | Distances, reconstructed paths, source selection, unreachable vertices and negative-weight rejection. |
| Complexity analysis — 10 | Time, extra space, adjacency-list assumptions and lazy binary-heap bounds. |
| Experimental design — 10 | Exact sizes, fixed seeds, warm-up, five timed runs and defined counters. |
| Results and discussion — 10 | Raw/average tables, timing variation, three plots and interpretation. |
| Testing — 5 | Required edge cases plus independent exhaustive and Floyd-Warshall checks. |
| README and GitHub quality — 5 | Complete report, reproducible commands, organized packages and separate development commits. |
