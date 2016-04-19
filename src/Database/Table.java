/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract class to deal with one SQL table
 *
 * All operations but static method asList must be preceeded by a call to connect(), and succeeded by a call to close();
 *
 * Subclasses should implement their own constructors to provide the database Object along with the table name They may also provide fields
 * to as column labels
 *
 * @author gaelph
 */
public class Table {

    protected Database database;
    protected String name;

    private static final Logger LOG = Logger.getLogger(Table.class.getName());

    private int numOpened = 0;

    protected Table() {
    }

    public Table(Database database, String name) {
        this.database = database;
        this.name = name;
    }

    private static String compileString(Iterator itr, String prePostfix, String seprator) {
        String result = "";

        while (itr.hasNext()) {
            result += prePostfix + itr.next() + prePostfix;

            if (itr.hasNext()) {
                result += seprator;
            }
        }

        return result;
    }

    /**
     * Returns the content of the table as List containing a Map of all values
     *
     * @param table
     *
     * @return a List of <pre>HashMap<String, Object></pre>
     */
    public static java.util.List<HashMap<String, Object>> asList(Table table) {

        try {
            table.database.connect(Database.READ_ONLY);
            ResultSet users = table.select(null);
            ResultSetMetaData meta = users.getMetaData();

            java.util.List<HashMap<String, Object>> result = new ArrayList<>();

            while (users.next()) {
                HashMap<String, Object> userMap = new HashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    userMap.put(meta.getColumnLabel(i), users.getObject(i));
                }
            }

            table.database.close();

            return result;

        }
        catch (SQLException ex) {
            Logger.getLogger(Table.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    /**
     * Performs a user defined SQL query and returns the result
     *
     * @param query the query as String
     *
     * @return ResultSet
     *
     * @throws SQLException
     */
    public ResultSet performQuery(String query) throws SQLException {
        ResultSet result;
        database.connect(Database.READ_WRITE);

        result = database.connection().createStatement().executeQuery(query);

        database.close();

        return result;
    }

    /**
     * SELECT <i>columns or *</i> FROM <i>table</i>
     *
     * @param columns
     *
     * @return ResultSet
     *
     * @throws SQLException
     */
    public ResultSet select(String[] columns) throws SQLException {
        return select(columns, null, null, false);
    }

    /**
     * SELECT <i>columns or *</i> FROM <i>table</i> WHERE <i>conditions</i>
     *
     * @param columns
     * @param conditions eg `name`="John" and `id`=12
     *
     * @return ResultSet
     *
     * @throws SQLException
     */
    public ResultSet select(String[] columns, String conditions) throws SQLException {
        return select(columns, conditions, null, false);
    }

    /**
     * SELECT <i>columns or *</i> FROM <i>table</i> WHERE <i>conditions</i> ORDER BY
     * <i>orderingColumn asc if true, desc if false</i>
     *
     * @param columns
     * @param conditions
     * @param orderingColumn
     * @param asc
     *
     * @return ResultSet
     *
     * @throws SQLException
     */
    public ResultSet select(String[] columns, String conditions, String orderingColumn, boolean asc) throws SQLException {
        String columnsString = "*";
        String query = "select ";

        if (columns != null) {
            if (columns.length > 0) {
                columnsString = compileString(Arrays.asList(columns).iterator(), "`", ",");
            }
        }

        query += columnsString + " from `" + name + "`";

        if (conditions != null) {
            query += " where " + conditions;
        }

        if (orderingColumn != null) {
            query += " order by `" + orderingColumn + (asc ? "` asc" : "` desc");
        }

        ResultSet result;

        PreparedStatement statement = database.connection().prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE,
                                                                             ResultSet.CONCUR_UPDATABLE);

        result = statement.executeQuery();

        return result;
    }

    /**
     * SELECT <i>columns or *</i> FROM <i>table</i> WHERE <i>criterion and params</i><br>
     * The content of the <i>params</i> array replaces the <i>?</i> in the <i>criterion</i>
     * string.<br>
     * <br>
     * For example : <br>
     * select(null, "`name`=? AND `age`<=?", Arrays.asList("John", 18, "age").toArray, "?", true) <br> will produce the query :
     * <br>
     * SELECT * FROM `
     * <i>table_name</i>` WHERE `name`="John" AND `age`<=18 ORDER BY "age" ASC<br>
     *
     * @param columns
     * @param criterion
     * @param params
     * @param orderby
     * @param ascending
     *
     * @return
     *
     * @throws SQLException
     */
    public ResultSet select(String[] columns, String criterion, Object[] params, String orderby, boolean ascending) throws SQLException {
        String columnsString = "*";
        String query = "select ";

        if (columns != null) {
            if (columns.length > 0) {
                columnsString = compileString(Arrays.asList(columns).iterator(), "`", ",");
            }
        }

        query += columnsString + " from `" + name + "`";

        if (criterion != null) {
            query += " where " + criterion;
        }

        if (orderby != null) {
            query += " order by `" + orderby + (ascending ? "` asc" : " desc");
        }

        ResultSet result;
        PreparedStatement statement = database.connection().prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE,
                                                                             ResultSet.CONCUR_UPDATABLE);

        if (params != null) {
            Iterator itr = Arrays.asList(params).iterator();
            int index = 1;

            while (itr.hasNext()) {
                statement.setObject(index, itr.next());

                index++;
            }
        }

        result = statement.executeQuery();

        return result;
    }

    public void select(String[] columns, String criterion, Object[] params, String orderby, boolean ascending, Consumer<Row> action) {
        String columnsString = "*";
        String query = "select ";

        connect();

        if (columns != null) {
            if (columns.length > 0) {
                columnsString = compileString(Arrays.asList(columns).iterator(), "`", ",");
            }
        }

        query += columnsString + " from `" + name + "`";

        if (criterion != null) {
            query += " where " + criterion;
        }

        if (orderby != null) {
            query += " order by `" + orderby + (ascending ? "` asc" : " desc");
        }

        try {

            ResultSet result;
            PreparedStatement statement = database.connection().prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE,
                                                                                 ResultSet.CONCUR_UPDATABLE);

            if (params != null) {
                Iterator itr = Arrays.asList(params).iterator();
                int index = 1;

                while (itr.hasNext()) {
                    statement.setObject(index, itr.next());

                    index++;
                }
            }

            result = statement.executeQuery();

            while (result.next()) {
                action.accept(new Row(result));
            }

        }
        catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        finally {
            close();
        }
    }

    /**
     * Returns a ResultSet containing all rows where <i>idenfyingProperty</i> matches
     * <i>identifyingValue</i>
     *
     * @param idenfyingProperty
     * @param identifyingValue
     *
     * @return
     */
    public ResultSet get(String idenfyingProperty, Object identifyingValue) {

        try {
            Object[] params = new Object[1];
            params[0] = identifyingValue;
            ResultSet result = select(null, "`" + idenfyingProperty + "`" + "=?", params, null, false);

            return result;
        }
        catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }

        return null;
    }

    /**
     * functional get
     *
     * @param idenfyingProperty
     * @param identifyingValue
     * @param action
     *
     */
    public void find(String idenfyingProperty, Object identifyingValue, Consumer<Row> action) {
        connect();
        try {
            Object[] params = new Object[1];
            params[0] = identifyingValue;
            ResultSet result = select(null, "`" + idenfyingProperty + "`" + "=?", params, null, false);

            while (result.next()) {
                if (!result.isClosed()) {
                    action.accept(new Row(result));
                }
            }
        }
        catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        finally {
            close();
        }
    }

    public Optional<Row> find(String idenfyingProperty, Object identifyingValue) {
        connect();
        try {
            Object[] params = new Object[1];
            params[0] = identifyingValue;
            ResultSet result = select(null, "`" + idenfyingProperty + "`" + "=?", params, null, false);

            if (result.next()) {
                return Optional.of(new Row(result));
            }
        }
        catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        finally {
            close();
        }

        return Optional.empty();
    }

    /**
     * Returns ResultSet containing all rows and all columns
     *
     * @return
     */
    public ResultSet getAll() {
        try {
            ResultSet result = select(null);

            return result;
        }
        catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }

        return null;
    }

    /**
     * functional get All
     *
     * @param action Consumer
     *
     */
    public void findAll(Consumer<Row> action) {
        connect();
        try {
            ResultSet result = select(null);

            while (result.next()) {
                action.accept(new Row(result));
            }
        }
        catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        finally {
            close();
        }
    }

    /**
     * Returns ResultSet containing all rows and all columns order by <i>property</i>
     *
     * @param property
     * @param ascending
     *
     * @return
     */
    public ResultSet getAllSorted(String property, boolean ascending) {
        try {
            ResultSet result = select(null, null, property, ascending);

            return result;
        }
        catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }

        return null;
    }

    /**
     * functional get All Sorted
     *
     * @param property
     * @param ascending
     * @param action
     *
     */
    public void findAllSorted(String property, boolean ascending, Consumer<Row> action) {
        connect();
        try {
            ResultSet result = select(null, null, property, ascending);

            while (result.next()) {
                action.accept(new Row(result));
            }
        }
        catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        finally {
            close();
        }
    }

    /**
     * Set the <i>property</i> to <i>value</i> for all rows matching <i>idenfyingProperty</i> ==
     * <i>identifyingValue</i>
     *
     * @param identifyingProperty
     * @param identifyingValue
     * @param property
     * @param value
     *
     * @return
     */
    public boolean setProperty(String identifyingProperty, Object identifyingValue, String property, Object value) {
        boolean ret = false;
        try {
            ResultSet result = get(identifyingProperty, identifyingValue);

            while (result.next()) {
                result.updateObject(property, value);

                result.updateRow();
            }

        }
        catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }

        return ret;
    }

    /**
     *
     * @param identifyingProperty
     * @param identifyingValue
     * @param columns
     * @param values
     *
     * @return
     */
    public boolean setProperties(String identifyingProperty, Object identifyingValue, List<String> columns, List<Object> values) {
        boolean ret = false;

        if (columns.size() != values.size()) {
            return ret;
        }

        try {
            ResultSet result = get(identifyingProperty, identifyingValue);

            while (result.next()) {
                Iterator colItr = columns.iterator();
                Iterator valItr = values.iterator();

                while (colItr.hasNext() && valItr.hasNext()) {
                    String column = (String) colItr.next();
                    Object value = (Object) valItr.next();

                    result.updateObject(column, value);
                }

                result.updateRow();

                if (result.rowUpdated()) {
                    ret = true;
                }
            }

        }
        catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }

        return ret;
    }

    /**
     * Gets the <i>property</i> for all rows matching <i>idenfyingProperty</i> ==
     * <i>identifyingValue</i>
     *
     * @param idenfyingProperty
     * @param identifyingValue
     * @param property
     *
     * @return an Object representing the requested value for <i>property</i> or null
     */
    public Object getProperty(String idenfyingProperty, Object identifyingValue, String property) {

        try {
            ResultSet result = get(idenfyingProperty, identifyingValue);

            while (result.next()) {
                return result.getObject(property);
            }

        }
        catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }

        return null;
    }

    /**
     * Inserts a new row in the table There MUST be as many columns as values
     *
     * @param columns
     * @param values
     *
     * @return true if row was inserted
     */
    public boolean create(java.util.List<String> columns, java.util.List<Object> values) {
        boolean ret = false;

        if (columns.size() != values.size()) {
            return ret;
        }

        try {
            ResultSet result = select(null);
            result.moveToInsertRow();

            Iterator colItr = columns.iterator();
            Iterator valItr = values.iterator();

            while (colItr.hasNext() && valItr.hasNext()) {
                String column = (String) colItr.next();
                Object value = (Object) valItr.next();

                result.updateObject(column, value);
            }

            result.insertRow();

            if (result.rowInserted()) {
                ret = true;
            }

            result.beforeFirst();

        }
        catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }

        return ret;
    }

    public void newRow(Consumer<Row> action) throws SQLException {
        connect();
        ResultSet result = select(null);

        result.moveToInsertRow();

        action.accept(new Row(result, true));

        close();
    }

    /**
     * Removes all rows where <i>idenfyingProperty</i> equals <i>identifyingValue</i>
     *
     * @param idenfyingProperty
     * @param identifyingValue
     */
    public void delete(String idenfyingProperty, Object identifyingValue) {
        connect();
        try {
            ResultSet result = get(idenfyingProperty, identifyingValue);

            while (result.next()) {
                result.deleteRow();
                result.beforeFirst();
            }

        }
        catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        finally {
            close();
        }
    }

    /**
     * Open the connection to the database
     */
    public void connect() {
        this.numOpened++;
        database.connect(Database.READ_WRITE);
    }

    /**
     * Close the connection to the database
     */
    public void close() {
        this.numOpened--;

        if (this.numOpened <= 0) {
            this.numOpened = 0;
            database.close();
        }
    }

    public class Row {

        private final ResultSet base;
        private boolean isInsertRow;

        public Row(ResultSet base) {
            this.base = base;
        }

        public Row(ResultSet base, boolean isInsertRow) {
            this.base = base;
            this.isInsertRow = isInsertRow;
        }

        public Object get(String property) throws SQLException {
            if (!this.base.isClosed()) {
                return this.base.getObject(property);
            }
            else {
                return "";
            }
        }

        public void set(String property, Object value) throws SQLException {
            this.base.updateObject(property, value);
        }

        public void commit() throws SQLException {
            if (this.isInsertRow) {
                this.base.insertRow();
            }
            else {
                this.base.updateRow();
            }
        }
    }

}
