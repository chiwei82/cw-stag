package edu.uob;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class LocationEntity extends GameEntity implements EntityHolder {

    private final HashSet<String> paths;
    private final HashMap<String, PlayerEntity> players;
    private final HashMap<String, GameEntity> characters;
    private final HashMap<String, GameEntity> artefacts;
    private final HashMap<String, GameEntity> furniture;

    public LocationEntity(String name, String description) {
        super(name, description, "location");
        paths = new HashSet<>();
        players = new HashMap<>();
        characters = new HashMap<>();
        artefacts = new HashMap<>();
        furniture = new HashMap<>();
    }

    public void addEntity(String name, String description, String type) {
        GameEntity entity = new GameEntity(name, description, type) {};
        if (type.toLowerCase().contains("artefacts")) {
            artefacts.put(name, entity);
        } else if (type.toLowerCase().contains("furniture")) {
            furniture.put(name, entity);
        } else if (type.toLowerCase().contains("characters")) {
            characters.put(name, entity);
        }
    }

    public void addPath(String locationName) {
        paths.add(locationName);
    }
    public void removePath(String locationName) {
        paths.remove(locationName);
    }

    public void addPlayer(PlayerEntity player) {
        players.put(player.getName(), player);
    }
    public void removePlayer(String playerName) {
        players.remove(playerName);
    }

    public boolean containsEntity(String name) {
        return artefacts.containsKey(name) || furniture.containsKey(name) || characters.containsKey(name);
    }

    public GameEntity getEntity(String name) {
        if (artefacts.containsKey(name)) return artefacts.get(name);
        if (furniture.containsKey(name)) return furniture.get(name);
        return characters.get(name);
    }

    public void removeEntity(String name) {
        artefacts.remove(name);
        furniture.remove(name);
        characters.remove(name);
    }

    public String locationInfoToString(String currentPlayerName) {
        StringBuilder result = new StringBuilder();
        result.append(getName()).append(": ").append(getDescription()).append("\n");
        appendEntities(result, "Artefacts", artefacts.values());
        appendEntities(result, "Furniture", furniture.values());
        appendEntities(result, "Characters", characters.values());
        appendEntities(result, "Players", players.values().stream()
                .filter(p -> !p.getName().equals(currentPlayerName))
                .toList());
        result.append("Paths: ");
        paths.forEach(path -> result.append(path).append(", "));
        return result.toString();
    }

    private void appendEntities(StringBuilder sb, String label, Collection<? extends GameEntity> items) {
        sb.append(label).append(": ");
        items.forEach(
            item
                    -> sb.append(item.getName()).append(": ").append(item.getDescription()).append(", ")
        );
        sb.append("\n");
    }

    public HashSet<String> getPaths() { return paths; }
    public HashMap<String, GameEntity> getArtefacts() { return artefacts; }
    public HashMap<String, GameEntity> getFurniture() { return furniture; }
    public HashMap<String, GameEntity> getCharacters() { return characters; }
    public HashMap<String, PlayerEntity> getPlayers() { return players; }
}