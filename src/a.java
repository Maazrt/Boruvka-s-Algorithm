import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;
import javax.swing.*;

class VisualPanel extends JPanel {
    private List<Vertex> vertices;
    private List<Connector> connectors;
    private Set<Connector> minimumTree;
    private List<Connector> displayEdges;
    private boolean stepsEnabled;
    private int stepCount;

    public VisualPanel(List<Vertex> vertices, List<Connector> connectors) {
        this.vertices = vertices;
        this.connectors = connectors;
        this.minimumTree = new HashSet<>();
        this.displayEdges = new ArrayList<>();
        this.stepsEnabled = false;
        this.stepCount = 0;
    }

    public void enableSteps(boolean stepsEnabled) {
        this.stepsEnabled = stepsEnabled;
        this.displayEdges = new ArrayList<>();
        repaint();
    }

    public void updateStep(int step) {
        this.stepCount = step;
        if (stepsEnabled && step > 0 && step <= connectors.size()) {
            this.displayEdges = new ArrayList<>(minimumTree);
        } else {
            this.displayEdges = new ArrayList<>();
        }
        repaint();
    }

    public void setMinimumTree(Set<Connector> minimumTree) {
        this.minimumTree = minimumTree;
        this.displayEdges = new ArrayList<>();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        for (Connector edge : connectors) {
            paintEdge(edge, g, Color.BLACK);
        }

        for (Vertex vertex : vertices) {
            paintVertex(vertex, g, Color.darkGray);
        }

        for (Connector edge : displayEdges) {
            paintEdge(edge, g, Color.blue);
        }

        for (Connector edge : minimumTree) {
            paintEdge(edge, g, Color.RED);
        }

        if (stepsEnabled) {
            if (stepCount < connectors.size()) {
                paintEdge(connectors.get(stepCount), g, Color.GREEN);
            }
        }
    }

    private void paintEdge(Connector edge, Graphics g, Color color) {
        int sourceX = edge.start.x;
        int sourceY = edge.start.y;
        int destX = edge.end.x;
        int destY = edge.end.y;

        g.setColor(color);
        g.drawLine(sourceX, sourceY, destX, destY);
        drawWeight(edge.weight, (sourceX + destX) / 2, (sourceY + destY) / 2, g);
    }

    private void paintVertex(Vertex vertex, Graphics g, Color color) {
        g.setColor(color);
        g.fillOval(vertex.x - 10, vertex.y - 10, 20, 20);
        g.drawString(vertex.id, vertex.x - 5, vertex.y + 5);
    }

    private void drawWeight(int weight, int x, int y, Graphics g) {
        g.setColor(Color.BLACK);
        g.drawString(Integer.toString(weight), x, y);
    }
}

class Vertex {
    String id;
    int x, y;

    public Vertex(String id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }
}

class Connector {
    Vertex start, end;
    int weight;

    public Connector(Vertex start, Vertex end, int weight) {
        this.start = start;
        this.end = end;
        this.weight = weight;
    }
}

class Visualization extends JFrame {
    private VisualPanel visualPanel;

    public Visualization(List<Vertex> vertices, List<Connector> connectors) {
        this.visualPanel = new VisualPanel(vertices, connectors);
        add(visualPanel);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 400);
        setLocationRelativeTo(null);
        setTitle("Algorithm Visual");
        setVisible(true);
    }

    public void demonstrateAlgorithm(List<Vertex> vertices, List<Connector> connectors) {
        Set<Connector> minimumTree = applyAlgorithm(vertices, connectors);
        visualPanel.enableSteps(true);

        for (int i = 0; i <= connectors.size(); i++) {
            visualPanel.updateStep(i);

            if (i < connectors.size()) {
                Connector currentEdge = connectors.get(i);
                System.out.println("Step " + (i + 1) + ": " +
                        currentEdge.start.id + " - " + currentEdge.end.id +
                        " (Weight: " + currentEdge.weight + ")");
            } else {
                System.out.println("Step " + (i + 1) + ": No more steps");
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        visualPanel.enableSteps(false);
        visualPanel.setMinimumTree(minimumTree);
    }

    private Set<Connector> applyAlgorithm(List<Vertex> vertices, List<Connector> connectors) {
        Set<Connector> minimumTree = new HashSet<>();
        int vertexCount = vertices.size();

        SubGroup[] groups = new SubGroup[vertexCount];
        for (int i = 0; i < vertexCount; i++) {
            groups[i] = new SubGroup(i, 0);
        }

        while (minimumTree.size() < vertexCount - 1) {
            Connector[] nearest = new Connector[vertexCount];

            for (Connector edge : connectors) {
                int group1 = locate(groups, vertices.indexOf(edge.start));
                int group2 = locate(groups, vertices.indexOf(edge.end));

                if (group1 != group2) {
                    if (nearest[group1] == null || nearest[group1].weight > edge.weight) {
                        nearest[group1] = edge;
                    }
                    if (nearest[group2] == null || nearest[group2].weight > edge.weight) {
                        nearest[group2] = edge;
                    }
                }
            }

            for (int i = 0; i < vertexCount; i++) {
                if (nearest[i] != null) {
                    int group1 = locate(groups, vertices.indexOf(nearest[i].start));
                    int group2 = locate(groups, vertices.indexOf(nearest[i].end));

                    if (group1 != group2) {
                        minimumTree.add(nearest[i]);
                        merge(groups, group1, group2);
                    }
                }
            }
        }

        return minimumTree;
    }

    private int locate(SubGroup[] groups, int i) {
        if (groups[i].parent != i) {
            groups[i].parent = locate(groups, groups[i].parent);
        }
        return groups[i].parent;
    }

    private void merge(SubGroup[] groups, int x, int y) {
        int rootX = locate(groups, x);
        int rootY = locate(groups, y);

        if (groups[rootX].rank < groups[rootY].rank) {
            groups[rootX].parent = rootY;
        } else if (groups[rootX].rank > groups[rootY].rank) {
            groups[rootY].parent = rootX;
        } else {
            groups[rootY].parent = rootX;
            groups[rootX].rank++;
        }
    }

    private static class SubGroup {
        int parent, rank;

        public SubGroup(int parent, int rank) {
            this.parent = parent;
            this.rank = rank;
        }
    }

    public static void main(String[] args) {
        List<Vertex> vertices = new ArrayList<>();
        List<Connector> connectors = new ArrayList<>();

        vertices.add(new Vertex("A", 50, 50));
        vertices.add(new Vertex("B", 150, 50));
        vertices.add(new Vertex("C", 150, 150));
        vertices.add(new Vertex("D", 50, 150));
        vertices.add(new Vertex("E", 250, 100));
        vertices.add(new Vertex("F", 100, 250));
        vertices.add(new Vertex("G", 350, 150));
        vertices.add(new Vertex("H", 200, 300));



        Map<String, Vertex> vertexMap = new HashMap<>();
        for (Vertex vertex : vertices) {
            vertexMap.put(vertex.id, vertex);
        }

        try (BufferedReader br = new BufferedReader(new FileReader("edges.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length == 3) {
                    String startId = parts[0];
                    String endId = parts[1];
                    int weight = Integer.parseInt(parts[2]);

                    Vertex start = vertexMap.get(startId);
                    Vertex end = vertexMap.get(endId);
                    if (start != null && end != null) {
                        connectors.add(new Connector(start, end, weight));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Visualization frame = new Visualization(vertices, connectors);
        frame.demonstrateAlgorithm(vertices, connectors);
    }
}
