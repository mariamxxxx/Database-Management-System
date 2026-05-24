package DBMS;

import java.io.File;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

public class FileManager
{
    static String path = FileManager.class.getResource("FileManager.class").toString();
    static File directory = new File(path.substring(6,path.length()-17) + File.separator
            + "Tables" + File.separator);

    // Simulate disk: tableName -> object
    private static HashMap<String, Table>       tables    = new HashMap<>();
    // Simulate disk: "tableName/pageNumber" -> Page
    private static HashMap<String, Page>        pages     = new HashMap<>();
    // Simulate disk: "tableName/columnName" -> BitmapIndex
    private static HashMap<String, BitmapIndex> indices   = new HashMap<>();
    // Simulate directory listing: tableName -> sorted set of filenames
    private static TreeMap<String, TreeSet<String>> directoryMap = new TreeMap<>();

    public static boolean storeTable(String tableName, Table t)
    {
        try
        {
            tables.put(tableName, t);
            directoryMap.computeIfAbsent(tableName, k -> {
                TreeSet<String> s = new TreeSet<>();
                s.add(k + ".db");
                return s;
            });
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

    public static Table loadTable(String tableName)
    {
        return tables.get(tableName);
    }

    public static boolean storeTablePage(String tableName, int pageNumber, Page p)
    {
        try
        {
            pages.put(tableName + "/" + pageNumber, p);
            directoryMap.computeIfAbsent(tableName, k -> new TreeSet<>()).add(pageNumber + ".db");
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

    public static Page loadTablePage(String tableName, int pageNumber)
    {
        return pages.get(tableName + "/" + pageNumber);
    }

    public static boolean storeTableIndex(String tableName, String columnName, BitmapIndex b)
    {
        try
        {
            indices.put(tableName + "/" + columnName, b);
            directoryMap.computeIfAbsent(tableName, k -> new TreeSet<>()).add(columnName + ".db");
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

    public static BitmapIndex loadTableIndex(String tableName, String columnName)
    {
        return indices.get(tableName + "/" + columnName);
    }

    public static void reset()
    {
        tables.clear();
        pages.clear();
        indices.clear();
        directoryMap.clear();
    }

    public static String trace()
    {
        StringBuilder res = new StringBuilder("Tables{ ");
        for (String tableName : directoryMap.keySet())
        {
            res.append(tableName).append("{ ");
            for (String file : directoryMap.get(tableName))
                res.append(file).append(" ");
            res.append("} ");
        }
        res.append("}");
        return res.toString();
    }
}