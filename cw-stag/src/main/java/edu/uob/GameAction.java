package edu.uob;

import org.w3c.dom.NodeList;

import java.util.Collection;
import java.util.HashSet;

public class GameAction
{
    private HashSet<String> triggers;
    private HashSet<String> subjects;
    private HashSet<String> consumed;
    private HashSet<String> produced;
    private String narration;

    public GameAction() {
        triggers = new HashSet<>();
        subjects = new HashSet<>();
        consumed = new HashSet<>();
        produced = new HashSet<>();
        narration = "";
    }

    public void setAttributes(
            NodeList triggers,
            NodeList subjects,
            NodeList consumed,
            NodeList produced,
            String narration )
    {
        addFromNodeList(this.triggers, triggers);
        addFromNodeList(this.subjects, subjects);
        addFromNodeList(this.consumed, consumed);
        addFromNodeList(this.produced, produced);
        this.narration = narration;
    }

    // helper
    private void addFromNodeList(Collection<String> target, NodeList source) {
        for (int i = 0; i < source.getLength(); i++) {
            target.add(source.item(i).getTextContent().toLowerCase());
        }
    }

    public HashSet<String> getTriggers() { return triggers; }
    public HashSet<String> getSubjects() { return subjects; }
    public HashSet<String> getConsumed() { return consumed; }
    public HashSet<String> getProduced() { return produced; }
    public String getNarration() { return narration ; }
}
