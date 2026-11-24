package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class STAGTests {

    private GameServer server;

    // Create a new server _before_ every @Test
    @BeforeEach
    void setup() {
        File entitiesFile = Paths.get("config" + File.separator + "basic-entities.dot").toAbsolutePath().toFile();
        File actionsFile = Paths.get("config" + File.separator + "basic-actions.xml").toAbsolutePath().toFile();

        server = new GameServer(entitiesFile, actionsFile);
    }

    String sendCommandToServer(String command) {
        // Try to send a command to the server - this call will timeout if it takes too long (in case the server enters an infinite loop)
        return assertTimeoutPreemptively(Duration.ofMillis(1000), () -> { return server.handleCommand(command);},
                "Server took too long to respond (probably stuck in an infinite loop)");
    }
    
    // Add more unit tests or integration tests here.
    @Test
    void testDrop(){
        sendCommandToServer("simon: get potion");
        sendCommandToServer("simon: drop potion");

        String response = sendCommandToServer("simon: look");
        assertTrue(response.contains("potion"));

        response = sendCommandToServer("simon: inv");
        assertFalse(response.contains("potion"));
    }

    @Test
    void testInv(){
        String response;
        assertTrue(sendCommandToServer("simon: inv").isEmpty());

        response = sendCommandToServer("simon: inv");
        assertFalse(response.contains("potion"));

        sendCommandToServer("simon: get potion");
        response = sendCommandToServer("simon: inv");
        assertTrue(response.contains("potion"));
    }

    @Test
    void testGotoNonExistedPath() {
        sendCommandToServer("simon: goto cellar");
        String response = sendCommandToServer("simon: look");
        assertFalse(response.contains("cellar"));
    }

    @Test
    void testCaseInsensitive() {
        assertTrue(sendCommandToServer("simon: LOOK").contains("trapdoor"));
        assertTrue(sendCommandToServer("simon: Look").contains("trapdoor"));
        assertTrue(sendCommandToServer("Simon: Look").contains("trapdoor"));
    }

    @Test
    void testDecoratedCommand() {
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: get key");
        sendCommandToServer("simon: goto cabin");
        // "please open the trapdoor using the key"
        String response = sendCommandToServer("simon: hi server please open the trapdoor using the key");
        assertTrue(response.contains("cellar"));
    }

    @Test
    void testWordOrdering() {
        // On Teams, QA says:
        // "fight hit elf" it is clear
        // 1. what action the user is trying to perform
        // 2. and there is no alternative action that might be valid
        sendCommandToServer("simon: forest goto");
        sendCommandToServer("simon: key get");
        sendCommandToServer("simon: goto cabin");
        String response = sendCommandToServer("simon: unlock open trapdoor");
        assertTrue(response.contains("cellar"));
    }

    @Test
    void testPartialCommandWithSubjectOnly() {
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: get key");
        sendCommandToServer("simon: goto cabin");
        // only impacted subject mentioned
        String response = sendCommandToServer("simon: unlock trapdoor");
        assertTrue(response.contains("cellar"));
    }

    @Test
    void testPartialCommandWithConsumedItemOnly() {
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: get key");
        sendCommandToServer("simon: goto cabin");
        // only consumed item is provided
        String response = sendCommandToServer("simon: unlock with key");
        assertTrue(response.contains("cellar"));
    }

    @Test
    void testExtraneousEntitiesRejected() {
        sendCommandToServer("simon: get potion");
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: get key");
        sendCommandToServer("simon: goto cabin");
        // "potion" is not a subject of open action → must be rejected
        sendCommandToServer("simon: open trapdoor with key and potion");

        String response = sendCommandToServer("simon: look");
        assertFalse(response.contains("cellar"));
    }

    @Test
    void testVariableWhitespace() {
        String response = sendCommandToServer("simon:   look    ");
        assertTrue(response.contains("cabin"));
    }

    @Test
    void testCompositeCommandRejected() {
        sendCommandToServer("simon: get axe and get potion");
        // invalid command -> get no item
        String response = sendCommandToServer("simon: inv");
        assertFalse(response.contains("axe") && response.contains("potion"));
    }

    @Test
    void testPlayersHaveSeparateState() {
        String response;

        sendCommandToServer("simon: get potion");
        sendCommandToServer("pimpi: get potion");
        // pimpi can't get item from other player
        response = sendCommandToServer("pimpi: inv");
        assertFalse(response.contains("potion"));
        // but look should show potion is gone from cabin
        response = sendCommandToServer("pimpi: look");
        assertFalse(response.contains("potion"));
    }

    @Test
    void testInvalidPlayerName() {
        sendCommandToServer("s!mon: look");
        String response = sendCommandToServer("pimpi: look");
        assertFalse(response.contains("s!mon"));
    }

    @Test
    void testValidPlayerName() {
        String response;
        // uppercase and lowercase letters
        sendCommandToServer("Simon: look");
        response = sendCommandToServer("pimpi: look");
        assertTrue(response.contains("simon"));

        // apostrophes
        sendCommandToServer("s'mon: look");
        response = sendCommandToServer("pimpi: look");
        assertTrue(response.contains("s'mon"));

        // hyphens
        sendCommandToServer("si-mon: look");
        response = sendCommandToServer("pimpi: look");
        assertTrue(response.contains("si-mon"));

        // spaces
        sendCommandToServer("simon lock: look");
        response = sendCommandToServer("pimpi: look");
        assertTrue(response.contains("simon lock"));
    }

    @Test
    void testCanOnlyGetArtefact() {
        String response;
        // forest is a location
        sendCommandToServer("simon: get forest");
        response = sendCommandToServer("simon: inv");
        assertFalse(response.contains("forest"));

        // trapdoor is furniture
        sendCommandToServer("simon: get trapdoor");
        response = sendCommandToServer("simon: inv");
        assertFalse(response.contains("trapdoor"));

        // key is an artefact
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: get key");

        response = sendCommandToServer("simon: inv");
        assertTrue(response.contains("key"));

        sendCommandToServer("simon: unlock with key");
        sendCommandToServer("simon: goto cellar");
        // elf is a character
        sendCommandToServer("simon: get elf");
        response = sendCommandToServer("simon: inv");
        assertFalse(response.contains("elf"));

        // pimpi is a player
        sendCommandToServer("pimpi: goto cellar");
        sendCommandToServer("simon: get pimpi");
        response = sendCommandToServer("simon: inv");
        assertFalse(response.contains("pimpi"));
    }

    @Test
    void testSubjectMustBeAvailable() {
        // key is in forest, not in cabin — open should fail
        String response = sendCommandToServer("simon: open trapdoor with key");
        // cellar path should NOT appear
        assertFalse(sendCommandToServer("simon: look").contains("cellar"));
    }

    @Test
    void testCanUseInventoryItemAsSubject() {
        sendCommandToServer("simon: get axe");
        sendCommandToServer("simon: goto forest");
        // axe is in inventory, tree is in forest — chop should work
        sendCommandToServer("simon: chop tree");
        String response = sendCommandToServer("simon: look");
        assertTrue(response.contains("log"));
    }

    @Test
    void testAmbiguous(){
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: get key");

        // inv inventory -> we can identify what user wants to do
        assertTrue(sendCommandToServer("simon: inv inventory").contains("key"));

        // inv goto ; goto is invalid here since location is not specified
        // also two triggers are mentioned
        assertFalse(sendCommandToServer("simon: inv goto ").contains("key"));

        sendCommandToServer("simon: goto cabin");
        // unlock open trapdoor -> multi keyphrases triggered but only one action is valid
        assertFalse(sendCommandToServer("simon: inv unlock with key ").contains("key"));
        assertTrue(sendCommandToServer("simon: unlock open trapdoor").contains("cellar"));
    }

    @Test
    void testGetNoItem() {
        sendCommandToServer("simon: get    ");
        assertTrue(sendCommandToServer("simon: inv").isEmpty());
    }

    @Test
    void testGetItemElsewhere() {
        sendCommandToServer("simon: get key"); // key is in forest, not cabin
        assertFalse(sendCommandToServer("simon: inv").contains("key"));
    }

    @Test
    void testDropNotOwned() {
        String response = sendCommandToServer("simon: drop potion");
        assertTrue(response.contains("not in your inventory"));
    }

    @Test
    void testGotoUnknown() {
        sendCommandToServer("simon: goto MVB");
        assertTrue(sendCommandToServer("simon: look").contains("cabin"));
    }

    @Test
    void testGotoTwoLocations() {
        sendCommandToServer("simon: goto forest cellar");
        assertTrue(sendCommandToServer("simon: look").contains("cabin"));
    }

    @Test
    void testProducedItemAppearsInRoom() {
        sendCommandToServer("simon: get axe");
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: chop tree");
        assertTrue(sendCommandToServer("simon: look").contains("log"));
    }

    @Test
    void testOtherPlayerBlocksAction() {
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: get key");
        sendCommandToServer("simon: goto cabin");
        sendCommandToServer("pimpi: look");
        sendCommandToServer("pimpi: open trapdoor");
        assertFalse(sendCommandToServer("pimpi: look").contains("cellar"));
    }

    @Test
    void testPathProduced(){
        assertFalse(sendCommandToServer("simon:look").contains("cellar"));
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: get key");
        sendCommandToServer("simon: goto cabin");
        sendCommandToServer("simon: unlock trapdoor");
        sendCommandToServer("simon: goto cellar");
        sendCommandToServer("simon: goto cabin");
        assertTrue(sendCommandToServer("simon:look").contains("cellar"));
    }

    @Test
    void testItemProduced(){
        sendCommandToServer("simon: get axe");
        sendCommandToServer("simon: goto forest");
        assertFalse(sendCommandToServer("simon: look").contains("log"));
        sendCommandToServer("simon: chop tree");
        assertTrue(sendCommandToServer("simon: look").contains("log"));
    }

    @Test
    void testItemConsumed(){
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: get key");
        sendCommandToServer("simon: goto cabin");
        sendCommandToServer("simon: unlock trapdoor");
        assertFalse(sendCommandToServer("simon: inv").contains("key"));
    }

    @Test
    void testTriggerBuiltAndActionAtSameTime() {
        sendCommandToServer("simon: get axe");
        sendCommandToServer("simon: goto forest");

        sendCommandToServer("simon: inv cut tree");
        assertTrue(sendCommandToServer("simon: look").contains("tree"));

        sendCommandToServer("simon: cut inv tree");
        assertTrue(sendCommandToServer("simon: look").contains("tree"));

        sendCommandToServer("simon: cut tree inv");
        assertTrue(sendCommandToServer("simon: look").contains("tree"));
    }
}
