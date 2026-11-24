package edu.uob;

import java.util.HashMap;

public class PlayerEntity extends GameEntity implements EntityHolder {

    static final int MAX_HEALTH = 3;
    static final int MIN_HEALTH = 0;
    private final HashMap<String, GameEntity> belongings;
    private String currentLocation;
    private int health;

    public PlayerEntity(String name, String description, String location) {
        super(name, description, "player");
        belongings = new HashMap<>();
        currentLocation = location;
        health = MAX_HEALTH;
    }

    @Override
    public void addEntity(String name, String description, String type) {
        belongings.put(name, new GameEntity(name, description, type) {});
    }

    @Override
    public void removeEntity(String name) { belongings.remove(name); }

    @Override
    public GameEntity getEntity(String name) { return belongings.get(name); }

    @Override
    public boolean containsEntity(String name) { return belongings.containsKey(name); }

    public void setCurrentLocation(String location){
        currentLocation = location;
    }

    public void increaseHealth() { if (health < MAX_HEALTH) { health +=1 ; }}
    public void decreaseHealth() { if (health > MIN_HEALTH) { health -=1 ; }}
    public void resetHealth() {this.health = MAX_HEALTH; }

    public String playerInvToString() {
        StringBuilder result = new StringBuilder();
        belongings.keySet().forEach(item -> result.append(item).append("\n"));
        return result.toString();
    }

    public HashMap<String, GameEntity> getBelongings() { return belongings; }
    public String getLocation() { return currentLocation; }
    public int getHealth() { return health; }
}
