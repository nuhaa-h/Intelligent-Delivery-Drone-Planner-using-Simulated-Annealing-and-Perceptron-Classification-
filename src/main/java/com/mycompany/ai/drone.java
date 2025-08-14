/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.mycompany.ai;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author HP
 */
public class drone extends javax.swing.JFrame {

    /**
     * Creates new form drone
     */
   // Class-level field so you can update later
private RoutePanel routePanel;
private int[] initialRoute;
// Perceptron + scaling
private Perceptron perceptron;          // trained model
private StandardScaler scaler;          // stores mean/std for 3 weather features
private int[] safety;                   // per city: 0 = safe, 1 = unsafe


// Cost penalty for unsafe cities (tune if you want)
private static final double UNSAFE_PENALTY = 50.0;


public drone() {
    initComponents();
    // ONE RoutePanel in the center, the button at the bottom
routePanel = new RoutePanel();

PlotPanel.removeAll(); // clear what the GUI builder added
routePanel.setPreferredSize(new Dimension(400, 300)); // width, height in pixels
PlotPanel.add(routePanel, BorderLayout.CENTER);
PlotPanel.setLayout(new java.awt.BorderLayout());
PlotPanel.add(routePanel, java.awt.BorderLayout.CENTER);
PlotPanel.add(jButton3, java.awt.BorderLayout.SOUTH); // keep the button visible
PlotPanel.revalidate();
PlotPanel.repaint();
// Fill N rows based on the spinner, then immediately show the initial route
jButton5.addActionListener(e -> {
    int n = ((Number) jSpinner1.getValue()).intValue();
    fillRandomData(n, /*autoShowInitial=*/true);
});

jTable1.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

CityTableModel model = new CityTableModel();
jTable1.setModel(model);
jSpinner1.addChangeListener(e -> model.setRowCount((int) jSpinner1.getValue()));

jSpinner1.setValue(10); // default
model.setRowCount(10);

// Make table numeric only
for (int c = 0; c < jTable1.getColumnCount(); c++) {
    jTable1.getColumnModel().getColumn(c).setCellEditor(numericEditor());
}

// Button actions
jButton3.addActionListener(e -> onShowInitial());
jButton2.addActionListener(e -> onRunSA());

}
private void fillRandomData(int n, boolean autoShowInitial) {
    // Commit any in-progress cell edit before we change the model
    if (jTable1.isEditing()) jTable1.getCellEditor().stopCellEditing();

    // Keep spinner + model in sync
    jSpinner1.setValue(n);
    CityTableModel m = (CityTableModel) jTable1.getModel();
    m.setRowCount(n);

    java.util.Random rnd = new java.util.Random(); // or new Random(42) for reproducible tests

    for (int i = 0; i < n; i++) {
        // Coordinates in a nice box; rounded to 1 decimal
        double x = round1(randIn(rnd, 50, 450));
        double y = round1(randIn(rnd, 50, 350));

        // Weather: ~35% "unsafe-ish", else "safe-ish"
        boolean unsafeish = rnd.nextDouble() < 0.35;

        double temp = unsafeish ? randIn(rnd, 28, 35) : randIn(rnd, 18, 26);  // °C
        double hum  = unsafeish ? randIn(rnd, 70, 90) : randIn(rnd, 40, 60);  // %
        double wind = unsafeish ? randIn(rnd, 14, 24) : randIn(rnd,  5, 12);  // km/h

        m.setValueAt(x,    i, 0); // X
        m.setValueAt(y,    i, 1); // Y
        m.setValueAt(temp, i, 2); // Temperature
        m.setValueAt(hum,  i, 3); // Humidity %
        m.setValueAt(wind, i, 4); // Wind speed
    }

    // Optional: draw something immediately so the user sees feedback
    if (autoShowInitial) {
        onShowInitial(); // uses your table -> routePanel pipeline
    }
}

// helpers
private static double randIn(java.util.Random rnd, double min, double max) {
    return min + rnd.nextDouble() * (max - min);
}
private static double round1(double v) {
    return Math.round(v * 10.0) / 10.0;
}

public static int[] exhaustiveSearch(double[][] coords) {
    int n = coords.length;
    int[] cities = new int[n];
    for (int i = 0; i < n; i++) cities[i] = i;

    double bestDistance = Double.MAX_VALUE;
    int[] bestRoute = cities.clone();

    List<int[]> permutations = new ArrayList<>();
    permute(cities, 1, permutations); // keep city[0] fixed to reduce duplicates

    for (int[] route : permutations) {
        double dist = totalDistance(coords, route);
        if (dist < bestDistance) {
            bestDistance = dist;
            bestRoute = route.clone();
        }
    }

    System.out.println("EPOX Optimal Distance: " + bestDistance);
    return bestRoute;
}

private static void permute(int[] arr, int start, List<int[]> result) {
    if (start == arr.length) {
        result.add(arr.clone());
        return;
    }
    for (int i = start; i < arr.length; i++) {
        swap(arr, start, i);
        permute(arr, start + 1, result);
        swap(arr, start, i);
    }
}

private static void swap(int[] arr, int i, int j) {
    int temp = arr[i];
    arr[i] = arr[j];
    arr[j] = temp;
}
// Read X,Y from table into a matrix the plot understands
private double[][] getXY() {
    // commit the value in the cell the user is currently editing
    if (jTable1.isEditing()) {
        jTable1.getCellEditor().stopCellEditing();
    }

    CityTableModel m = (CityTableModel) jTable1.getModel();
    java.util.List<double[]> rows = new java.util.ArrayList<>();

    for (int r = 0; r < m.getRowCount(); r++) {
        Object ox = m.getValueAt(r, 0);
        Object oy = m.getValueAt(r, 1);
        if (ox == null || oy == null) continue;
        String sx = String.valueOf(ox).trim();
        String sy = String.valueOf(oy).trim();
        if (sx.isEmpty() || sy.isEmpty()) continue;

        double x = (ox instanceof Number) ? ((Number) ox).doubleValue() : Double.parseDouble(sx);
        double y = (oy instanceof Number) ? ((Number) oy).doubleValue() : Double.parseDouble(sy);
        rows.add(new double[]{x, y});
    }
    return rows.toArray(new double[0][2]);
}

private static int[] randomRoute(int n) {
    java.util.List<Integer> L = new java.util.ArrayList<>();
    for (int i = 0; i < n; i++) L.add(i);
    java.util.Collections.shuffle(L, new java.util.Random());
    int[] r = new int[n];
    for (int i = 0; i < n; i++) r[i] = L.get(i);
    return r;
}

// Closed-loop distance of a route
private static double totalDistance(double[][] xy, int[] r) {
    double d = 0;
    for (int i = 0; i < r.length; i++) {
        int a = r[i], b = r[(i + 1) % r.length];
        d += Math.hypot(xy[a][0] - xy[b][0], xy[a][1] - xy[b][1]);
    }
    return d;
}

// Simple SA with swap neighbor
private static int[] anneal(int[] start, double[][] xy, int[] safety,
                            double T0, double alpha, int itersPerT, int maxTemps) {
    java.util.Random rnd = new java.util.Random();
    int n = start.length;
    int[] curr = start.clone();
    double currC = routeCost(xy, curr, safety);
    int[] best = curr.clone();
    double bestC = currC;

    double T = Math.max(1e-6, T0);
    for (int t=0; t<maxTemps && T>1e-6; t++) {
        for (int k=0; k<itersPerT; k++) {
            int i=rnd.nextInt(n), j=rnd.nextInt(n); if(i==j) continue;
            if(i>j){int tmp=i; i=j; j=tmp;}
            int[] cand = curr.clone();
            int tmp = cand[i]; cand[i]=cand[j]; cand[j]=tmp;

            double candC = routeCost(xy, cand, safety);
            double delta = candC - currC;
            // Metropolis criterion
            if (delta <= 0 || rnd.nextDouble() < Math.exp(-delta / T)) {
                curr = cand; currC = candC;
                if (currC < bestC) { best = curr.clone(); bestC = currC; }
            }
        }
        T *= alpha; // geometric cooling
    }
    return best;
}

private void onShowInitial() {
    double[][] xy = getXY();
    if (xy.length < 2) {
        jLabel7.setText("Initial distance: — (need ≥ 2 cities)");
        routePanel.setData(xy, initialRoute, null, safety);
        return;
    }

    // safety from perceptron if trained; else all-safe
    safety = predictSafetyFromTable();
    if (safety == null || safety.length != xy.length) {
        safety = new int[xy.length]; // zeros = safe
    }

    initialRoute = randomRoute(xy.length);

    double d = totalDistance(xy, initialRoute);
    double c = routeCost(xy, initialRoute, safety);

    jLabel7.setText(String.format("Initial distance: %.3f", d));
    jLabel5.setText(String.format("initial cost : %.3f", c));

    routePanel.setData(xy, initialRoute, null);
}

private void onRunSA() {
    double[][] xy = getXY();

    if (xy.length < 2) {
        jLabel8.setText("Optimized distance: — (need ≥ 2 cities)");
        return;
    }
    if (initialRoute == null) initialRoute = randomRoute(xy.length);

    // Refresh safety from table (uses perceptron if trained)
    safety = predictSafetyFromTable();
    if (safety == null || safety.length != xy.length) {
        safety = new int[xy.length]; // zeros = safe
    }

    // read SA params
    double T0, alpha;
    try {
        T0 = Double.parseDouble(jTextField1.getText().trim());
        alpha = Double.parseDouble(jTextField2.getText().trim());
        if (alpha <= 0 || alpha >= 1) {
            throw new IllegalArgumentException("Cooling rate alpha must be in (0,1)");
        }
    } catch (Exception ex) {
        javax.swing.JOptionPane.showMessageDialog(this,
            "Initial temperature (e.g., 200) and cooling rate in (0,1) (e.g., 0.95).",
            "Input error", javax.swing.JOptionPane.WARNING_MESSAGE);
        return;
    }

    int[] best = anneal(initialRoute, xy, safety, T0, alpha, 250, 120);

    double dBest = totalDistance(xy, best);
    double cBest = routeCost(xy, best, safety);

    jLabel8.setText(String.format("Optimized distance: %.3f", dBest));
    jLabel6.setText(String.format("optimized cost : %.3f", cBest));

    routePanel.setData(xy, initialRoute, best, safety);
}

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jSpinner1 = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jLabel2 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jButton5 = new javax.swing.JButton();
        canvas1 = new java.awt.Canvas();
        PlotPanel = new javax.swing.JPanel();
        jButton3 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setText("Number of delivery cities ");

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "X", "Y", "Temperature", "Humidity %", "Wind speed"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        jLabel2.setText("initial temperature");

        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });

        jLabel3.setText("cooling rate");

        jButton1.setText("train perceptron");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("run SA optimization");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jLabel4.setText("route cost analysis");

        jLabel5.setText("initial cost : 0.0");

        jLabel6.setText("optimized cost : 0.0");

        jLabel7.setText("initial distance : 0.0");

        jLabel8.setText("optimized distance : 0.0");

        jButton5.setText("fill random values");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSpinner1, javax.swing.GroupLayout.PREFERRED_SIZE, 184, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 400, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                            .addComponent(jButton1)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(jButton2)))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(jButton5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jLabel3)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGap(0, 14, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 145, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jSpinner1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 348, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(27, 27, 27)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2)
                    .addComponent(jButton5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel4)
                .addGap(34, 34, 34)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel6)
                .addGap(12, 12, 12)
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel8)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jButton3.setText("show initial route");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout PlotPanelLayout = new javax.swing.GroupLayout(PlotPanel);
        PlotPanel.setLayout(PlotPanelLayout);
        PlotPanelLayout.setHorizontalGroup(
            PlotPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PlotPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(420, 420, 420))
        );
        PlotPanelLayout.setVerticalGroup(
            PlotPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, PlotPanelLayout.createSequentialGroup()
                .addGap(0, 473, Short.MAX_VALUE)
                .addComponent(jButton3))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(43, 43, 43)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(PlotPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(canvas1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(PlotPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(canvas1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(28, 28, 28))))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
 try {
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        if (fc.showOpenDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) return;

        var data = trainingDataLoader.load(fc.getSelectedFile().toPath());
        scaler = new StandardScaler();
        scaler.fit(data.X);
        double[][] Xs = scaler.transform(data.X);

        perceptron = new Perceptron(3, 0.1); // lr=0.1
        perceptron.fit(Xs, data.y, 50);

        javax.swing.JOptionPane.showMessageDialog(this,
            "Perceptron trained on " + data.X.length + " rows.");

        // 🔹 بعد التدريب: عمل تنبؤ فوري على بيانات الجدول
        safety = predictSafetyFromTable(); 
        if (safety != null) {
            for (int i = 0; i < safety.length; i++) {
                ((CityTableModel) jTable1.getModel()).setValueAt(safety[i], i, 5);
            }
        }

    } catch (Exception ex) {
        ex.printStackTrace();
        javax.swing.JOptionPane.showMessageDialog(this,
            "Training failed: " + ex.getMessage());
    }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
 onRunSA();         // TODO add your handling code here:
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
 onShowInitial();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        fillRandomData(((Number) jSpinner1.getValue()).intValue(), true);
    }//GEN-LAST:event_jButton5ActionPerformed
public static double routeCost(double[][] xy, int[] route, int[] safety) {
     double totalCost = 0.0;
    for (int i = 0; i < route.length; i++) {
        int currCity = route[i];
        int nextCity = route[(i + 1) % route.length];
        double dx = xy[currCity][0] - xy[nextCity][0];
        double dy = xy[currCity][1] - xy[nextCity][1];
        double distance = Math.sqrt(dx * dx + dy * dy);
        totalCost += distance;
        if (safety[currCity] == 1) totalCost += 50; 
    }
    return totalCost;
}

private static javax.swing.DefaultCellEditor numericEditor() {
    java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance();
    nf.setGroupingUsed(false);
    javax.swing.text.NumberFormatter fmt = new javax.swing.text.NumberFormatter(nf);
    fmt.setValueClass(Double.class);
    fmt.setAllowsInvalid(false);
    javax.swing.JFormattedTextField f = new javax.swing.JFormattedTextField(fmt);
    f.setColumns(8);
    return new javax.swing.DefaultCellEditor(f);
}
private double[][] getWeatherX() {
    if (jTable1.isEditing()) jTable1.getCellEditor().stopCellEditing();
    CityTableModel m = (CityTableModel) jTable1.getModel();
    java.util.List<double[]> rows = new java.util.ArrayList<>();
    for (int r=0; r<m.getRowCount(); r++){
        Object ot = m.getValueAt(r, 2), oh = m.getValueAt(r, 3), ow = m.getValueAt(r, 4);
        if (ot==null || oh==null || ow==null) continue;
        String st=String.valueOf(ot).trim(), sh=String.valueOf(oh).trim(), sw=String.valueOf(ow).trim();
        if (st.isEmpty() || sh.isEmpty() || sw.isEmpty()) continue;
        double T = (ot instanceof Number) ? ((Number)ot).doubleValue() : Double.parseDouble(st);
        double H = (oh instanceof Number) ? ((Number)oh).doubleValue() : Double.parseDouble(sh);
        double W = (ow instanceof Number) ? ((Number)ow).doubleValue() : Double.parseDouble(sw);
        rows.add(new double[]{T,H,W});
    }
    return rows.toArray(new double[0][3]);
}
private int[] predictSafetyFromTable() {
   if (perceptron == null || scaler == null) return null;
    double[][] X = getWeatherX();
    double[][] Xs = scaler.transform(X);
    int[] out = new int[Xs.length];
    for (int i=0; i<Xs.length; i++) {
        out[i] = perceptron.predict01(Xs[i]);
        ((CityTableModel) jTable1.getModel()).setValueAt(out[i], i, 5); 
    }
    return out;
}
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(drone.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(drone.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(drone.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(drone.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new drone().setVisible(true);
            }
        });
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel PlotPanel;
    private java.awt.Canvas canvas1;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton5;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSpinner jSpinner1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    // End of variables declaration//GEN-END:variables
}
