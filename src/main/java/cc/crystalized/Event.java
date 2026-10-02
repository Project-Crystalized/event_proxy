package cc.crystalized;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;
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
        Event.server = server;
        teams.forEach(t -> t.players.forEach(pd -> EventProxy.logger.warn(pd.name)));
    }

    public void start(){
        if(!readyCheck()){
            //TODO alert admins and don't start
            return;
        }
        running = true;
        server.sendMessage(text("Starting event...").color(TextColor.fromHexString(color)).decoration(BOLD, true));
        Match.generateFirstMatches();
        games.getFirst().getGameServers(server);
        current = games.getFirst();
        while(futureMatches.peek() != null && futureMatches.peek().start()){
            futureMatches.poll();
        }
    }

    public void pause(){
        paused = true;
        //TODO idk
    }

    public void endPause(){
        if(!readyCheck()){
            //TODO alert admins and keep paused
            return;
        }
        paused = false;
        //TODO idk
    }

    public void end(){
        running = false;
        //TODO results
    }

    public Team getTeam(String p){
        for(Team t : teams){
            if(t.players.stream().anyMatch(pd -> pd.name.equals(p))) return t;
        }
        return null;
    }

    public boolean readyCheck(){
        HashMap<Team, Boolean> readies = new HashMap<>();
        long epoch = 0;
        for(Team t : teams){
            epoch = t.sendReadyCheck();
            readies.put(t, false);
            final long ep = epoch;
            AtomicInteger seconds = new AtomicInteger();
            Event.server.getScheduler().buildTask(EventProxy.plugin, (self) -> {
                int timesReady = 0;
                if (seconds.intValue() == 300 || (running && !paused) || timesReady == t.players.size()){
                    readies.put(t, timesReady == t.players.size());
                    self.cancel();
                }
                for(PlayerData pd : t.players){
                    if(pd.lastReadyCheck == ep && pd.ready) timesReady++;
                }
                seconds.getAndIncrement();
            }).repeat(1L, TimeUnit.SECONDS).schedule();
        }
        while(readies.containsValue(false) && Instant.now().getEpochSecond() - epoch < 300){}
        return !readies.containsValue(false);
    }

    public int getPlayerAmount(){
        int players = 0;
        for(Team t : teams){
            for(PlayerData data : t.players){
                players++;
            }
        }
        return players;
    }
}
class Team{
    String name;
    String color;
    List<PlayerData> players;
    int points = 0;
    int gamesThisRound;
    boolean inGame = false;
    boolean eliminated = false;

    public Team(String name, String color, List<String> players) {
        this.name = name;
        this.color = color;
        this.players = players.stream().map(s -> new PlayerData(s, 0)).toList();
    }

    public long sendReadyCheck(){
        for(PlayerData pd : players){
            Optional<Player> player = Event.server.getPlayer(pd.name);
            if(player.isEmpty()) continue;
            Title title = Title.title(translatable("Ready-check").color(GREEN).decoration(BOLD, true), translatable("Click ready in chat").color(YELLOW));
            player.get().showTitle(title);
            player.get().sendMessage(text("--------------------------\n").append(translatable("Are you ready? \n").color(YELLOW).decoration(BOLD, true)).append(translatable("READY").color(GREEN).decoration(BOLD, true).clickEvent(ClickEvent.runCommand("/ready"))));
            long epoch = Instant.now().getEpochSecond();
            pd.lastReadyCheck = epoch;
            return epoch;
        }
        return 0L;
    }
}

class PlayerData{
    String name;
    int points;
    long lastReadyCheck;
    boolean ready = false;

    public PlayerData(String name, int points){
        this.name = name;
        this.points = points;
    }

    public static PlayerData getPlayerData(String player){
        for(Team t : EventProxy.event.teams){
            for(PlayerData pd : t.players){
                if(pd.name.equals(player)) return pd;
            }
        }
        return null;
    }
}
