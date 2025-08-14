package com.mycompany.ai;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class CityTableModel extends AbstractTableModel {
    private final String[] cols = {"X","Y","Temperature","Humidity %","Wind speed","SafeToFly"};
    private final List<Object[]> rows = new ArrayList<>();

    @Override public int getRowCount() { return rows.size(); }
    @Override public int getColumnCount() { return cols.length; }
    @Override public String getColumnName(int c) { return cols[c]; }
    @Override public boolean isCellEditable(int r,int c){ return c != 5; } // SafeToFly غير قابل للتعديل
    @Override public Object getValueAt(int r,int c){ return rows.get(r)[c]; }
    @Override public void setValueAt(Object v,int r,int c){ rows.get(r)[c]=v; fireTableCellUpdated(r,c); }

    public void setRowCount(int n){
        int cur = rows.size();
        if(n>cur){ 
            for(int i=cur;i<n;i++) rows.add(new Object[]{0.0,0.0,0.0,0.0,0.0,0});
        }
        else if(n<cur){ 
            for(int i=cur-1;i>=n;i--) rows.remove(i); 
        }
        fireTableDataChanged();
    }
}
