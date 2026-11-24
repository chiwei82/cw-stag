package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class ExtendedSTAGTests {

    private GameServer server;

    @BeforeEach
    void setup() {
        File entitiesFile = Paths.get("config" + File.separator + "extended-entities.dot").toAbsolutePath().toFile();
        File actionsFile = Paths.get("config" + File.separator + "extended-actions.xml").toAbsolutePath().toFile();
        server = new GameServer(entitiesFile, actionsFile);
    }

    String sendCommandToServer(String command) {
        return assertTimeoutPreemptively(Duration.ofMillis(1000), () -> server.handleCommand(command),
                "Server took too long to respond (probably stuck in an infinite loop)");
    }

    // unlocks trapdoor from cabin and moves player down to cellar
    private void goToCellar(String player) {
        sendCommandToServer(player + ": goto forest");
        sendCommandToServer(player + ": get key");
        sendCommandToServer(player + ": goto cabin");
        sendCommandToServer(player + ": unlock trapdoor");
        sendCommandToServer(player + ": goto cellar");
    }

    @Test
    void testMultiplePlayers() {
        sendCommandToServer("simon: look");
        sendCommandToServer("pimpi: hi Teacher Simon!");
        // simon should see pimpi even though pimpi didn't execute any valid action
        String response = sendCommandToServer("simon: look");
        assertTrue(response.contains("pimpi"));
    }

    @Test
    void testHealth() {
        assertEquals("3", sendCommandToServer("simon: health"));
        goToCellar("simon");
        sendCommandToServer("simon: hit elf");
        assertEquals("2", sendCommandToServer("simon: health"));
    }

    @Test
    void testDeath() {
        goToCellar("simon");
        sendCommandToServer("simon: hit elf");
        sendCommandToServer("simon: hit elf");
        String response = sendCommandToServer("simon: hit elf");
        assertEquals("3", sendCommandToServer("simon: health"));
        assertTrue(sendCommandToServer("simon: inv").isEmpty());
        assertTrue(sendCommandToServer("simon: look").contains("cabin"));
    }

    @Test
    void testDeathItemsDropped() {
        sendCommandToServer("simon: get axe");
        goToCellar("simon");
        sendCommandToServer("simon: hit elf");
        sendCommandToServer("simon: hit elf");
        sendCommandToServer("simon: hit elf");
        assertFalse(sendCommandToServer("simon: inv").contains("axe"));
        sendCommandToServer("simon: goto cellar");
        assertTrue(sendCommandToServer("simon: look").contains("axe"));
    }

    @Test
    void testDrinkPotion() {
        sendCommandToServer("simon: get potion");
        goToCellar("simon");
        sendCommandToServer("simon: hit elf");
        assertEquals("2", sendCommandToServer("simon: health"));
        sendCommandToServer("simon: goto cabin");
        sendCommandToServer("simon: drink potion");
        assertEquals("3", sendCommandToServer("simon: health"));
    }

    @Test
    void testDrinkPotionAtFullHealth() {
        sendCommandToServer("simon: get potion");
        sendCommandToServer("simon: drink potion");
        assertEquals("3", sendCommandToServer("simon: health"));
    }

    @Test
    void testMultiWordTrigger() {
        sendCommandToServer("simon: get axe");
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: cut down tree");
        assertTrue(sendCommandToServer("simon: look").contains("log"));
    }

}
