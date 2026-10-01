package cc.crystalized;

import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.*;

import static net.kyori.adventure.text.format.TextDecoration.BOLD;

public class Event {
    static ProxyServer server;
    String name;
    String color;
    ArrayList<Team> teams;
    boolean running = false;
    boolean paused = false;
    ArrayList<Match> previousMatches = new ArrayList<>();
    ArrayList<Match> runningMatches = new ArrayList<>();
    Queue<Match> futureMatches = new PriorityQueue<>();
    ArrayList<Game> games;
    Game current;

    public Event(String name, String color, ArrayList<Game> games, ArrayList<Team> teams, ProxyServer server){
        this.name = name;
        this.color = color;
        this.games = games;
        this.teams = teams;
        this.server = server;
    }

    public void start(){
        running = true;
        server.sendMessage(Component.text("Starting event...").color(TextColor.fromHexString(color)).decoration(BOLD, true));
        Match.generateFirstMatches();
        games.getFirst().getGameServers(server);
        current = games.getFirst();

    }

    public void pause(){
        paused = true;
        //TODO
    }

    public void endPause(){

    }

    public void end(){
        running = false;
        //TODO
    }

    public Team getTeam(String p){
        for(Team t : teams){
            if(t.players.keySet().contains(p)) return t;
        }
        return null;
    }
}
class Team {
    String name;
    String color;
    HashMap<String, Integer> players = new HashMap<>();
    int points;
    int gamesThisRound;
    boolean inGame = false;
    boolean eliminated = false;

    public Team(String name, String color, List<String> players) {
        this.name = name;
        this.color = color;
        players.forEach(s -> this.players.put(s, 0));
        this.points = 0;
    }
}
