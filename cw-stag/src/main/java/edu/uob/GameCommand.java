package edu.uob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class GameCommand {
    private final ArrayList<String> triggers;
    private final HashSet<String> impactedEntities;
    private final HashSet<String> mentionedLocation;
    private final String normalizedCommand;

    public GameCommand(
            String rawCommand,
            HashSet<String> allEntityNames,
            HashSet<String> allTriggers,
            HashMap<String, LocationEntity> locations
    ) {
        this.normalizedCommand = normalizeCommand(rawCommand);
        this.mentionedLocation = findMentionedLocations(locations);
        this.impactedEntities = findImpactedEntities(allEntityNames);
        this.triggers = findTriggers(allTriggers);
    }

    private String normalizeCommand(String rawCommand) {
        return rawCommand
                .toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private ArrayList<String> findTriggers(HashSet<String> allTriggers) {
        ArrayList<String> found = new ArrayList<>();
        for (String trigger : allTriggers) {
            if (trigger.contains(" ")) {
                if (normalizedCommand.contains(trigger)){
                    found.add(trigger);
                }
            } else {
                for (String word : normalizedCommand.split("\\s+")) {
                    if (word.equals(trigger)) {
                        found.add(trigger);
                        break;
                    }
                }
            }
        }
        return found;
    }

    private HashSet<String> findMentionedLocations(HashMap<String, LocationEntity> locations) {
        HashSet<String> found = new HashSet<>();
        for (String location : locations.keySet()) {
            for (String word : normalizedCommand.split("\\s+")) {
                if (word.equals(location)) {
                    found.add(location);
                    break;
                }
            }
        }
        return found;
    }

    private HashSet<String> findImpactedEntities(HashSet<String> allEntityNames) {
        HashSet<String> found = new HashSet<>();
        for (String word : normalizedCommand.split("\\s+")) {
            if (allEntityNames.contains(word)) {
                found.add(word);
            }
        }
        return found;
    }

    public boolean hasExtraneousEntities(HashSet<String> subjects) {
        for (String entity : impactedEntities) {
            if (!subjects.contains(entity)){
                return true;
            }
        }
        return false;
    }

    public ArrayList<String> getTriggers() { return triggers; }
    public HashSet<String> getImpactedEntities() { return impactedEntities; }
    public HashSet<String> getMentionedLocation() { return mentionedLocation; }
}