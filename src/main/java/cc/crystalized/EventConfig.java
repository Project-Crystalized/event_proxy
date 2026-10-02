package cc.crystalized;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.velocitypowered.api.proxy.ProxyServer;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import org.slf4j.Logger;

public class EventConfig {
    ProxyServer server;
    Logger logger;
    public EventConfig(ProxyServer server, Logger logger){
        this.server = server;
        this.logger = logger;
        try {
            final String directory = Files.readString(Paths.get(System.getProperty("user.home") + "/databases/event_dbs/event_config.json"));
            JsonObject json = JsonParser.parseString(directory).getAsJsonObject();

            JsonElement v = json.get("version");
            if(v.getAsInt() != 1){
                throw new Exception("incorrect event_config.json file version, please update your event_config.json");
            }
            String name = json.get("name").getAsString();
            String color = json.get("color").getAsString();
            ArrayList<Game> game = new ArrayList<>();
            ArrayList<Team> teams = new ArrayList<>();
            JsonArray games = json.get("games").getAsJsonArray();
            for(JsonElement j : games){
                JsonObject o = json.get(j.getAsString()).getAsJsonObject();
                game.add(new Game(j.getAsString(), o.get("match_type").getAsString(), o.get("amount_of_games").getAsInt(), o.get("max_team_amount").getAsInt()));
            }

            Map<String, JsonElement> map = json.getAsJsonObject("teams").asMap();
            for(String s : map.keySet()){
                JsonObject o = map.get(s).getAsJsonObject();
                teams.add(new Team(o.get("name").getAsString(), o.get("color").getAsString(), getTeam(o.get("players").getAsJsonArray())));
            }

            EventProxy.event = new Event(name, color, game, teams, server);

        }catch(Exception e){
            logger.error("Could not load the event configuration file!\n Error: " + e);
            e.printStackTrace();
            logger.error("The Plugin is unusable without the configuration file!");
            //server.getPluginManager().disablePlugin(EventProxy.getInstance());
            throw new RuntimeException(new Exception());
        }
    }

    private ArrayList<String> getTeam(JsonArray array){
        ArrayList<String> team = new ArrayList<>();
        for(JsonElement j : array){
            team.add(j.getAsString());
        }
        return team;
    }
}
