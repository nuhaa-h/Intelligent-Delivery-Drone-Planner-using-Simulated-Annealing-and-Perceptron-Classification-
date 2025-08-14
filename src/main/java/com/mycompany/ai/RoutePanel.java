package com.mycompany.ai;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;
import java.util.function.BiFunction;

public class RoutePanel extends JPanel {
    private double[][] cities;
    private int[] initialRoute;
    private int[] optimizedRoute;
    private int[] safety;

    public RoutePanel() {
        setPreferredSize(new Dimension(800, 600));
    }

    public void setData(double[][] xy, int[] initR, int[] optR, int[] safety){
        this.cities = xy;
        this.initialRoute = initR;
        this.optimizedRoute = optR;
        this.safety = safety;
        repaint();
    }
    public void setData(double[][] xy, int[] initR, int[] optR){
        setData(xy, initR, optR, this.safety);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (cities == null || cities.length == 0) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double minX = 1e9, minY = 1e9, maxX = -1e9, maxY = -1e9;
        for (double[] c : cities) {
            minX = Math.min(minX, c[0]);
            maxX = Math.max(maxX, c[0]);
            minY = Math.min(minY, c[1]);
            maxY = Math.max(maxY, c[1]);
        }

        final int W = getWidth();
        final int H = getHeight();
        final double pad = 32;
        final double sx = (W - 2 * pad) / Math.max(1e-9, (maxX - minX));
        final double sy = (H - 2 * pad) / Math.max(1e-9, (maxY - minY));
        final double minXFinal = minX;
        final double minYFinal = minY;
        final int HFinal = H;

        BiFunction<Double, Double, Point2D> map = (x, y) -> {
            double px = pad + (x - minXFinal) * sx;
            double py = HFinal - pad - (y - minYFinal) * sy;
            return new Point2D.Double(px, py);
        };

        if (initialRoute != null && initialRoute.length > 1) {
            g2.setColor(new Color(100, 100, 100));
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{8f, 8f}, 0));
            for (int i = 0; i < initialRoute.length; i++) {
                int a = initialRoute[i];
                int b = initialRoute[(i + 1) % initialRoute.length];
                Point2D p1 = map.apply(cities[a][0], cities[a][1]);
                Point2D p2 = map.apply(cities[b][0], cities[b][1]);
                g2.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
            }
        }

        if (optimizedRoute != null && optimizedRoute.length > 1) {
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(2f));
            for (int i = 0; i < optimizedRoute.length; i++) {
                int a = optimizedRoute[i];
                int b = optimizedRoute[(i + 1) % optimizedRoute.length];
                Point2D p1 = map.apply(cities[a][0], cities[a][1]);
                Point2D p2 = map.apply(cities[b][0], cities[b][1]);
                g2.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
            }
        }

        for (int i=0; i<cities.length; i++){
            Point2D p = map.apply(cities[i][0], cities[i][1]);
            boolean unsafe = (safety != null && i < safety.length && safety[i]==1);
            g2.setColor(unsafe ? Color.RED : Color.GREEN);
            g2.fill(new Ellipse2D.Double(p.getX()-5,p.getY()-5,10,10));
            g2.setColor(Color.BLACK);
            g2.drawString("City " + i, (float)p.getX()+8,(float)p.getY()-8);
        }

        g2.dispose();
    }
}
