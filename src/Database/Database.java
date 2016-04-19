/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author gaelph
 */
public class Database {

    public final String name;
    public final String host;

    public final String userName;
    public final String password;

    private static final String MYSQL_JDBC_STRING = "com.mysql.jdbc.Driver";
    private static final Logger LOG = Logger.getLogger(Database.class.getName());

    private final Map<String, Table> tableBuffer = new HashMap<>();

    private Connection connect = null;
    private Statement statement = null;

    public static final boolean READ_ONLY = false;
    public static final boolean READ_WRITE = true;

    public Database(String host, String name, String userName, String password) {
        this.name = name;
        this.host = host;

        this.userName = userName;
        this.password = password;
    }

    public boolean connect(boolean rorw) {
        try {
            Class.forName(MYSQL_JDBC_STRING);

            Properties connectionProperties = new Properties();
            connectionProperties.setProperty("user", userName);
            connectionProperties.setProperty("password", password);

            connect = DriverManager.getConnection("jdbc:mysql://" + host + "/" + name, connectionProperties);

            statement = connect.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                                                rorw ? ResultSet.CONCUR_UPDATABLE : ResultSet.CONCUR_READ_ONLY);

            return true;

        }
        catch (ClassNotFoundException ex) {
            LOG.log(Level.SEVERE, "My SQL JDBC Driver not found");
        }
        catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }

        return false;
    }

    public Table getTable(String tableName) {
        if (!this.tableBuffer.containsKey(tableName)) {
            this.tableBuffer.put(tableName, new Table(this, tableName));
        }

        return this.tableBuffer.get(tableName);
    }

    public Connection connection() {
        return connect;
    }

    public void close() {
        try {
            statement.close();
            connect.close();

        }
        catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }
}
