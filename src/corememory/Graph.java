/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package corememory;

/**
 *
 * @author Wicky
 */
import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPGrowth;
import ca.pfv.spmf.algorithms.sequentialpatterns.gsp_AGP.AlgoGSP;
import ca.pfv.spmf.algorithms.sequentialpatterns.gsp_AGP.items.SequenceDatabase;
import ca.pfv.spmf.algorithms.sequentialpatterns.gsp_AGP.items.creators.AbstractionCreator;
import ca.pfv.spmf.algorithms.sequentialpatterns.gsp_AGP.items.creators.AbstractionCreator_Qualitative;
import ca.pfv.spmf.test.MainTestFPGrowth_saveToFile;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.StyledEditorKit;

import weiss.nonstandard.PriorityQueue;
import weiss.nonstandard.PairingHeap;
import weiss.nonstandard.BinaryHeap;

// Used to signal violations of preconditions for
// various shortest path algorithms.
class GraphException extends RuntimeException {

    public GraphException(String name) {
        super(name);
    }
}

// Represents an edge in the graph.
class Edge {

    public Vertex dest;   // Second vertex in Edge
    public double cost;   // Edge cost

    public Edge(Vertex d, double c) {
        dest = d;
        cost = c;
    }
}

// Represents an entry in the priority queue for Dijkstra's algorithm.
class Path implements Comparable {

    public Vertex dest;   // w
    public double cost;   // d(w)

    public Path(Vertex d, double c) {
        dest = d;
        cost = c;
    }

    public int compareTo(Object rhs) {
        double otherCost = ((Path) rhs).cost;

        return cost < otherCost ? -1 : cost > otherCost ? 1 : 0;
    }
}

// Represents a vertex in the graph.
class Vertex {

    public String name;   // Vertex name
    public List adj;    // Adjacent vertices
    public double dist;   // Cost
    public Vertex prev;   // Previous vertex on shortest path
    public int scratch;// Extra variable used in algorithm

    public Vertex(String nm) {
        name = nm;
        adj = new LinkedList();
        reset();
    }

    public void reset() {
        dist = Graph.INFINITY;
        prev = null;
        pos = null;
        scratch = 0;
    }
    public PriorityQueue.Position pos;  // Used for dijkstra2 (Chapter 23)
}

// Graph class: evaluate shortest paths.
//
// CONSTRUCTION: with no parameters.
//
// ******************PUBLIC OPERATIONS**********************
// void addEdge( String v, String w, double cvw )
//                              --> Add additional edge
// void printPath( String w )   --> Print path after alg is run
// void unweighted( String s )  --> Single-source unweighted
// void dijkstra( String s )    --> Single-source weighted
// void negative( String s )    --> Single-source negative weighted
// void acyclic( String s )     --> Single-source acyclic
// ******************ERRORS*********************************
// Some error checking is performed to make sure graph is ok,
// and to make sure graph satisfies properties needed by each
// algorithm.  Exceptions are thrown if errors are detected.
public class Graph {

    public static final double INFINITY = Double.MAX_VALUE;
    private Map vertexMap = new HashMap(); // Maps String to Vertex
    private static String path = "", sysTime = "" + System.currentTimeMillis();
    private static BufferedWriter pathsFWriter;

    /**
     * Add a new edge to the graph.
     */
    public void addEdge(String sourceName, String destName, double cost, boolean isDirected) {
        Vertex v = getVertex(sourceName);
        Vertex w = getVertex(destName);
        v.adj.add(new Edge(w, cost));
        if (!isDirected) {
            w.adj.add(new Edge(v, cost));
        }
    }

    /**
     * Driver routine to handle unreachables and print total cost. It calls
     * recursive routine to print shortest path to destNode after a shortest
     * path algorithm has run.
     */
    public void printPath(String destName) {
        Vertex w = (Vertex) vertexMap.get(destName);
        if (w == null) {
            //throw new NoSuchElementException("Destination vertex not found");
        } else if (w.dist == INFINITY) {
            //System.out.println(destName + " is unreachable");
        } else {
            //System.out.print("(Cost is: " + w.dist + ") ");
            //path = path + w.dist + "\t";
            printPath(w);
            path += "\n";
            //System.out.print(path);
            writePath();
            //System.out.println();
        }
    }

    public void writePath() {
        try {

            //writing un-biased paths (wihtout source and destination vertices)
            if (path.indexOf(" ") != path.lastIndexOf(" ")) {
                path = path.substring(path.indexOf(" ") + 1, path.lastIndexOf(" "));
                path += "\n";
                pathsFWriter.write(path);
            }
            //writing paths including source and destinations
//            pathsFWriter.write(path);
            path = "";
            //pathsFile.close();
            //fout.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Graph.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    /**
     * If vertexName is not present, add it to vertexMap. In either case, return
     * the Vertex.
     */
    private Vertex getVertex(String vertexName) {
        Vertex v = (Vertex) vertexMap.get(vertexName);
        if (v == null) {
            v = new Vertex(vertexName);
            vertexMap.put(vertexName, v);
        }
        return v;
    }

    /**
     * Recursive routine to print shortest path to dest after running shortest
     * path algorithm. The path is known to exist.
     */
    private void printPath(Vertex dest) {
        if (dest.prev != null) {
            printPath(dest.prev);
            //System.out.print(" to ");
            path += " ";
        }
        //System.out.print(dest.name);
        path += dest.name;
    }

    /**
     * Initializes the vertex output info prior to running any shortest path
     * algorithm.
     */
    private void clearAll() {
        for (Iterator itr = vertexMap.values().iterator(); itr.hasNext();) {
            ((Vertex) itr.next()).reset();
        }
    }

    /**
     * Single-source unweighted shortest-path algorithm.
     */
    public void unweighted(String startName) {
        clearAll();

        Vertex start = (Vertex) vertexMap.get(startName);
        if (start == null) {
            throw new NoSuchElementException("Start vertex not found");
        }

        LinkedList q = new LinkedList();
        q.addLast(start);
        start.dist = 0;

        while (!q.isEmpty()) {
            Vertex v = (Vertex) q.removeFirst();

            for (Iterator itr = v.adj.iterator(); itr.hasNext();) {
                Edge e = (Edge) itr.next();
                Vertex w = e.dest;
                if (w.dist == INFINITY) {
                    w.dist = v.dist + 1;
                    w.prev = v;
                    q.addLast(w);
                }
            }
        }
    }

    /**
     * Single-source weighted shortest-path algorithm.
     */
    public void dijkstra(String startName) {
        PriorityQueue pq = new BinaryHeap();

        Vertex start = (Vertex) vertexMap.get(startName);
        if (start == null) {
            throw new NoSuchElementException("Start vertex not found");
        }

        clearAll();
        pq.insert(new Path(start, 0));
        start.dist = 0;

        int nodesSeen = 0;
        while (!pq.isEmpty() && nodesSeen < vertexMap.size()) {
            Path vrec = (Path) pq.deleteMin();
            Vertex v = vrec.dest;
            if (v.scratch != 0) // already processed v
            {
                continue;
            }

            v.scratch = 1;
            nodesSeen++;

            for (Iterator itr = v.adj.iterator(); itr.hasNext();) {
                Edge e = (Edge) itr.next();
                Vertex w = e.dest;
                double cvw = e.cost;

                if (cvw < 0) {
                    throw new GraphException("Graph has negative edges");
                }

                if (w.dist > v.dist + cvw) {
                    w.dist = v.dist + cvw;
                    w.prev = v;
                    pq.insert(new Path(w, w.dist));
                }
            }
        }
    }

    /**
     * Single-source weighted shortest-path algorithm using pairing heaps.
     */
    public void dijkstra2(String startName) {
        PriorityQueue pq = new PairingHeap();

        Vertex start = (Vertex) vertexMap.get(startName);
        if (start == null) {
            throw new NoSuchElementException("Start vertex not found");
        }

        clearAll();
        start.pos = pq.insert(new Path(start, 0));
        start.dist = 0;

        while (!pq.isEmpty()) {
            Path vrec = (Path) pq.deleteMin();
            Vertex v = vrec.dest;

            for (Iterator itr = v.adj.iterator(); itr.hasNext();) {
                Edge e = (Edge) itr.next();
                Vertex w = e.dest;
                double cvw = e.cost;

                if (cvw < 0) {
                    throw new GraphException("Graph has negative edges");
                }

                if (w.dist > v.dist + cvw) {
                    w.dist = v.dist + cvw;
                    w.prev = v;

                    Path newVal = new Path(w, w.dist);
                    if (w.pos == null) {
                        w.pos = pq.insert(newVal);
                    } else {
                        pq.decreaseKey(w.pos, newVal);
                    }
                }
            }
        }
    }

    /**
     * Single-source negative-weighted shortest-path algorithm.
     */
    public void negative(String startName) {
        clearAll();

        Vertex start = (Vertex) vertexMap.get(startName);
        if (start == null) {
            throw new NoSuchElementException("Start vertex not found");
        }

        LinkedList q = new LinkedList();
        q.addLast(start);
        start.dist = 0;
        start.scratch++;

        while (!q.isEmpty()) {
            Vertex v = (Vertex) q.removeFirst();
            if (v.scratch++ > 2 * vertexMap.size()) {
                throw new GraphException("Negative cycle detected");
            }

            for (Iterator itr = v.adj.iterator(); itr.hasNext();) {
                Edge e = (Edge) itr.next();
                Vertex w = e.dest;
                double cvw = e.cost;

                if (w.dist > v.dist + cvw) {
                    w.dist = v.dist + cvw;
                    w.prev = v;
                    // Enqueue only if not already on the queue
                    if (w.scratch++ % 2 == 0) {
                        q.addLast(w);
                    } else {
                        w.scratch--;  // undo the enqueue increment    
                    }
                }
            }
        }
    }

    /**
     * Single-source negative-weighted acyclic-graph shortest-path algorithm.
     */
    public void acyclic(String startName) {
        Vertex start = (Vertex) vertexMap.get(startName);
        if (start == null) {
            throw new NoSuchElementException("Start vertex not found");
        }

        clearAll();
        LinkedList q = new LinkedList();
        start.dist = 0;

        // Compute the indegrees
        Collection vertexSet = vertexMap.values();
        for (Iterator vsitr = vertexSet.iterator(); vsitr.hasNext();) {
            Vertex v = (Vertex) vsitr.next();
            for (Iterator witr = v.adj.iterator(); witr.hasNext();) {
                ((Edge) witr.next()).dest.scratch++;
            }
        }

        // Enqueue vertices of indegree zero
        for (Iterator vsitr = vertexSet.iterator(); vsitr.hasNext();) {
            Vertex v = (Vertex) vsitr.next();
            if (v.scratch == 0) {
                q.addLast(v);
            }
        }

        int iterations;
        for (iterations = 0; !q.isEmpty(); iterations++) {
            Vertex v = (Vertex) q.removeFirst();

            for (Iterator itr = v.adj.iterator(); itr.hasNext();) {
                Edge e = (Edge) itr.next();
                Vertex w = e.dest;
                double cvw = e.cost;

                if (--w.scratch == 0) {
                    q.addLast(w);
                }

                if (v.dist == INFINITY) {
                    continue;
                }

                if (w.dist > v.dist + cvw) {
                    w.dist = v.dist + cvw;
                    w.prev = v;
                }
            }
        }

        if (iterations != vertexMap.size()) {
            throw new GraphException("Graph has a cycle!");
        }
    }

    /**
     * Process a request; return false if end of file.
     */
    public static boolean processRequest(BufferedReader in, Graph g) {
        String startName = null;
        String destName = null;
        String alg = null;

        try {
            System.out.print("Enter start node:");
            if ((startName = in.readLine()) == null) {
                return false;
            }
            System.out.print("Enter destination node:");
            if ((destName = in.readLine()) == null) {
                return false;
            }
            System.out.print(" Enter algorithm (u, d, n, a ): ");
            if ((alg = in.readLine()) == null) {
                return false;
            }

            if (alg.equals("u")) {
                g.unweighted(startName);
            } else if (alg.equals("d")) {
                g.dijkstra(startName);
                g.printPath(destName);
                g.dijkstra2(startName);
            } else if (alg.equals("n")) {
                g.negative(startName);
            } else if (alg.equals("a")) {
                g.acyclic(startName);
            }

            g.printPath(destName);
        } catch (IOException e) {
            System.err.println(e);
        } catch (NoSuchElementException e) {
            System.err.println(e);
        } catch (GraphException e) {
            System.err.println(e);
        }
        return true;
    }

    /**
     * A main routine that: 1. Reads a file containing edges (supplied as a
     * command-line parameter); 2. Forms the graph; 3. Repeatedly prompts for
     * two vertices and runs the shortest path algorithm. The data file is a
     * sequence of lines of the format source destination.
     */
    public static void main(String[] args) throws IOException {
        //Create graph object
        String dataset = "toyDU";
        boolean isDirected = true;
        String absolutePath = System.getProperty("user.dir") + "\\datasets\\" + dataset + "\\";
        double minsup = 0.0;
        int seeds = 2000; // number of SSSP using Dijkstra Algorithm
        System.out.println(absolutePath);
        System.out.println("minsup = " + minsup);
        Graph g = new Graph();
        //**********************************************************************
        try {
            //Reading file and populate graph object
            FileReader fin = new FileReader(absolutePath + "edges.txt");
            BufferedReader graphFile = new BufferedReader(fin);

            // Read the edges and insert
            String line;
            System.out.print("\nReading edges...");
            while ((line = graphFile.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line);
                try {
                    String source, dest;
                    if (st.countTokens() == 2) {
                        source = st.nextToken();
                        dest = st.nextToken();
                        g.addEdge(source, dest, 1, isDirected);

                    } else if (st.countTokens() == 3) {
                        source = st.nextToken();
                        dest = st.nextToken();
                        int cost = Integer.parseInt(st.nextToken());
                        g.addEdge(source, dest, cost, isDirected);
                    } else {
                        System.err.println("Skipping ill-formatted line " + line);
                        continue;

                    }
                } catch (NumberFormatException e) {
                    System.err.println("Skipping ill-formatted line " + line);
                }
            }
        } catch (IOException e) {
            System.err.println(e);
        }
        System.out.print("end\n");
        System.out.println(g.vertexMap.size() + " vertices");
        seeds = g.vertexMap.size();
        System.out.println("seeds = " + seeds);
        //**********************************************************************

        FileWriter fout = null;
        BufferedWriter pathsFile = null;
        try {
            fout = new FileWriter(absolutePath + "Paths" + seeds + ".txt", false);
            pathsFile = new BufferedWriter(fout);

            //******************************************************************
            //Write node degree into file
            System.out.print("Wrting degree information...");
            FileWriter dfout = new FileWriter(absolutePath + "degree.txt", false);
            BufferedWriter degreeFile = new BufferedWriter(dfout);
            Iterator it = g.vertexMap.entrySet().iterator();
            //
            int maxDeg = 0;
            // iterate the graph vertex by vertex
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry) it.next();
                Vertex v = (Vertex) pairs.getValue();
                if (maxDeg < (v.adj.size() / 2)) {
                    maxDeg = v.adj.size() / 2;
                }
                //degreeFile.write(pairs.getKey() + " " + (v.adj.size() / 2) + "\n");
                //it.remove(); // avoids a ConcurrentModificationException
            }
            int degCount[] = new int[maxDeg + 1];
            for (int i = 0; i <= maxDeg; i++) {
                degCount[i] = 0;
            }
            // iterate the graph vertex by vertex
            it = g.vertexMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry) it.next();
                Vertex v = (Vertex) pairs.getValue();
                degCount[v.adj.size() / 2] = degCount[v.adj.size() / 2] + 1;
                //degreeFile.write(pairs.getKey() + " " + (v.adj.size() / 2) + "\n");
                //it.remove(); // avoids a ConcurrentModificationException
            }
            degreeFile.write("Degree FractionOfNodes\n");
            for (int i = 0; i <= maxDeg; i++) {
                degreeFile.write(i + " " + degCount[i] + "\n");
            }
            degreeFile.close();
            dfout.close();
            System.out.print("done\n");
            //******************************************************************

        } catch (FileNotFoundException ex) {
            Logger.getLogger(Graph.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException e) {
            System.err.println(e);
        }
        //**********************************************************************
        System.out.print("\nComputing Shortest Paths...");
        g.setFileWriter(pathsFile);
        int count = seeds;
        boolean mark = false;
        Iterator it1 = g.vertexMap.entrySet().iterator();
        Map.Entry pairs1, pairs2;
        while (it1.hasNext() && count > 0) {
            pairs1 = (Map.Entry) it1.next();
            g.dijkstra((String) pairs1.getKey());
            //g.dijkstra2((String)pairs1.getKey());
            Iterator it2 = g.vertexMap.entrySet().iterator();
            mark = false;
            while (it2.hasNext()) {
                pairs2 = (Map.Entry) it2.next();
                if (!pairs2.getKey().equals(pairs1.getKey())) {
                    //the shortest path algorithm from destination to source
                    g.printPath((String) pairs2.getKey());
                }

//                if (!mark && pairs2.getKey().equals(pairs1.getKey())) {
//                    mark = true;
//                } else if (mark && !pairs2.getKey().equals(pairs1.getKey())) {
//                    //the shortest path algorithm from destination to source
//                    g.printPath((String) pairs2.getKey());
//                }
            }
            g.writePath();
            count--;
        }
        pathsFile.close();
        fout.close();
        System.out.print("end\n");
        System.out.println((seeds * (g.vertexMap.size() - 1)) + " paths");
        //**********************************************************************
//        System.out.print("\nTransforming Paths to SubGraphs...");
//        g.Paths2Subgraphs(absolutePath, seeds);
//        System.out.print("end\n");
        //**********************************************************************
//        System.out.print("\nTransforming Paths to a Graph...");
//        g.Paths2Graph(absolutePath, seeds);
//        System.out.print("end\n");
        //**********************************************************************
//        System.out.print("\nPerforming gSpan...");
//        g.ExecuteGSpan(dataset, seeds, minsup);
//        System.out.print("end\n");
        //**********************************************************************
        //mining frequent itemsets to identify the overlaps among all the paths
        System.out.print("\nItemset mining...");
        g.ItemSetMining(absolutePath, minsup, seeds);
        System.out.print("end\n");
        //**********************************************************************
        //mining frequent itemsets to identify the overlaps among all the paths
//        System.out.print("\nSequential Itemset mining...");
//        g.SeqItemSetMining(absolutePath, seeds);
//        System.out.print("end\n");
        //**********************************************************************
        //Re-Writing (1, 2, .... N)itemset count in sequence
        System.out.print("\nRe-writing itemsets count...");
        g.PathSegOverlapCount(absolutePath, minsup, seeds);
        System.out.print("end\n");
        //**********************************************************************
        //Re-Writing Frequent Path Segments in an order
        System.out.print("\nRe-writing frequent path segments...");
        g.FrequentPathSegments(absolutePath, minsup, seeds);
        System.out.print("end\n");

        //**********************************************************************
//        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
//        while (processRequest(in, g))
//             ;
    }

    public void Paths2Graph(String absolutePath, int seeds) {
        try {
            //Reading file and populate graph object
            FileReader fin = new FileReader(absolutePath + "Paths" + seeds + ".txt");
            BufferedReader pathsFile = new BufferedReader(fin);
            FileWriter fout = new FileWriter(absolutePath + "Paths2Graph" + seeds + ".txt", false);
            BufferedWriter sgFile = new BufferedWriter(fout);

            // Read the edges and insert
            Map vMap = new HashMap();
            String line, edgesStr;
            while ((line = pathsFile.readLine()) != null) {
                edgesStr = "";
                StringTokenizer st = new StringTokenizer(line);
                //add edges information
                st = new StringTokenizer(line);
                int vCount = st.countTokens();
                String v1, v2, cost = "";
                v1 = st.nextToken();
                for (int i = 0; i < (vCount - 1); i++) {
                    v2 = st.nextToken();
                    Vertex v = (Vertex) vertexMap.get(v1);
                    List eList = v.adj;
                    Iterator eListIter = eList.iterator();
                    while (eListIter.hasNext()) {
                        Edge e = (Edge) eListIter.next();
                        if (e.dest.name.equals(v2)) {
                            cost = Double.toString(e.cost);
                            break;
                        }
                    }
                    if ((vMap.containsKey(v1) && v2.equals(vMap.get(v1))) || (vMap.containsKey(v2) && v1.equals(vMap.get(v2)))) {
                        // do nothing
                    } else {
                        edgesStr += (v1 + " " + v2 + /*" " + cost +*/ "\n");
                        vMap.put(v1, v2);
                    }
                    v1 = v2;
                }
                sgFile.append(edgesStr);
            }
            vMap.clear();
            sgFile.close();
            fout.close();
            pathsFile.close();
            fin.close();

        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public void ExecuteGSpan(String dataset, int seeds, double minsup) {
        int totalPaths = (vertexMap.size() - 1) * seeds;
        int sup = (int) (minsup * totalPaths);
        final String dir = System.getProperty("user.dir");
        int processExitVal = 0;
        try {
            Process p = Runtime.getRuntime().exec("cmd /c  start gspan.exe " + dataset + " " + seeds + " " + 0/*
                     * sup
                     */ + " 1");
            //processExitVal = p.waitFor();
        } catch (IOException e) {
            System.out.println("IOException");
            e.printStackTrace();
        }
//        catch (InterruptedException e) {
//            System.out.println("InterruptedException");
//            e.printStackTrace();
//        }
        System.out.println(processExitVal);
        System.out.println("Execution complete");

    }

    public void Paths2Subgraphs(String absolutePath, int seeds) {
        try {
            //Reading file and populate graph object
            FileReader fin = new FileReader(absolutePath + "Paths" + seeds + ".txt");
            BufferedReader pathsFile = new BufferedReader(fin);
            FileWriter fout = new FileWriter(absolutePath + "SubGraphs" + seeds + ".txt", false);
            BufferedWriter sgFile = new BufferedWriter(fout);

            // Read the edges and insert
            String line, subGraph;
            int subGraphID = 0;
            while ((line = pathsFile.readLine()) != null) {
                subGraph = "";
                StringTokenizer st = new StringTokenizer(line);
                subGraph += ("t # " + subGraphID + "\n");
                //add vertices information
                int vertexID = 0;
                while (st.hasMoreTokens()) {
                    //Vertex v = (Vertex)vertexMap.get(st.nextToken());
                    subGraph += ("v " + vertexID + " " + st.nextToken() + "\n");
                    vertexID++;
                }
                //add edges information
                st = new StringTokenizer(line);
                int vCount = st.countTokens();
                String v1, v2, cost = "";
                v1 = st.nextToken();
                for (int i = 0; i < (vCount - 1); i++) {
                    v2 = st.nextToken();
                    Vertex v = (Vertex) vertexMap.get(v1);
                    List eList = v.adj;
                    Iterator eListIter = eList.iterator();
                    while (eListIter.hasNext()) {
                        Edge e = (Edge) eListIter.next();
                        if (e.dest.name.equals(v2)) {
                            cost = Double.toString(e.cost);
                            break;
                        }
                    }
                    subGraph += ("e " + i + " " + (i + 1) + " " + cost + "\n");
                    v1 = v2;
                }
                sgFile.append(subGraph);
                subGraphID++;
            }

            sgFile.close();
            fout.close();
            pathsFile.close();
            fin.close();

        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public static int generate(int min, int max) {
        return min + (int) (Math.random() * ((max - min) + 1));
    }

    public void PathSegOverlapCount(String absPath, double minsup, int seeds) {
        try {
            //String input = fileToPath("contextPasquier99.txt");  // the database
            String input = absPath + "FreqItemset_" + Double.toString(minsup) + "_" + seeds + ".txt";

            //For Reading FreqItemset file
            FileReader fin = new FileReader(input);
            BufferedReader fiFile = new BufferedReader(fin);

            //finding max itemset length
            int maxItemsetLen = 0;
            // Read the edges and insert
            String line, segLine;
            while ((line = fiFile.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line);
                if (st.countTokens() > maxItemsetLen) {
                    maxItemsetLen = st.countTokens();
                }
            }
            //
            int maxItemsetCount = 0;

            //for itemset length >= 1
            for (int len = 1; len <= (maxItemsetLen - 2); len++) {
                maxItemsetCount = 0;
                //For Writing Path Segments Overlap Counts
                String output = absPath + "PathSegOverlapCount_MinSup" + Double.toString(minsup) + "_Seed" + seeds + "_Len" + len + ".txt";  // the path for saving the frequent itemsets found
                FileWriter fout = new FileWriter(output, false);
                BufferedWriter psocfile = new BufferedWriter(fout);
                //reading file -- populate maxItemsetCount[] array
                fiFile = new BufferedReader(new FileReader(input));
                while ((line = fiFile.readLine()) != null) {
                    StringTokenizer st = new StringTokenizer(line);
                    segLine = "";
                    if (st.countTokens() == (len + 2)) {
                        for (int token = 1; token <= (len); token++) {
                            if (token == len) {
                                segLine += st.nextToken();
                                st.nextToken();
                                int tempVal = Integer.parseInt(st.nextToken());
                                if (maxItemsetCount < tempVal) {
                                    maxItemsetCount = tempVal;
                                }
                            } else {
                                segLine += (st.nextToken() + "_");
                            }
                        }
                        //write the segLine into file
                        //psocfile.write(segLine);
                    }
                }
                //
                int freqCount[] = new int[maxItemsetCount + 1];
                for (int i = 0; i <= maxItemsetCount; i++) {
                    freqCount[i] = 0;
                }
                //reading file to count transaction against each frequency value (similar to degree values 1, 2, 3 .... for power law plot)
                fiFile = new BufferedReader(new FileReader(input));
                while ((line = fiFile.readLine()) != null) {
                    StringTokenizer st = new StringTokenizer(line);
                    segLine = "";
                    if (st.countTokens() == (len + 2)) {
                        for (int token = 1; token <= (len); token++) {
                            if (token == len) {
                                segLine += st.nextToken();
                                st.nextToken();
                                int tempVal = Integer.parseInt(st.nextToken());
                                freqCount[tempVal] = freqCount[tempVal] + 1;
                                //segLine += (" " + len + " " + st.nextToken() + "\n");
                            } else {
                                segLine += (st.nextToken() + "_");
                            }
                        }
                        //write the segLine into file
                        //psocfile.write(segLine);
                    }
                }
                psocfile.write("Freq FractionOfSegments\n");
                for (int i = 0; i <= maxItemsetCount; i++) {
                    psocfile.write(i + " " + freqCount[i] + "\n");
                }
                psocfile.close();
                fout.close();
            }

            fiFile.close();
            fin.close();

        } catch (FileNotFoundException ex) {
            Logger.getLogger(Graph.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public void FrequentPathSegments(String absPath, double minsup, int seeds) {
        try {
            //String input = fileToPath("contextPasquier99.txt");  // the database
            String input = absPath + "FreqItemset_" + Double.toString(minsup) + "_" + seeds + ".txt";

            //For Reading FreqItemset file
            FileReader fin = new FileReader(input);
            BufferedReader fiFile = new BufferedReader(fin);
            //For Writing Frequent Shortest Path Segements
            String output = absPath + "FreqPathSegs_MinSup" + Double.toString(minsup) + "_Seed" + seeds + ".txt";  // the path for saving the frequent itemsets found
            FileWriter fout = new FileWriter(output, false);
            BufferedWriter psocfile = new BufferedWriter(fout);
            psocfile.write("PathSegment Frequency\n");

            //finding max itemset length
            int maxItemsetLen = 0;
            // Read the edges and insert
            String line, segLine;
            while ((line = fiFile.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line);
                if (st.countTokens() > maxItemsetLen) {
                    maxItemsetLen = st.countTokens();
                }
            }
            //
            int maxItemsetCount = 0;

            //for itemset length >= 1
            for (int len = 1; len <= (maxItemsetLen - 2); len++) {
                maxItemsetCount = 0;
                //reading file -- populate maxItemsetCount[] array
                fiFile = new BufferedReader(new FileReader(input));
                while ((line = fiFile.readLine()) != null) {
                    StringTokenizer st = new StringTokenizer(line);
                    segLine = "";
                    if (st.countTokens() == (len + 2)) {
                        for (int token = 1; token <= (len); token++) {
                            if (token == len) {
                                segLine += st.nextToken();
                                st.nextToken();
                                int tempVal = Integer.parseInt(st.nextToken());
                                segLine += (" " + tempVal + "\n");
//                                if (maxItemsetCount < tempVal) {
//                                    maxItemsetCount = tempVal;
//                                }
                            } else {
                                segLine += (st.nextToken() + "_");
                            }
                        }
                        //write the segLine into file
                        psocfile.write(segLine);
                    }
                }
            }

            psocfile.close();
            fout.close();
            fiFile.close();
            fin.close();

        } catch (FileNotFoundException ex) {
            Logger.getLogger(Graph.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public void ItemSetMining(String absPath, double minsup, int seeds) {
        try {
            //String input = fileToPath("contextPasquier99.txt");  // the database
            //double minsup = 0.0001; // means a minsup of 2 transaction (we used a relative support)
            String output = absPath + "FreqItemset_" + Double.toString(minsup) + "_" + seeds + ".txt";  // the path for saving the frequent itemsets found
            String input = absPath + "Paths" + seeds + ".txt";
            // Applying the FPGROWTH algorithmMainTestFPGrowth.java
            AlgoFPGrowth algo = new AlgoFPGrowth();
            algo.runAlgorithm(input, output, minsup);
            algo.printStats();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Graph.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public void SeqItemSetMining(String absPath, int seeds) {
        // Load a sequence database
        double support = (double) 0.1, mingap = 0, maxgap = Integer.MAX_VALUE, windowSize = 0;
        boolean keepPatterns = true;
        boolean verbose = false;
        String input = absPath + "Paths" + seeds + ".txt";
        AbstractionCreator abstractionCreator = AbstractionCreator_Qualitative.getInstance();
        SequenceDatabase sequenceDatabase = new SequenceDatabase(abstractionCreator);
        try {
            sequenceDatabase.loadFile(input, support);

            AlgoGSP algorithm = new AlgoGSP(support, mingap, maxgap, windowSize, abstractionCreator);

            System.out.println(sequenceDatabase.toString());

            algorithm.runAlgorithm(sequenceDatabase, keepPatterns, verbose, null);

            System.out.println(algorithm.getNumberOfFrequentPatterns() + " frequent pattern found.");

            System.out.println(algorithm.printedOutputToSaveInFile());
        } catch (IOException ex) {
            Logger.getLogger(Graph.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static String fileToPath(String filename) throws UnsupportedEncodingException {
        URL url = MainTestFPGrowth_saveToFile.class.getResource(filename);
        return java.net.URLDecoder.decode(url.getPath(), "UTF-8");
    }

    private void setFileWriter(BufferedWriter pathsFile) {
        pathsFWriter = pathsFile;
    }
}
