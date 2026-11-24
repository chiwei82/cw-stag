package edu.uob;

import java.util.HashSet;

final class GameValidation {
    private GameValidation() {}

    static boolean isValidPlayerName(String playerName){
        if (playerName == null || playerName.isEmpty()) return false;
        for (char c : playerName.toCharArray()) {
            boolean isLetter = Character.isLetter(c);
            boolean isSpace = c == ' ';
            boolean isApostrophe = c == '\'';
            boolean isHyphen = c == '-';
            if (!isLetter && !isSpace && !isApostrophe && !isHyphen) return false;
        }
        return true;
    }

    static boolean isSingleTriggered(GameCommand parsedCommand) {
        HashSet<String> uniqueCount = new HashSet<>();
        for (String trigger : parsedCommand.getTriggers()) {
            uniqueCount.add(trigger.equals("inventory") ? "inv" : trigger);
        }
        return uniqueCount.size() == 1;
    }

    static boolean hasMentionedASubject(GameAction action, GameCommand parsedCommand) {
        for (String subject : action.getSubjects()) {
            if (parsedCommand.getImpactedEntities().contains(subject)) return true;
        }
        return false;
    }

    static boolean areSubjectsAvailable(PlayerEntity player, HashSet<String> subjects, LocationEntity location) {
        for (String subject : subjects) {
            boolean inLocation = location.containsEntity(subject) || location.getName().equals(subject);
            boolean inInventory = player.containsEntity(subject);
            if (!inLocation && !inInventory) return false;
        }
        return true;
    }
}
