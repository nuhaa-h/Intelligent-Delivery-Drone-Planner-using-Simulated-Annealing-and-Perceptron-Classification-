/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.ai;

/**
 *
 * @author HP
 */
import java.util.Arrays;

public class Perceptron {
    private final double[] w;  // weights (length = 3)
    private double b;
    private final double lr;

    public Perceptron(int nFeatures, double learningRate){
        this.w = new double[nFeatures];
        this.b = 0.0;
        this.lr = learningRate;
    }
    private static int toSign(int y01){ return y01==1 ? 1 : -1; }  // 1->+1, 0->-1
    private static int to01(int ySign){ return ySign>=0 ? 1 : 0; }

    public void fit(double[][] X, int[] y01, int epochs){
        for(int e=0;e<epochs;e++){
            for(int i=0;i<X.length;i++){
                int yi = toSign(y01[i]);
                double z = dot(w, X[i]) + b;
                int yhat = z>=0 ? 1 : -1;
                if (yhat != yi){
                    for(int j=0;j<w.length;j++) w[j] += lr * yi * X[i][j];
                    b += lr * yi;
                }
            }
        }
    }
    public int predict01(double[] x){
        double z = dot(w, x) + b;
        return to01(z>=0 ? 1 : -1);
    }
    private static double dot(double[] a, double[] b){
        double s=0; for(int i=0;i<a.length;i++) s+=a[i]*b[i]; return s;
    }
}

/** Standardization (z-score) so features are on similar scale. */
class StandardScaler {
    double[] mean, std;

    public void fit(double[][] X){
        int n=X.length, d=X[0].length;
        mean = new double[d]; std = new double[d];
        for(int j=0;j<d;j++){
            for (double[] x : X) mean[j] += x[j];
            mean[j] /= Math.max(1, n);
            for (double[] x : X) std[j] += Math.pow(x[j]-mean[j], 2);
            std[j] = Math.sqrt(std[j]/Math.max(1, n));
            if (std[j]==0) std[j]=1;
        }
    }
    public double[] transform(double[] x){
        double[] y = Arrays.copyOf(x, x.length);
        for(int j=0;j<y.length;j++) y[j] = (y[j]-mean[j]) / std[j];
        return y;
    }
    public double[][] transform(double[][] X){
        double[][] Y=new double[X.length][X[0].length];
        for(int i=0;i<X.length;i++) Y[i]=transform(X[i]);
        return Y;
    }
}
