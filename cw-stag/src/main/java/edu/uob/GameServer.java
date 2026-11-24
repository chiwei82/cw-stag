package edu.uob;

import com.alexmerz.graphviz.objects.Edge;
import com.alexmerz.graphviz.objects.Graph;
import com.alexmerz.graphviz.objects.Node;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.*;

public final class GameServer {

    private static final char END_OF_TRANSMISSION = 4;
    private static final int INDEX_OF_LOCATIONS = 0;
    private static final int INDEX_OF_PATHS = 1;
    private static final String STOREROOM_NAME = "storeroom";
    private static final String STOREROOM_DESCRIPTION = "Storage for any entities not placed in the game";
    private static final String PLAYER_DESCRIPTION = "a player";
    private static final List<String> BUILT_IN_COMMANDS = List.of("inventory", "inv", "get", "drop", "goto", "look", "health");

    private HashMap<String, LocationEntity> locations;
    private HashMap<String, PlayerEntity> playerMap = new HashMap<>();
    private HashMap<String, ArrayList<GameAction>> actionMap;
    private HashSet<String> allEntityNames; // for identify entities in command
    private HashSet<String> allTriggers;
    private String initialLocation;

    public static void main(String[] args) throws IOException {
        File entitiesFile = Paths.get("config" + File.separator + "extended-entities.dot").toAbsolutePath().toFile();
        File actionsFile = Paths.get("config" + File.separator + "extended-actions.xml").toAbsolutePath().toFile();
        GameServer server = new GameServer(entitiesFile, actionsFile);
        server.blockingListenOn(8888);
    }

    /**
    * Do not change the following method signature or we won't be able to mark your submission
    * Instanciates a new server instance, specifying a game with some configuration files
    *
    * @param entitiesFile The game configuration file containing all game entities to use in your game
    * @param actionsFile The game configuration file containing all game actions to use in your game
    */
    public GameServer(File entitiesFile, File actionsFile) {
        EntityLoader entityLoader = new EntityLoader(entitiesFile);
        ArrayList<Graph> rawEntityData = entityLoader.getRawGraphData();

        ActionLoader actionLoader = new ActionLoader(actionsFile);
        Document rawActionData = actionLoader.getRawActionData();

        // add build-in triggerWord
        allTriggers = new HashSet<>();
        allTriggers.addAll(BUILT_IN_COMMANDS);

        // set entity & action
        loadEntity(rawEntityData);
        loadAction(rawActionData);
    }

    /**
    * Do not change the following method signature or we won't be able to mark your submission
    * This method handles all incoming game commands and carries out the corresponding actions.</p>
    *
    * @param command The incoming command to be processed
    */
    public String handleCommand(String command) {
        String playerName = command.substring(0, command.indexOf(':')).trim().toLowerCase();
        String playerCommand = command.substring(command.indexOf(':')+1);

        if (!playerMap.containsKey(playerName)) {
            if (!GameValidation.isValidPlayerName(playerName)) {
                return GameMessage.INVALID_PLAYER_NAME;
            }

            // create new player
            PlayerEntity newPlayer = new PlayerEntity(playerName, PLAYER_DESCRIPTION, initialLocation);
            playerMap.put(playerName, newPlayer);
            locations.get(initialLocation).addPlayer(newPlayer);
        }

        // tidy up command -> loose matching -> validation -> execute
        PlayerEntity player = playerMap.get(playerName);
        GameCommand parsedCommand = new GameCommand(playerCommand, allEntityNames, allTriggers, locations);
        if (parsedCommand.getTriggers().isEmpty()) {
            return GameMessage.INVALID_COMMAND;
        }

        // problematic
        for (String trigger : parsedCommand.getTriggers()){
            if (BUILT_IN_COMMANDS.contains(trigger)){
                return handleBuiltInCommand(trigger, player, parsedCommand);
            }
        }
        return handleCustomCommand(player, parsedCommand);
    }

    // execute
    private void loadEntity(ArrayList<Graph> rawEntityData){
        locations = new HashMap<>();
        allEntityNames = new HashSet<>();
        ArrayList<Graph> subgraphs = rawEntityData.get(0).getSubgraphs();
        boolean storeroomFound = isStoreroomFound(subgraphs.get(INDEX_OF_LOCATIONS));
        setPaths(subgraphs.get(INDEX_OF_PATHS));
        if (!storeroomFound) {
            locations.put(STOREROOM_NAME, new LocationEntity(STOREROOM_NAME, STOREROOM_DESCRIPTION));
        }
    }

    // validation
    private boolean isStoreroomFound(Graph locationsGraph) {
        boolean storeroomFound = false;
        for (Graph locationGraph : locationsGraph.getSubgraphs()) {
            String locationName = loadLocationName(locationGraph);
            if (locationName.equalsIgnoreCase(STOREROOM_NAME)){
                storeroomFound = true;
            }
            if (initialLocation == null) {
                initialLocation = locationName;
            }
            setLocationEntities(locationGraph, locationName);
        }
        return storeroomFound;
    }

    // helper + setter (set location, location name)
    private String loadLocationName(Graph locationGraph) {
        Node node = locationGraph.getNodes(false).get(0);
        String name = node.getId().getId().toLowerCase();
        // put location entity into locations hashmap
        locations.put(name, new LocationEntity(name, node.getAttribute("description")));
        // put location name into all entity names
        allEntityNames.add(name);
        return name;
    }

    // execute + setter (set item, item name)
    private void setLocationEntities(Graph locationGraph, String locationName) {
        for (Graph typeGraph : locationGraph.getSubgraphs()) {
            String entityType = typeGraph.getId().getId();
            for (Node node : typeGraph.getNodes(false)) {
                String entityName = node.getId().getId().toLowerCase();
                // put item entity into locations map
                locations.get(locationName).addEntity(entityName, node.getAttribute("description"), entityType);
                // put item name into all entity names
                allEntityNames.add(entityName);
            }
        }
    }

    // execute
    private void setPaths(Graph pathsGraph) {
        for (Edge edge : pathsGraph.getEdges()) {
            String source = edge.getSource().getNode().getId().getId();
            String target = edge.getTarget().getNode().getId().getId();
            locations.get(source).addPath(target);
        }
    }

    // execute
    private void loadAction(Document rawActionData) {
        actionMap = new HashMap<>();
        NodeList actions = rawActionData.getElementsByTagName("action");

        for (int i = 0; i < actions.getLength(); i++) {
            GameAction actionEntity = new GameAction();
            Element action = (Element) actions.item(i);

            NodeList triggers = getChildNodes(action, "triggers", "keyphrase");
            NodeList subjects = getChildNodes(action, "subjects", "entity");
            NodeList consumed = getChildNodes(action, "consumed", "entity");
            NodeList produced = getChildNodes(action, "produced", "entity");
            String narration  = action.getElementsByTagName("narration").item(0).getTextContent();

            actionEntity.setAttributes(triggers, subjects, consumed, produced, narration);
            for (String triggerWord : actionEntity.getTriggers()) {
                if (!actionMap.containsKey(triggerWord)) {
                    actionMap.put(triggerWord, new ArrayList<>());
                }
                actionMap.get(triggerWord).add(actionEntity);
                allTriggers.add(triggerWord);
            }
        }
    }

    // helper
    private NodeList getChildNodes(Element parent, String tag, String childTag) {
        return ((Element) parent.getElementsByTagName(tag).item(0)).getElementsByTagName(childTag);
    }

    // message + execute
    private String acquireItem(PlayerEntity player, String itemName) {
        LocationEntity currentLoc = locations.get(player.getLocation());
        if (!currentLoc.getArtefacts().containsKey(itemName)) {
            return GameMessage.notAvailable(itemName);
        }
        moveEntityTo(itemName, player, player);
        return GameMessage.pickedUp(itemName);
    }

    // message + execute
    private String dropItem(PlayerEntity player, String itemName) {
        if (!player.containsEntity(itemName)) {
            return GameMessage.notInInventory(itemName);
        };
        moveEntityTo(itemName, player, locations.get(player.getLocation()));
        return GameMessage.dropped(itemName);
    }

    // message + execute
    private String movePlayer(PlayerEntity player, String targetLocation) {
        if (!locations.containsKey(targetLocation)) {
            return GameMessage.LOCATION_NOT_EXIST;
        }
        if (!locations.get(player.getLocation()).getPaths().contains(targetLocation)){
            return GameMessage.PATH_NOT_EXIST;
        }
        locations.get(player.getLocation()).removePlayer(player.getName());
        locations.get(targetLocation).addPlayer(player);
        player.setCurrentLocation(targetLocation);
        return GameMessage.movedTo(targetLocation);
    }

    // message
    private String getLocationInformation(String location, String currentPlayerName) {
        if (!locations.containsKey(location)) {
            return GameMessage.LOCATION_NOT_EXIST;
        }
        return locations.get(location).locationInfoToString(currentPlayerName);
    }

    // message + execute
    private String handleBuiltInCommand(String trigger, PlayerEntity player, GameCommand parsedCommand) {
        if (!GameValidation.isSingleTriggered(parsedCommand)){
            return GameMessage.AMBIGUOUS_ACTION;
        }

        switch (trigger) {
            case "look":
                return getLocationInformation(player.getLocation(), player.getName());

            case "inventory":
            case "inv":
                return player.playerInvToString();

            case "get":
                if (parsedCommand.getImpactedEntities().size() != 1) {
                    return GameMessage.SPECIFY_ONE_GET;
                }
                return acquireItem(player, parsedCommand.getImpactedEntities().iterator().next());

            case "drop":
                if (parsedCommand.getImpactedEntities().size() != 1) {
                    return GameMessage.SPECIFY_ONE_DROP;
                }
                return dropItem(player, parsedCommand.getImpactedEntities().iterator().next());

            case "goto":
                if (parsedCommand.getMentionedLocation().size() != 1) {
                    return GameMessage.SPECIFY_ONE_GOTO;
                }
                if (!parsedCommand.getImpactedEntities().isEmpty() &&
                        !parsedCommand.getImpactedEntities().equals(parsedCommand.getMentionedLocation())) {
                    return GameMessage.EXTRANEOUS_GOTO;
                }
                return movePlayer(player, parsedCommand.getMentionedLocation().iterator().next());

            case "health":
                return String.valueOf(player.getHealth());

            default:
                return GameMessage.UNKNOWN_COMMAND;
        }
    }

    // message + execute
    private String handleCustomCommand(PlayerEntity player, GameCommand parsedCommand) {
        ArrayList<GameAction> possibleActions = matchPossibleAction(parsedCommand, actionMap);
        HashSet<GameAction> availableActions = filterValidActions(possibleActions, player, parsedCommand);

        if (availableActions.isEmpty()) return GameMessage.INVALID_COMMAND;
        if (availableActions.size() > 1) {
            StringBuilder sb = new StringBuilder(GameMessage.AMBIGUOUS_ACTION);
            for (GameAction action : availableActions) {
                sb.append("- ").append(action.getNarration()).append("\n");
            }
            return sb.toString();
        }
        return executeAction(player, availableActions.iterator().next());
    }

    private ArrayList<GameAction> matchPossibleAction( GameCommand parsedCommand, HashMap<String, ArrayList<GameAction>> actionMap) {
        ArrayList<GameAction> matchedActionList = new ArrayList<>();
        parsedCommand.getTriggers().forEach(
                (triggerWord) -> {
                    if (actionMap.containsKey(triggerWord)){
                        matchedActionList.addAll(actionMap.get(triggerWord));
            }
        });
        return matchedActionList;
    }

    private HashSet<GameAction> filterValidActions(ArrayList<GameAction> possibleActions, PlayerEntity player, GameCommand parsedCommand) {
        HashSet<GameAction> validActions = new HashSet<>();
        LocationEntity location = locations.get(player.getLocation());

        for (GameAction action : possibleActions) {
            if (!GameValidation.hasMentionedASubject(action, parsedCommand)) continue;
            if (parsedCommand.hasExtraneousEntities(action.getSubjects())) continue;
            if (!GameValidation.areSubjectsAvailable(player, action.getSubjects(), location)) continue;
            if (!isActionValid(action, player, location)) continue;
            validActions.add(action);
        }
        return validActions;
    }

    // validation
    private boolean isActionValid(GameAction action, PlayerEntity player, LocationEntity location) {
        HashSet<String> toCheck = new HashSet<>(action.getConsumed());
        toCheck.addAll(action.getProduced());

        for (String item : toCheck) {
            if (item.equals("health")) continue;
            if (!checkOwnership(item, player)) return false;
            if (!checkAvailability(item, player, location, action)) return false;
        }
        return true;
    }

    // validation
    private boolean checkOwnership(String item, PlayerEntity currentPlayer) {
        for (PlayerEntity other : playerMap.values()) {
            if (other != currentPlayer && other.containsEntity(item)) return false;
        }
        return true;
    }

    // validation
    private boolean checkAvailability(String item, PlayerEntity player, LocationEntity location, GameAction action) {
        if (locations.containsKey(item)) {
            if (action.getConsumed().contains(item) && !location.getPaths().contains(item)) return false;
            if (action.getProduced().contains(item) && location.getPaths().contains(item)) return false;
        } else {
            if (findEntity(item, player) == null) return false;
        }
        return true;
    }

    // message + execute
    private String executeAction(PlayerEntity player, GameAction action) {
        LocationEntity location = locations.get(player.getLocation());
        for (String consumed : action.getConsumed()) {
            if (applyConsumed(consumed, player, location)) {
                resetDeadPlayer(player, location);
                return GameMessage.DIED;
            }
        }
        for (String produced : action.getProduced()) {
            applyProduced(produced, player, location);
        }
        return action.getNarration();
    }

    private boolean applyConsumed(String item, PlayerEntity player, LocationEntity location) {
        if (item.equals("health")) {
            player.decreaseHealth();
            return player.getHealth() == 0;
        }
        if (locations.containsKey(item)) {
            location.removePath(item);
        } else {
            moveEntityTo(item, player, locations.get(STOREROOM_NAME));
        }
        return false;
    }

    private void applyProduced(String item, PlayerEntity player, LocationEntity location) {
        if (item.equals("health")) {
            player.increaseHealth();
            return;
        }
        if (locations.containsKey(item)) {
            location.addPath(item);
        } else {
            moveEntityTo(item, player, location);
        }
    }

    // execute
    private void moveEntityTo(String name, PlayerEntity player, EntityHolder destination) {
        GameEntity entity = findEntity(name, player);
        if (entity == null) {
            return;
        }

        LocationEntity sourceLocation = findLocation(name);
        EntityHolder from;
        if (sourceLocation != null) {
            from =  sourceLocation;
        } else {
            from = player;
        }
        from.removeEntity(name);
        destination.addEntity(entity.getName(), entity.getDescription(), entity.getType());
    }

    // execute
    private void resetDeadPlayer(PlayerEntity player, LocationEntity locationEntity) {
        for (GameEntity item : player.getBelongings().values()) {
            locationEntity.addEntity(item.getName(), item.getDescription(), item.getType());
        }
        player.getBelongings().clear();
        player.resetHealth();
        locations.get(player.getLocation()).removePlayer(player.getName());
        player.setCurrentLocation(initialLocation);
        locations.get(initialLocation).addPlayer(player);
    }

    // search
    private GameEntity findEntity(String name, PlayerEntity player) {
        for (LocationEntity loc : locations.values()) {
            if (loc.containsEntity(name)) return loc.getEntity(name);
        }
        return player.getEntity(name);
    }

    // search
    private LocationEntity findLocation(String name) {
        for (LocationEntity loc : locations.values()) {
            if (loc.containsEntity(name)) return loc;
        }
        return null;
    }

    /**
    * Do not change the following method signature or we won't be able to mark your submission
    * Starts a *blocking* socket server listening for new connections.
    *
    * @param portNumber The port to listen on.
    * @throws IOException If any IO related operation fails.
    */
    public void blockingListenOn(int portNumber) throws IOException {
        try (ServerSocket s = new ServerSocket(portNumber)) {
            System.out.println("Server listening on port " + portNumber);
            while (!Thread.interrupted()) {
                try {
                    blockingHandleConnection(s);
                } catch (IOException e) {
                    System.out.println("Connection closed");
                }
            }
        }
    }

    /**
    * Do not change the following method signature or we won't be able to mark your submission
    * Handles an incoming connection from the socket server.
    *
    * @param serverSocket The client socket to read/write from.
    * @throws IOException If any IO related operation fails.
    */
    private void blockingHandleConnection(ServerSocket serverSocket) throws IOException {
        try (Socket s = serverSocket.accept();
        BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()))) {
            System.out.println("Connection established");
            String incomingCommand = reader.readLine();
            if(incomingCommand != null) {
                System.out.println("Received message from " + incomingCommand);
                String result = handleCommand(incomingCommand);
                writer.write(result);
                writer.write("\n" + END_OF_TRANSMISSION + "\n");
                writer.flush();
            }
        }
    }
}
