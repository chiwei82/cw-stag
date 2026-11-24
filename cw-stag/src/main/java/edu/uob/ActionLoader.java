package edu.uob;

import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public class ActionLoader {

    private Document rawActionData;

    public ActionLoader(File actionsFile) {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            rawActionData = builder.parse(actionsFile);
            rawActionData.getDocumentElement().normalize();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public Document getRawActionData() { return rawActionData; }
}