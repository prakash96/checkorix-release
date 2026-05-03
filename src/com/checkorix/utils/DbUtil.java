package com.checkorix.utils;
import java.sql.Connection;
import java.sql.DriverManager;

public class DbUtil {

    public enum DbType {
        POSTGRES, MYSQL, ORACLE, SQLSERVER, UNKNOWN
    }

    public static DbType detect(String url) {
        if (url == null) return DbType.UNKNOWN;

        url = url.toLowerCase();

        if (url.startsWith("jdbc:postgresql:")) return DbType.POSTGRES;
        if (url.startsWith("jdbc:mysql:")) return DbType.MYSQL;
        if (url.startsWith("jdbc:oracle:")) return DbType.ORACLE;
        if (url.startsWith("jdbc:sqlserver:")) return DbType.SQLSERVER;

        return DbType.UNKNOWN;
    }

    public static String driver(DbType type) {
        switch (type) {
            case POSTGRES:
                return "org.postgresql.Driver";
            case MYSQL:
                return "com.mysql.cj.jdbc.Driver";
            case ORACLE:
                return "oracle.jdbc.OracleDriver";
            case SQLSERVER:
                return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            default:
                throw new RuntimeException("Unsupported DB type");
        }
    }

    public static void loadDriver(String url) {
        try {
            DbType type = detect(url);
            String driver = driver(type);
            Class.forName(driver);
        } catch (Exception e) {
            throw new RuntimeException("Driver load failed", e);
        }
    }

    public static Connection getConnection(String url, String user, String pass) throws Exception {
        loadDriver(url);
        return DriverManager.getConnection(url, user, pass);
    }
}