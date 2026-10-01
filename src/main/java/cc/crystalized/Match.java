package cc.crystalized;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Match {
    private static int nextID = 0;
    final int id;
    RegisteredServer server = null;
    boolean running = false;
    Game game;
    List<Team> teams;
    HashMap<Team, Integer> results;
    Team winner;

    public Match(List<Team> teams, Game game) {
        this.teams = teams;
        this.game = game;
        this.id = nextID++;
    }

    public static void generateFirstMatches(){
        Event e = EventProxy.event;
        Game g = e.games.getFirst();
        List<Team> teams = new ArrayList<>();
        int i = 0;
        for(Team t : e.teams){
            teams.add(t);
            if(i == g.maxTeamAmount){
                e.futureMatches.add(new Match(teams, g));
                t.gamesThisRound++;
                teams.clear();
            }
            i++;
        }
    }

    public static boolean generateMatches(){
        int lower = EventProxy.event.teams.getFirst().gamesThisRound;
        for(Team t : EventProxy.event.teams){
            if(t.gamesThisRound < lower){
                lower = t.gamesThisRound;
            }
        }
        if(lower == EventProxy.event.current.amountOfGames){
            return false;
        }

        ArrayList<Team> necessary = new ArrayList<>();

        for(Team t : EventProxy.event.teams){
            if(t.gamesThisRound == lower){
                necessary.add(t);
            }
        }

        Game g = EventProxy.event.current;

        if(g.type == Type.normal){
            return doDefaultMatchMaking(necessary);
        }else if(g.type == Type.knockout){
            return doKnockoutMatchMaking(necessary);
        }else if(g.type == Type.knockout_with_losers){
            return doKnockoutMatchMakingWithLosers(necessary);
        }

        return false;
    }

    public static boolean doDefaultMatchMaking(ArrayList<Team> teams){
        //TODO disable duplicates
        Event e = EventProxy.event;
        Game g = e.current;
        List<Team> tea = new ArrayList<>();
        int i = 0;
        boolean success = false;
        for(Team t : teams){
            tea.add(t);
            if(i == g.maxTeamAmount){
                success = true;
                e.futureMatches.add(new Match(tea, g));
                t.gamesThisRound++;
                tea.clear();
            }
            i++;
        }
        return success;
    }

    public static boolean doKnockoutMatchMaking(ArrayList<Team> teams){
        Event e = EventProxy.event;
        Game g = e.current;
        List<Team> tea = new ArrayList<>();
        int i = 0;
        boolean success = false;
        for(Team t : teams) {
            if(t.eliminated) continue;
            if(!(getLastMatch(t).winner.equals(t))){
                t.eliminated = true;
                continue;
            }
            tea.add(t);
            if(i == g.maxTeamAmount){
                success = true;
                e.futureMatches.add(new Match(tea, g));
                t.gamesThisRound++;
                tea.clear();
            }
            i++;
        }
        return success;
    }

    public static boolean doKnockoutMatchMakingWithLosers(ArrayList<Team> teams){
        Event e = EventProxy.event;
        Game g = e.current;
        List<Team> tea = new ArrayList<>();
        ArrayList<Team> losers = new ArrayList<>();
        int i = 0;
        boolean success = false;
        for(Team t : teams) {
            if(t.eliminated) continue;
            if(getLosses(t) >= 2){
                t.eliminated = true;
                continue;
            }
            if(!(getLastMatch(t).winner.equals(t))){
                losers.add(t);
                continue;
            }
            tea.add(t);
            if(i == g.maxTeamAmount){
                success = true;
                e.futureMatches.add(new Match(tea, g));
                t.gamesThisRound++;
                tea.clear();
            }
            i++;
        }
        tea.clear();
        i = 0;
        for(Team t : losers){
            tea.add(t);
            if(i == g.maxTeamAmount){
                success = true;
                e.futureMatches.add(new Match(tea, g));
                t.gamesThisRound++;
                tea.clear();
            }
            i++;
        }
        return success;
    }

    public static Match getRunningMatch(String player){
        for(Match m : EventProxy.event.runningMatches){
            if(m.teams.contains(EventProxy.event.getTeam(player))){
                return m;
            }
        }
        return null;
    }

    public void end(){
        running = false;
        EventProxy.event.runningMatches.remove(this);
        EventProxy.event.previousMatches.add(this);
        //TODO update points (and winners in match)
        Match next = EventProxy.event.futureMatches.poll();
        if(next == null && !generateMatches()){
            //TODO round end & pause
            return;
        }
        next = EventProxy.event.futureMatches.poll();
        next.start();
    }

    public void start(){
        running = true;
        server = game.getFreeServer();
        if(server == null) return;
        EventProxy.event.runningMatches.add(this);
        //TODO countdowns and delays
        //TODO ready checks
        for(Team t : teams){
            Player play = null;
            for(String player : t.players.keySet()){
                if(!Event.server.getPlayer(player).isPresent()){
                    //TODO do stuff for when player is offline
                }
                Player p = Event.server.getPlayer(player).get();
                play = p;
                p.createConnectionRequest(server);
            }
            play.getCurrentServer().get().sendPluginMessage(EventProxy.CRYSTAL_CHANNEL, update_message(t.players.keySet()).toByteArray());
        }
    }

    public static ArrayList<Match> getPreviousMatches(Team t){
        ArrayList<Match> matches = new ArrayList<>();
        for(Match m : EventProxy.event.previousMatches){
            if(m.teams.contains(t)){
                matches.add(m);
            }
        }
        return matches;
    }

    public static Match getLastMatch(Team t){
        Match highestID = getPreviousMatches(t).getFirst();
        for(Match m : getPreviousMatches(t)){
            if(m.id > highestID.id) highestID = m;
        }
        return highestID;
    }

    public static int getLosses(Team t){
        int losses = 0;
        for(Match m : getPreviousMatches(t)){
            if(!m.winner.equals(t)) losses++;
        }
        return losses;
    }

    public static boolean havePlayedBefore(Team t1, Team t2){
        List<Match> difference = getPreviousMatches(t1).stream().filter(e -> getPreviousMatches(t2).contains(e)).toList();
        if(difference.isEmpty()) return false;
        return true;
    }

    public static ArrayList<Team> haveNotPlayedBefore(Team team){
        ArrayList<Team> fin = new ArrayList<>();
        for(Team t2 : EventProxy.event.teams){
            if(t2.equals(team)) continue;
            if(havePlayedBefore(team, t2)) continue;
            fin.add(team);
        }
        return fin;
    }

    public ByteArrayDataOutput update_message(Set<String> players) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Event");
        for (String p : players) {
            out.writeUTF(p);
        }
        return out;
    }
}
class Game{
    HashMap<RegisteredServer, Boolean> servers;
    String name;
    Type type;
    int amountOfGames;
    int maxTeamAmount;

    public Game(String name, String type, int amountOfGames, int maxTeamAmount) {
        this.name = name;
        this.type = Type.getType(type);
        this.amountOfGames = amountOfGames;
        this.maxTeamAmount = maxTeamAmount;
    }

    public void getGameServers(ProxyServer proxyServer){
        for (RegisteredServer rs : proxyServer.getAllServers()) {
            if (rs.getServerInfo().getName().contains(name)) {
                servers.put(rs, true);
            }
        }
    }

    public RegisteredServer getFreeServer(){
        for(RegisteredServer rs : servers.keySet()){
            if(servers.get(rs)) return rs;
        }
        return null;
    }
}

enum Type{
    normal,
    knockout,
    knockout_with_losers;
    public static Type getType(String type){
        if(type.equals("default")) return normal;
        for(Type t : Type.values()){
            if(t.toString().equals(type)) return t;
        }
        return normal;
    }
}
