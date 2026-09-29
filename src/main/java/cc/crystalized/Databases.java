package cc.crystalized;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.HashMap;
import java.util.UUID;

public class Databases {
    public static final String LOBBY = "jdbc:sqlite:" + System.getProperty("user.home") + "/databases/event_dbs" + "/lobby_db.sql?busy_timeout=5000";
    public static UUID getUUID(String name){
        try(Connection conn = DriverManager.getConnection(LOBBY)){
            PreparedStatement prep = conn.prepareStatement("SELECT player_uuid FROM LobbyPlayers WHERE player_name = ?;");
            prep.setString(1, name);
            ResultSet set = prep.executeQuery();
            set.next();
            ByteBuffer bb = ByteBuffer.wrap(set.getBytes("player_uuid"));
            long high = bb.getLong();
            long low = bb.getLong();
            return new UUID(high, low);
        } catch (SQLException e) {
            EventProxy.logger.info(e.getMessage());
            EventProxy.logger.info("couldn't get uuid for name");
            return null;
        }
    }

    public static HashMap<String, Object> fetchPlayerData(UUID uuid){
        try(Connection conn = DriverManager.getConnection(LOBBY)){
            PreparedStatement prep = conn.prepareStatement("SELECT * FROM LobbyPlayers WHERE player_uuid = ?;");
            prep.setBytes(1, uuid_to_bytes(uuid));
            ResultSet set = prep.executeQuery();
            set.next();
            ResultSetMetaData data = set.getMetaData();
            int count = data.getColumnCount();
            HashMap<String, Object> map = new HashMap<>();
            for(int i = 1; i <= count; i++){
                map.put(data.getColumnLabel(i), set.getObject(data.getColumnLabel(i)));
            }
            return map;
        }catch(SQLException e){
            EventProxy.logger.info(e.getMessage());
            EventProxy.logger.info("couldn't get player data for UUID: " + uuid);
            return null;
        }
    }

    public static HashMap<String, Object> fetchSettings(UUID uuid){
        try(Connection conn = DriverManager.getConnection(LOBBY)){
            PreparedStatement prep = conn.prepareStatement("SELECT * FROM Settings WHERE player_uuid = ?;");
            prep.setBytes(1, uuid_to_bytes(uuid));
            ResultSet set = prep.executeQuery();
            set.next();
            ResultSetMetaData data = set.getMetaData();
            int count = data.getColumnCount();
            HashMap<String, Object> map = new HashMap<>();
            for(int i = 1; i <= count; i++){
                map.put(data.getColumnLabel(i), set.getObject(data.getColumnLabel(i)));
            }
            return map;
        }catch(SQLException e){
            EventProxy.logger.info(e.getMessage());
            EventProxy.logger.info("couldn't get settings data for UUID: " + uuid);
            return null;
        }
    }

    public static byte[] uuid_to_bytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }
}
