package stardust;

import java.util.*;
import java.io.*;
import java.nio.*;
import java.text.*;

public class Reconstruct {
  static DecimalFormat df = new DecimalFormat("#.###");
  static int edgeCount = 0;
  public static void main(String args[]) throws IOException {
    if (args.length < 1) {
      System.out.println("Reconstruct <id>");
      return;
    }
    long id = -1;
    try {
      id = Integer.parseInt(args[0]);
    } catch (Exception e) {
      return;
    }
    /* It looks like ID is only used in the dot output text */
    //System.out.println("Reconstruct called with id " +id);
    /* Not sure if need this */
    id = Math.abs(id);
    Graph g = reconstruct(System.in);
    if(validateGraph(g))
      createDot(id, g, System.out, "G");
      //count(id, g);
    else
      System.exit(1);
  }

  public static boolean validateGraph(Graph g) {
    if (g.root.label.indexOf("ENTRY_START") == -1) {
      System.out.println("Error: root node has label " + g.root.label);
      return false;
    } else {
        //System.out.println("Validating graph");
    }
    int dfsN = countNodes(g, g.root, new HashSet<Node>());
    int dfsE = countEdges(g, g.root, new HashSet<Node>());
    int hashN = 0;
    int hashE = 0;
    Node last = null;
    for(Node n : g.nodes.values())
      hashN++;
    /* last node listed should have zero children according to this test */
    for(Node n : g.nodes.values()) {
        //System.out.println(n.toString());
        for(Edge e : n.children) {
            //System.out.println(e.toString());
            hashE++;
        }
        if (n.children.size() == 0) {
            if (last != null) {
                /* ekrevat: skipping this, not sure why it is here */
                //System.out.println("last is not null");
                //return false;
            }
            last = n;
        }
    }
    //System.out.println("Last node is " +last.toString());
    if (dfsN != hashN || dfsE != hashE) {
      System.out.println("dfsN = " + dfsN + " hashN = " + hashN + " dfsE = " + dfsE + " hashE = " + hashE);
      return false;
    }
    int index = g.root.label.indexOf("ENTRY_START");
    //System.out.println("Checking from root " + g.root.label);
    if (index != -1) {
      if (last.label.indexOf(g.root.label.substring(0, index)) == -1 || (last.label.indexOf("END") == -1 && last.label.indexOf("ERROR") == -1))  {
        System.out.println("Error: last node has label " + g.root.label);
        return false;
      }
    } else if (g.root.label.indexOf("ENTRY_START") == -1) {
        System.out.println("Error: root node has label " + g.root.label);
        return false;
    }
    return true;
  }

  public static Graph reconstruct(InputStream in) throws IOException {
    Scanner sc = new Scanner(in);
    Random r = new Random();
    Graph g = new Graph();
    /* Node ID to Node Array mapping */
    HashMap<String, ArrayList<Node>> nodes = new HashMap<String, ArrayList<Node>>();
    /* "From" Node ID to "To" Node IDs */
    HashMap<String, ArrayList<String>> adjList = new HashMap<String, ArrayList<String>>();
    /* List of IDs that will need to be processed to assign parent edges using adjList */
    HashSet<String> idList = new HashSet<String>();
    while(sc.hasNextLine()) {
      String line = sc.nextLine();
      if (line.length() == 0)
        continue;
      if (line.equals("X-Trace Report ver 1.0")) {
        Node n = new Node();
        int i = 0;
        while(sc.hasNextLine()) {
          line = sc.nextLine();
          //System.out.println(line);
          if (line.length() == 0)
            break;
          String[] pair = line.split(":");
          if (pair[0].equals("Agent")) {
            n.agent = pair[1].trim();
          } else if (pair[0].equals("Label")) {
            n.label = pair[1].trim();
            i |= 1;
          } else if (pair[0].equals("Host")) {
            n.hostname = pair[1].trim();
            i |= 2;
          } else if (pair[0].equals("X-Trace")) {
            String hex = pair[1].trim();
            if ((hex.length() & 1) == 1)
              throw new IOException("Byte string is invalid");
            byte[] md = new byte[hex.length() >> 1];
            for(int j = 0; j < hex.length(); j+=2) {
              md[j >> 1] = (byte)((Character.digit(hex.charAt(j), 16) << 4)
                                 +Character.digit(hex.charAt(j+1), 16));
            }
            ByteBuffer buf = ByteBuffer.wrap(md, md.length - 8, 8);
            n.id = String.valueOf(Math.abs(buf.getLong()));
            //System.out.println("Node-ID mapping: " + hex + ", " + n.id);
            //n.id = hex.substring(18);
            idList.add(n.id);
            i |= 4;
          } else if (pair[0].equals("CPU")) {
            n.cpu = pair[1].trim();
          } else if (pair[0].equals("Timestamp")) {
            n.timestamp = Double.parseDouble(pair[1].trim());
            n.strTimestamp = pair[1].trim();
            i |= 8;
          } else if (pair[0].equals("Edge")) {
            if ((i & 4) == 0)
              throw new IOException("Id should always come before edges");
            String hex = pair[1].trim();
            if ((hex.length() & 1) == 1)
              throw new IOException("Byte string is invalid");
            byte[] parent = new byte[hex.length() >> 1];
            for(int j = 0; j < hex.length(); j+=2)
              parent[j >> 1] = (byte)((Character.digit(hex.charAt(j), 16) << 4)
                                 +Character.digit(hex.charAt(j+1), 16));
            ByteBuffer buf = ByteBuffer.wrap(parent);
            String pid = String.valueOf(buf.getLong());
            
            /* Add new edges to adjacency matrix */
            if (!adjList.containsKey(pid)) {
                adjList.put(pid, new ArrayList<String>());
            }
            adjList.get(pid).add(n.id);
          }
        }
        if (i != 15) {
          throw new IOException("Node is invalid: i = " + i);
        }
        //g.nodes.put(n.id, n);
        if (!nodes.containsKey(n.id))
          nodes.put(n.id, new ArrayList<Node>());
        nodes.get(n.id).add(n);
        //System.out.println("Added " +n.label +" to " +n.id.toString() + " with timestamp " + n.strTimestamp +"\n");
        /* ekrevat: my way of finding root node */
        if (n.label.indexOf("ENTRY_START") != -1) {
            g.root = n;
        }
      } 
      else {
        throw new IOException("Trace report is invalid, first line is: " + line );
      }
    }
    //if (g.root == null)
      //throw new IOException("No root node in RFG");
    for(Map.Entry<String, ArrayList<Node>> entry : nodes.entrySet()) {
      ArrayList<Node> a = entry.getValue();
      if (a.size() <= 0)
        continue;
      Collections.sort(a);
      a.get(0).id = a.get(0).id + "." + a.get(0).strTimestamp;
      g.nodes.put(a.get(0).id, a.get(0));
      //System.out.println("Adding graph node: " + a.get(0).label);
      for(int i = 1; i < a.size(); i++) {
          //System.out.println("More than one node for " + a.get(i).label);
        a.get(i).id = a.get(i).id + "." + a.get(i).strTimestamp;
        a.get(i-1).children.add(new Edge(a.get(i-1).id, a.get(i).id));
        g.nodes.put(a.get(i).id, a.get(i));
      }
    }
    for(Map.Entry<String, ArrayList<String>> entry : adjList.entrySet()) {
      ArrayList<Node> from = nodes.get(String.valueOf(Math.abs(Long.parseLong(entry.getKey()))));
      if (from == null) {
        long key = Long.parseLong(entry.getKey());
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        out.writeLong(key);
        byte[] rep = b.toByteArray();
        String hex = "";
        for(int i = 0; i < rep.length; i++) {
          String fb = "";
          int rm = (256 + rep[i]) % 16;
          if (rm >= 10)
            fb = (char)('A' + rm - 10) + fb;
          else
            fb = (char)('0' + rm) + fb;
          rm = ((256 + rep[i]) / 16) % 16;
          if (rm >= 10)
            fb = (char)('A' + rm - 10) + fb;
          else
            fb = (char)('0' + rm) + fb;
          hex += fb;
        }
        System.out.println("Nonexistent parent: " + hex);
        System.exit(1);
      }
      if (from.size() <= 0)
        continue;
      for (String s : entry.getValue()) {
        ArrayList<Node> to = nodes.get(s);
        idList.remove(s);
        /* ekrevat: "to" is assumed to only have one entry in node arraylist */
        from.get(from.size() - 1).children.add(new Edge(from.get(from.size()-1).id, to.get(0).id));
      }
    }

    /* ekrevat: The one guy left who didn't have his parent entry edge fixed must be the root node */
    if (idList.size() != 1) {
        if (idList.size() > 1) {
            System.out.println("Too many ids");
            for (String s : idList)
                System.out.println(s);
        }
        System.exit(1);
    } 
    if (g.root != nodes.get(idList.iterator().next()).get(0)) {
        System.out.println("ERROR: The only node without a parent should be the root node");
    }
    /* ekrevat: already set root node with alternative way, looking for ENTRY_START */
    //g.root = nodes.get(idList.iterator().next()).get(0);
    calculateTotalTime(g);
    calculateLatencies(g.nodes, g.root);
    return g;
  }

  public static void calculateTotalTime(Graph g) {
     double start = g.root.timestamp;
     Node n = g.root;
     while(n.children.size() > 0)
       n = g.nodes.get(n.children.getFirst().to);
     g.totalTime = (n.timestamp - start) / 1000;
     //System.out.println("Total time: " +g.totalTime);
  }

  public static class Graph {
    HashMap<String, Node> nodes;
    Node root;
    double totalTime;

    public Graph() {
      nodes = new HashMap<String, Node>();
      root = null;
      totalTime = -1;
    }
  }

  private static void createDot(long id, Graph g, PrintStream out, String name) throws IOException {
    df.setMinimumFractionDigits(3);
    out.println("# " + id + "  R: " + df.format(g.totalTime) + " usecs");
    out.println("Digraph " + name + "{");
    printNodes(g, g.root, new HashSet<Node>(), out);
    printEdges(g, g.root, new HashSet<Node>(), out);
    /*for(Node n : g.nodes.values()) {
      out.println(Math.abs(n.id) + "." + Math.abs(n.id) + " [label=\"" + n.hostname.toUpperCase() + "_" + n.label + "\"]");//"\\nDEFAULT\"]");
    }
    for(Node n : g.nodes.values()) {
      for(Edge e : n.children) {
        out.println(Math.abs(e.from) + "." + Math.abs(e.from) + " -> " + Math.abs(e.to) + "." + Math.abs(e.to) + " [label=\"R: " + df.format(e.latency) + " us\"]");
      }
    }*/
    out.println("}\n");
  }

  private static int countNodes(Graph g, Node n, HashSet<Node> visited) {
    if (visited.contains(n))
      return 0;
    visited.add(n);
    int count = 0;
    for(Edge e : n.children)
      count += countNodes(g, g.nodes.get(e.to), visited);
    return count + 1;
  }
  
  private static int countEdges(Graph g, Node n, HashSet<Node> visited) {
    if (visited.contains(n))
      return 0;
    visited.add(n);
    int count = 0;
    for(Edge e : n.children)
      count += countEdges(g, g.nodes.get(e.to), visited);
    return count + n.children.size();
  }

  private static void printNodes(Graph g, Node n, HashSet<Node> visited, PrintStream out) {
    if (visited.contains(n))
      return;
    visited.add(n);
    String endStr = "\"]";
    if (!n.cpu.equals("")) {
        endStr = " (CPU: " + n.cpu + ")\"]";
    }
    out.println(n.id + " [label=\"" + n.label + endStr);//"\\nDEFAULT\"]");
    for(Edge e : n.children)
      printNodes(g, g.nodes.get(e.to), visited, out);
  }

  private static void printEdges(Graph g, Node n, HashSet<Node> visited, PrintStream out) {
    if (visited.contains(n))
      return;
    visited.add(n);
    for(Edge e : n.children) {
        String endStr = " us\"]";
        out.println(e.from + " -> " + e.to + " [label=\"R: " + df.format(e.latency) + endStr);
        printEdges(g, g.nodes.get(e.to), visited, out);
    }
  }

  private static ArrayList<Edge> findNext(HashMap<String, Node> nodes, Node start, String hostname, HashSet<Node> visited) {
    ArrayList<Edge> nexts = new ArrayList<Edge>();
    if (visited.contains(start))
      return nexts;
    visited.add(start);
    for(Edge e : start.children) {
      Node child = nodes.get(e.to);
      if (child.hostname.equals(hostname))
        nexts.add(e);
      else
        nexts.addAll(findNext(nodes, child, hostname, visited));
    }
    return nexts;
  }

  public static void calculateLatencies(HashMap<String, Node> nodes, Node n) {
    if (n.visited)
      return;
    n.visited = true;
    for(Edge e : n.children) {
      Node child = nodes.get(e.to);
      if (Double.isNaN(e.latency)) {
        if (n.hostname.equals(child.hostname))
          e.latency = (child.timestamp - n.timestamp) / 1000;
        else {
          HashSet<Node> visited = new HashSet<Node>();
          //This should get the returning edge back to this hostname after another server does stuff
          ArrayList<Edge> nexts = findNext(nodes, child, n.hostname, visited);
          for(Edge next : nexts) {
            Node n1 = nodes.get(next.from);
            Node n2 = nodes.get(next.to);
            e.latency = next.latency = ((n2.timestamp - n.timestamp) - (n1.timestamp - child.timestamp))/2000;
          }
        }
      }
      calculateLatencies(nodes, child);
    }
  }

  public static class Node implements Comparable<Node>{
    String id;
    String agent;
    String label;
    String hostname;
    String cpu;
    String strTimestamp;
    double timestamp;
    LinkedList<Edge> children;
    boolean visited;

    public int compareTo(Node other) {
      if (timestamp < other.timestamp)
        return -1;
      if (timestamp > other.timestamp)
        return 1;
      return 0;
    }

    public Node() {
      id = "";
      label = "";
      agent = "";
      hostname = "";
      cpu = "";
      timestamp = -1;
      strTimestamp = "";
      children = new LinkedList<Edge>();
      visited = false;
    }

    public Node(String id) {
      this();
      this.id = id;
    }

    public void copy(Node n) {
      id = n.id;
      label = n.label;
      agent = n.agent;
      hostname = n.hostname;
      timestamp = n.timestamp;
      cpu = n.cpu;
    }

    public String toString() {
        return label;
    }
  }

  public static class Edge {
    String from, to;
    double latency;

    public Edge(String from, String to) {
      this.from = from;
      this.to = to;
      this.latency = Double.NaN;
    }

      public String toString() {
          return from + "->" + to + " with latency " + Double.toString(latency);
      }
  }
}
