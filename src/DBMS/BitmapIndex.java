package DBMS;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class BitmapIndex implements Serializable {

    private HashMap<String, ArrayList<Integer>> index;
    private int rowCount;

    public BitmapIndex() {
        this.index = new HashMap<>();
        this.rowCount = 0;
    }

    public void addRow(String value) {
        index.computeIfAbsent(value, k -> new ArrayList<>()).add(rowCount);
        rowCount++;
    }

    public String getBits(String value) {
        ArrayList<Integer> positions = index.get(value);
        if (positions == null) return "0".repeat(rowCount);
        char[] bits = new char[rowCount];
        java.util.Arrays.fill(bits, '0');
        for (int pos : positions) bits[pos] = '1';
        return new String(bits);
    }

    public int getRowCount() {
        return rowCount;
    }

    public HashMap<String, ArrayList<Integer>> getIndex() {
        return index;
    }

    public static String andBits(String a, String b) {
        int len = a.length();
        StringBuilder sb = new StringBuilder(len); // pre-allocate exact size
        for (int i = 0; i < len; i++)
            sb.append((a.charAt(i) == '1' && b.charAt(i) == '1') ? '1' : '0');
        return sb.toString();
    }

    @Override
    public String toString() {
        return "BitmapIndex{rowCount=" + rowCount + ", index=" + index + "}";
    }
}