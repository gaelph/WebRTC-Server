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
public class RoomTable extends Table {
    public static final String RID = "rid";
    public static final String NAME = "name";
    public static final String PARTICIPANTS = "participants";

    protected RoomTable(Database database, String name) {
        super(database, name);
    }
    
    public static RoomTable newInstance() {
        Database database = new Database("127.0.0.1:3306", "webcamtest-db", "admin", "admin");
        return new RoomTable(database, "rooms");
    }
    
}
