/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Database;

/**
 *
 * @author gaelph
 */
public class UserTable extends Table {
    
    public static final String UID = "UID";
    public static final String API_TOKEN = "api_token";
    public static final String SID = "SID";
    public static final String FIRST_NAME = "first_name";
    public static final String LAST_NAME = "last_name";
    public static final String EMAIL = "email";
    public static final String PASSWORD = "password"; //TODO avoid password storage
    public static final String STATUS = "status";

    protected UserTable(Database database, String name) {
        super(database, name);
    }
    
    public static UserTable newInstance() {
        Database database = new Database("127.0.0.1:3306", "webcamtest-db", "admin", "admin");
        return new UserTable(database, "users");
    }
}
