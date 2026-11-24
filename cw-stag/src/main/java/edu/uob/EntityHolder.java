package edu.uob;

interface EntityHolder {
    void addEntity(String name, String description, String type);
    void removeEntity(String name);
    GameEntity getEntity(String name);
    boolean containsEntity(String name);
}
