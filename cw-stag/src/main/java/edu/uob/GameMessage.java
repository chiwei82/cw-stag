package edu.uob;

final class GameMessage {
    private GameMessage() {}

    // fixed responses
    static final String INVALID_PLAYER_NAME = "invalid player name";
    static final String INVALID_COMMAND = "invalid command";
    static final String LOCATION_NOT_EXIST = "That location does not exist";
    static final String PATH_NOT_EXIST = "The path does not exist. You can't go there";
    static final String SPECIFY_ONE_GET = "Please specify exact one item to get";
    static final String SPECIFY_ONE_DROP = "Please specify exact one item to drop";
    static final String SPECIFY_ONE_GOTO = "Please specify exact one location to go to";
    static final String EXTRANEOUS_GOTO = "Unexpected extraneous entities in goto command";
    static final String UNKNOWN_COMMAND = "Unknown command";
    static final String DIED = "you died and lost all of your items, you must return to the start of the game";
    static final String AMBIGUOUS_ACTION = "There is more than one action possible - which one do you want to perform?\n";

    // dynamic responses
    static String pickedUp(String item) { return "You picked up: " + item; }
    static String notAvailable(String item) { return item + " is not available for you to get"; }
    static String dropped(String item) { return "You dropped: " + item; }
    static String notInInventory(String item) { return item + " is not in your inventory"; }
    static String movedTo(String location) { return "You moved to: " + location; }
}
