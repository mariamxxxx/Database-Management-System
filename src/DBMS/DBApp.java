package DBMS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class DBApp
{
	static int dataPageSize = 2;


	public static void createTable(String tableName, String[] columnsNames)
	{
		Table t = new Table(tableName, columnsNames);
		FileManager.storeTable(tableName, t);
	}

	public static void insert(String tableName, String[] record)
	{
		Table t = FileManager.loadTable(tableName);
		t.insert(record);

		String[] colNames = t.getColumnsNames();
		for (int i = 0; i < colNames.length; i++) {
			BitmapIndex idx = FileManager.loadTableIndex(tableName, colNames[i]);
			if (idx != null) {
				idx.addRow(record[i]);
				FileManager.storeTableIndex(tableName, colNames[i], idx);
			}
		}
		FileManager.storeTable(tableName, t);
	}

	public static ArrayList<String []> select(String tableName)
	{
		Table t = FileManager.loadTable(tableName);
		ArrayList<String []> res = t.select();
		FileManager.storeTable(tableName, t);
		return res;
	}

	public static ArrayList<String []> select(String tableName, int pageNumber, int recordNumber)
	{
		Table t = FileManager.loadTable(tableName);
		ArrayList<String []> res = t.select(pageNumber, recordNumber);
		FileManager.storeTable(tableName, t);
		return res;
	}

	public static ArrayList<String []> select(String tableName, String[] cols, String[] vals)
	{
		Table t = FileManager.loadTable(tableName);
		ArrayList<String []> res = t.select(cols, vals);
		FileManager.storeTable(tableName, t);
		return res;
	}

	public static String getFullTrace(String tableName)
	{
		Table t = FileManager.loadTable(tableName);
		String res = t.getFullTrace();
		String[] colNames = t.getColumnsNames();
		ArrayList<String> indexedCols = new ArrayList<>();
		for (String col : colNames) {
			if (FileManager.loadTableIndex(tableName, col) != null)
				indexedCols.add(col);
		}
		return res + "\nIndexed Columns: " + indexedCols;
	}

	public static String getLastTrace(String tableName)
	{
		Table t = FileManager.loadTable(tableName);
		String res = t.getLastTrace();
		return res;
	}

	// This function is responsible for checking if there is any missing records from the pages of the table. 
	// The missing pages can be: no pages, one page, and multiple pages. 
	// It takes the table name and returns an arraylist of the missing records (the ones that were deleted after its page is removed).
	// use trace to find missing records and return them in an arraylist of string arrays.

	public static ArrayList<String[]> validateRecords(String tableName) {
		Table t = FileManager.loadTable(tableName);
		ArrayList<String> lines = t.getTrace();
		String line;
		ArrayList<String[]> res = new ArrayList<>();
		int pageCount = t.getPageCount();
		int tracePointer = 0;
		int atIdx, pageNum;
		Page p;

		for (int i = 0; i < pageCount; i++) {
			p = FileManager.loadTablePage(tableName, i);
			if (p == null) {
				for (; tracePointer < lines.size(); tracePointer++) {
					line = lines.get(tracePointer);
					if (!line.startsWith("Inserted:")) continue;

					atIdx = line.indexOf("at page number:") + 15;
					pageNum = Integer.parseInt(line.substring(atIdx, line.indexOf(',', atIdx)).trim());

					if (pageNum > i) break;
					if (pageNum == i) {
						String inside = line.substring(line.indexOf('[') + 1, line.indexOf(']'));
						res.add(inside.split(", "));
					}
				}
			}
		}
		t.addTrace("Validating records: " + res.size() + " records missing.");
		FileManager.storeTable(tableName, t);
		return res;
	}


// This function is responsible for recovering the missing records, which may be of a single page or multiple pages, and pages from the table. 
// The missing data should be recovered in its old place, do not just insert the missing records again into the end of the table. 
// It takes the list of missing records and the table name, and doesn't return anything, the new table should just be saved on the hard disk and the trace should be updated.
	public static void recoverRecords(String tableName, ArrayList<String[]> missing) {
		Table t = FileManager.loadTable(tableName);
		ArrayList<String> lines = t.getTrace();

		HashMap<String, String[]> missingMap = new HashMap<>();
		for (String[] record : missing)
			missingMap.put(Arrays.toString(record), record);

		String recordStr = "";
		int pageNum = 0;
		int start, end, atIdx;
		Page p;
		int recovered = 0;
		HashSet<Integer> recoveredPages = new HashSet<>();

		for (String line : lines) {
			if (recovered == missing.size()) break;
			if (!line.startsWith("Inserted:")) continue;

			start = line.indexOf('[');
			end = line.indexOf(']');
			recordStr = line.substring(start, end + 1);

			if (missingMap.containsKey(recordStr)) {
				atIdx = line.indexOf("at page number:") + 15;
				pageNum = Integer.parseInt(line.substring(atIdx, line.indexOf(',', atIdx)).trim());
				p = FileManager.loadTablePage(tableName, pageNum);
				if (p == null) 
					p = new Page();

				p.insert(missingMap.get(recordStr));
				FileManager.storeTablePage(tableName, pageNum, p);

				if (!recoveredPages.contains(pageNum))
					recoveredPages.add(pageNum);
				recovered++;
			}
		}

		t.addTrace("Recovering " + missing.size() + " records in pages: " + recoveredPages);

		rebuildExistingIndexes(tableName, t);
		FileManager.storeTable(tableName, t);
	}

	public static void createBitMapIndex(String tableName, String colName) {
		Table t = FileManager.loadTable(tableName);
		int colIndex = findColumnIndex(t, colName);
		if (colIndex == -1) return;

		long startTime = System.currentTimeMillis();

		BitmapIndex index = buildIndexForColumn(tableName, t, colIndex);

		long stopTime = System.currentTimeMillis();
		t.addTrace("Index created for column: " + colName + ", execution time (mil):" + (stopTime - startTime));
		FileManager.storeTableIndex(tableName, colName, index);
		FileManager.storeTable(tableName, t);
	}

	public static String getValueBits(String tableName, String colName, String value) {
		BitmapIndex index = FileManager.loadTableIndex(tableName, colName);
		if (index == null) {
			Table t = FileManager.loadTable(tableName);
			int count = (t == null) ? 0 : t.getRecordsCount();
			return "0".repeat(count);
		}
		return index.getBits(value);
	}

	private static int findColumnIndex(Table t, String colName) {
		return t.getColMap().getOrDefault(colName, -1);
	}

	private static BitmapIndex buildIndexForColumn(String tableName, Table t, int colIndex) {
		BitmapIndex index = new BitmapIndex();
		int pageCount = t.getPageCount();
		for (int i = 0; i < pageCount; i++) {
			Page p = FileManager.loadTablePage(tableName, i);
			if (p == null) 
				continue;
			ArrayList<String[]> records = p.select();
			for (String[] record : records) {
				index.addRow(record[colIndex]);
			}
		}
		return index;
	}

	private static void rebuildExistingIndexes(String tableName, Table t) {
		String[] columns = t.getColumnsNames();
		int colLength = columns.length;
		for (int i = 0; i < colLength; i++) {
			BitmapIndex existing = FileManager.loadTableIndex(tableName, columns[i]);
			if (existing != null) {
				BitmapIndex rebuilt = buildIndexForColumn(tableName, t, i);
				FileManager.storeTableIndex(tableName, columns[i], rebuilt);
			}
		}
	}

	public static ArrayList<String[]> selectIndex(String tableName, String[] cols, String[] vals) {
		long startTime = System.currentTimeMillis();
		Table t = FileManager.loadTable(tableName);
		HashMap<String, Integer> colMap = t.getColMap();

		HashMap<String, String> condMap = new HashMap<>();
		for (int i = 0; i < cols.length; i++)
			condMap.put(cols[i], vals[i]);

		ArrayList<String> indexedCols = new ArrayList<>();
		ArrayList<String> nonIndexedCols = new ArrayList<>();
		String combinedBits = null;

		for (int i = 0; i < cols.length; i++) {
			BitmapIndex idx = FileManager.loadTableIndex(tableName, cols[i]);
			if (idx != null) {
				indexedCols.add(cols[i]);
				String bits = idx.getBits(vals[i]);
				combinedBits = (combinedBits == null) ? bits : BitmapIndex.andBits(combinedBits, bits);
			} else {
				nonIndexedCols.add(cols[i]);
			}
		}

		if (indexedCols.isEmpty()) {
			ArrayList<String[]> result = t.select(cols, vals);
			Collections.sort(nonIndexedCols);
			long stopTime = System.currentTimeMillis();
			t.addTrace("Select index condition:" + Arrays.toString(cols) + "->" + Arrays.toString(vals)
				+ ", Non Indexed: " + nonIndexedCols
				+ ", Final count: " + result.size()
				+ ", execution time (mil):" + (stopTime - startTime));
			FileManager.storeTable(tableName, t);
			return result;
		}

		ArrayList<String[]> result = new ArrayList<>();
		int globalRow = 0;
		int bitsLength = combinedBits.length();
		int indexedCount = 0;
		boolean match;
		Page p;
		ArrayList<String[]> records;
		boolean hasNonIndexed = !nonIndexedCols.isEmpty();
		int pageCount = t.getPageCount();

		for (int i = 0; i < pageCount; i++) {
			p = FileManager.loadTablePage(tableName, i);
			if (p == null) continue;
			records = p.select();
			for (String[] record : records) {
				if (globalRow < bitsLength && combinedBits.charAt(globalRow) == '1') {
					indexedCount++;
					if (!hasNonIndexed) {
						result.add(record);
					} else {
						match = true;
						for (String nonIdxCol : nonIndexedCols) {
							if (!record[colMap.get(nonIdxCol)].equals(condMap.get(nonIdxCol))) {
								match = false;
								break;
							}
						}
						if (match) result.add(record);
					}
				}
				globalRow++;
			}
		}

		// sort in place — no copy, done with logic so mutation is safe
		Collections.sort(indexedCols);
		Collections.sort(nonIndexedCols);

		long stopTime = System.currentTimeMillis();
		t.addTrace("Select index condition:" + Arrays.toString(cols) + "->" + Arrays.toString(vals)
			+ ", Indexed columns: " + indexedCols
			+ (nonIndexedCols.isEmpty() ? "" : ", Non Indexed: " + nonIndexedCols)
			+ ", Indexed selection count: " + indexedCount
			+ ", Final count: " + result.size()
			+ ", execution time (mil):" + (stopTime - startTime));
		FileManager.storeTable(tableName, t);

		return result;
	}

	public static void main(String []args) throws IOException
	{
	}

}
