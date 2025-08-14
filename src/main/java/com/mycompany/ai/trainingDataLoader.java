/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.ai;

/**
 *
 * @author HP
 */
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class trainingDataLoader {
    public static class Data {
        public final double[][] X; // [n][3]
        public final int[] y;      // [n] (0/1)
        public Data(double[][] X, int[] y){ this.X=X; this.y=y; }
    }

    public static Data load(Path xlsxPath) throws Exception {
        try (InputStream in = new FileInputStream(xlsxPath.toFile());
             Workbook wb = new XSSFWorkbook(in)) {
            Sheet sh = wb.getSheetAt(0);
            List<double[]> feats = new ArrayList<>();
            List<Integer> labels = new ArrayList<>();

            boolean header = true;
            for (Row r : sh) {
                if (header) { header=false; continue; } // skip header row
                if (r == null) continue;
                double temp = getNum(r, 0);
                double hum  = getNum(r, 1);
                double wind = getNum(r, 2);
                int safe01  = (int)getNum(r, 3); // 0=safe, 1=unsafe
                feats.add(new double[]{temp, hum, wind});
                labels.add(safe01);
            }
            double[][] X = feats.toArray(new double[0][]);
            int[] y = labels.stream().mapToInt(Integer::intValue).toArray();
            return new Data(X, y);
        }
    }

    private static double getNum(Row r, int c){
        Cell cell = r.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return 0;
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING  -> Double.parseDouble(cell.getStringCellValue().trim());
            default -> 0;
        };
    }
}
