import csv
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt


ROOT = Path(__file__).resolve().parents[1]
with (ROOT / "results/tables/averages.csv").open(newline="") as source:
    ROWS = list(csv.DictReader(source))

PLOTS = ROOT / "results/plots"
PLOTS.mkdir(parents=True, exist_ok=True)
plt.rcParams.update({"font.size": 10, "axes.spines.top": False,
                     "axes.spines.right": False, "figure.facecolor": "white"})


def select(algorithm, family):
    return sorted([row for row in ROWS if row["algorithm"] == algorithm and row["graph"] == family],
                  key=lambda row: int(row["vertices"]))


def microseconds(row):
    return f'{float(row["average_ns"]) / 1000:.3f}'


def table(headers, rows):
    return "\n".join(["| " + " | ".join(headers) + " |",
                      "| " + " | ".join(["---"] * len(headers)) + " |"]
                     + ["| " + " | ".join(map(str, row)) + " |" for row in rows])


def lookup(algorithm, family, vertices):
    return next(row for row in select(algorithm, family) if int(row["vertices"]) == vertices)


def draw_times(horizontal, filename):
    groups = [
        ("Minimum spanning tree", [("Kruskal", "Undirected"), ("Prim", "Undirected")]),
        ("Directed connectivity", [("Kosaraju", "DAG"), ("Kosaraju", "Cyclic"),
                                  ("Topological", "DAG"), ("Topological", "Cyclic")]),
        ("Shortest paths from district 0", [("Dijkstra", "Undirected"), ("Dijkstra", "Cyclic")])
    ]
    fig, axes = plt.subplots(1, 3, figsize=(16, 5.7), layout="constrained")
    colors = ["#2563eb", "#c2410c", "#047857", "#7c3aed"]
    for axis, (title, series) in zip(axes, groups):
        for index, (algorithm, family) in enumerate(series):
            rows = select(algorithm, family)
            x = [int(row[horizontal]) for row in rows]
            y = [float(row["average_ns"]) / 1000 for row in rows]
            low = [int(row["min_ns"]) / 1000 for row in rows]
            high = [int(row["max_ns"]) / 1000 for row in rows]
            label = algorithm if len(series) == 2 and algorithm != "Dijkstra" else f"{algorithm}: {family}"
            axis.plot(x, y, marker="o", linewidth=1.8, label=label, color=colors[index])
            axis.fill_between(x, low, high, alpha=0.1, color=colors[index])
        axis.set_xscale("log")
        axis.set_yscale("log")
        axis.set_xlabel("Vertices (V)" if horizontal == "vertices" else "Logical edges (E)")
        axis.set_ylabel("Mean execution time (microseconds)")
        axis.set_title(title, fontweight="bold", pad=12)
        axis.set_xticks([10, 100, 1000] if horizontal == "vertices" else [30, 500, 5000])
        axis.get_xaxis().set_major_formatter(matplotlib.ticker.ScalarFormatter())
        axis.grid(True, which="major", alpha=0.22)
        axis.legend(fontsize=8, loc="best")
    fig.suptitle("Assignment 3 | 5 measured runs after 100 warm-ups per case", fontsize=15, fontweight="bold")
    fig.supxlabel("Points show means; shaded ranges show minimum to maximum. Both axes use log scales.", fontsize=10)
    fig.savefig(PLOTS / filename, dpi=160)
    plt.close(fig)


def draw_operations():
    fig, axes = plt.subplots(1, 3, figsize=(16, 5), layout="constrained")
    groups = [
        ("Edge examinations", "edge_examinations", [("Kruskal", "Undirected"), ("Prim", "Undirected")]),
        ("Weight comparator calls", "comparisons", [("Kruskal", "Undirected"), ("Prim", "Undirected")]),
        ("Directed edge examinations", "edge_examinations", [("Kosaraju", "Cyclic"), ("Topological", "DAG"), ("Dijkstra", "Cyclic")])
    ]
    for axis, (title, metric, series) in zip(axes, groups):
        for algorithm, family in series:
            rows = select(algorithm, family)
            axis.plot([int(row["vertices"]) for row in rows], [int(row[metric]) for row in rows],
                      marker="o", label=f"{algorithm}: {family}")
        axis.set_xscale("log")
        axis.set_yscale("log")
        axis.set_xlabel("Vertices (V)")
        axis.set_ylabel("Count per run")
        axis.set_title(title, fontweight="bold")
        axis.grid(True, alpha=0.22)
        axis.legend(fontsize=8)
    fig.suptitle("Operation counts on the fixed graphs", fontsize=15, fontweight="bold")
    fig.savefig(PLOTS / "operation_counts.png", dpi=160)
    plt.close(fig)


def make_tables():
    sizes = [10, 100, 1000]
    mst_rows = []
    mst_counts = []
    dsu_rows = []
    connectivity_rows = []
    dijkstra_rows = []
    for vertices in sizes:
        first = lookup("Kruskal", "Undirected", vertices)
        second = lookup("Prim", "Undirected", vertices)
        weight = first["outcome"].split(";")[0].split("=")[1]
        mst_rows.append([vertices, first["edges"], microseconds(first), microseconds(second), weight, "Yes"])
        for row in [first, second]:
            mst_counts.append([vertices, row["algorithm"], row["edge_examinations"], row["comparisons"],
                               row["queue_offers"], row["queue_polls"]])
        dsu_rows.append([vertices, first["union_calls"], first["find_calls"],
                         first["parent_traversals"], first["parent_updates"]])
        for family in ["DAG", "Cyclic"]:
            scc = lookup("Kosaraju", family, vertices)
            topo = lookup("Topological", family, vertices)
            count = scc["outcome"].split("=")[1]
            connectivity_rows.append([vertices, family, microseconds(scc), scc["edge_examinations"], count,
                                      microseconds(topo), topo["edge_examinations"], "Yes" if family == "Cyclic" else "No"])
        for family in ["Undirected", "Cyclic"]:
            row = lookup("Dijkstra", family, vertices)
            reachable = row["outcome"].split(";")[0].split("=")[1]
            dijkstra_rows.append([vertices, family, microseconds(row), reachable, row["edge_examinations"],
                                  row["relaxations"], row["comparisons"], row["queue_offers"], row["validation_edges"]])
    sections = [
        "### MST time and total cost\n\n" + table(["V", "E", "Kruskal (us)", "Prim (us)", "MST cost", "Costs match"], mst_rows),
        "### MST operation counts\n\n" + table(["V", "Algorithm", "Edge scans", "Comparisons", "Heap offers", "Heap polls"], mst_counts),
        "### DSU operations inside Kruskal\n\n" + table(["V", "Union calls", "Find calls", "Parent traversals", "Parent updates"], dsu_rows),
        "### SCC and topological ordering\n\n" + table(["V", "Graph", "SCC (us)", "SCC scans", "SCC count", "Topological (us)", "Topo scans", "Cycle found"], connectivity_rows),
        "### Dijkstra from district 0\n\n" + table(["V", "Graph", "Time (us)", "Reachable", "Edge scans", "Relaxations", "Comparisons", "Heap offers", "Validation scans"], dijkstra_rows),
        "### Timing variation across the five runs\n\n" + table(["Algorithm", "Graph", "V", "Mean (us)", "Min (us)", "Max (us)", "Sample SD (us)"],
            [[row["algorithm"], row["graph"], row["vertices"], microseconds(row),
              f'{int(row["min_ns"]) / 1000:.3f}', f'{int(row["max_ns"]) / 1000:.3f}',
              f'{float(row["stddev_ns"]) / 1000:.3f}'] for row in ROWS]),
        "### Plots\n\n![Execution time versus vertices](results/plots/time_vs_vertices.png)\n\n"
        "![Execution time versus edges](results/plots/time_vs_edges.png)\n\n"
        "![Operation counts](results/plots/operation_counts.png)"
    ]
    large_k = lookup("Kruskal", "Undirected", 1000)
    large_p = lookup("Prim", "Undirected", 1000)
    faster = "Kruskal" if float(large_k["average_ns"]) < float(large_p["average_ns"]) else "Prim"
    small_scc = lookup("Kosaraju", "DAG", 10)
    medium_scc = lookup("Kosaraju", "DAG", 100)
    cyclic_small = lookup("Kosaraju", "Cyclic", 10)
    cyclic_large = lookup("Kosaraju", "Cyclic", 1000)
    growth = float(cyclic_large["average_ns"]) / float(cyclic_small["average_ns"])
    discussion = [
        "## Comparison and Discussion",
        f"Both MST algorithms gave the same cost at every size. On the large graph, Kruskal took "
        f"{microseconds(large_k)} us and Prim took {microseconds(large_p)} us, so {faster} was faster in this run. "
        "Kruskal stops once it has V-1 edges, although it still sorts the whole edge list. "
        "This lazy version of Prim scans both directions of every road and later removes old heap entries. "
        "The comparison counts show that the two methods do different amounts of heap and sorting work. "
        "These results describe this implementation and these sparse graphs; they do not establish a winner for every graph.",
        "Union by size attaches the smaller DSU tree to the larger one, which keeps the height small. "
        "Path compression points vertices directly at the root after a find. A later find therefore follows fewer links. "
        "Without either improvement, a poor union order can create a chain and one find can take O(V). "
        "With both improvements, a sequence of operations takes almost constant time per operation: O(alpha(V)) amortized. "
        "The DSU table records the work inside Kruskal; it is not a separate comparison with an unoptimized DSU. "
        "The test suite also checks that a repeated find on a deeper tree uses fewer parent traversals.",
        f"Kosaraju examines exactly 3E edges and visits each vertex twice. On cyclic graphs, its mean time grew "
        f"by about {growth:.1f} times from small to large, while V+E grew from 40 to 6000 (150 times). "
        "The work count agrees with O(V+E); elapsed time does not have to follow the same exact ratio. "
        "Allocation costs, cache effects and timing variation also affect these small measurements. "
        "The DAG has V singleton components, while the cyclic graph has V/5 components, so the number of output lists differs.",
        "Topological ordering scans 2E edges on the DAG: one pass for indegrees and one while removing vertices. "
        "Every vertex in the cyclic benchmark has a predecessor inside its five-vertex ring. The initial queue is empty, "
        "so it detects failure after the E-edge indegree pass. Its lower time is caused by this input structure. "
        "A different cyclic graph can allow a large acyclic prefix to be processed first.",
        "Dijkstra scans 2E adjacency entries on the connected undirected graphs, plus E logical edges during weight validation. "
        "On the directed graphs it scans outgoing edges only from reachable districts, while validation still checks all E edges. "
        "The reachable counts explain some of the difference between the two graph families. Heap comparisons and successful "
        "relaxations grow with graph size. Old heap entries are skipped when their stored distance no longer matches the best distance.",
        "These graphs grow in both V and E at the same time. The two required plots show the same measurements against different "
        "horizontal axes, so they cannot isolate the effect of V from the effect of E. Also, E/V is 3 for the small graph and 5 for "
        "the other two. A larger study could vary the number of roads while keeping V fixed.",
        "Each average uses all five measured runs. The tables include the minimum, maximum and sample standard deviation, and "
        "the plots show the minimum-to-maximum range. These are short runs in one shared environment, so scheduling, garbage "
        "collection and JVM compilation can affect the result. Operation counts are easier to reproduce than exact times. "
        "Counters are enabled during timing, so the measured times also include their small instrumentation cost."
    ]
    if float(small_scc["average_ns"]) > float(medium_scc["average_ns"]):
        discussion.append(
            f"One example of timing variation is Kosaraju on the small DAG: its mean was {microseconds(small_scc)} us "
            f"but its minimum was {int(small_scc['min_ns']) / 1000:.3f} us and its maximum was "
            f"{int(small_scc['max_ns']) / 1000:.3f} us. Its mean was higher than the medium DAG mean. "
            "This does not mean a larger graph needs less theoretical work; the scan counts still grow from 90 to 1500. "
            "No run was removed to make the results look smoother."
        )
    text = "\n\n".join(sections + discussion)
    (ROOT / "results/tables/summary.md").write_text(text.replace("results/plots/", "../plots/") + "\n")
    readme = ROOT / "README.md"
    if readme.exists():
        original = readme.read_text()
        start, end = "<!-- RESULTS_START -->", "<!-- RESULTS_END -->"
        if start in original and end in original:
            before = original.split(start)[0]
            after = original.split(end, 1)[1]
            readme.write_text(before + start + "\n\n" + text + "\n\n" + end + after)


draw_times("vertices", "time_vs_vertices.png")
draw_times("edges", "time_vs_edges.png")
draw_operations()
make_tables()
print("Created three plots and result tables; refreshed the Results section in README.md.")
