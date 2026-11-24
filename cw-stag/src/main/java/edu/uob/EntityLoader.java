package edu.uob;

import com.alexmerz.graphviz.ParseException;
import com.alexmerz.graphviz.Parser;
import com.alexmerz.graphviz.objects.Graph;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class EntityLoader {

    private ArrayList<Graph> rawGraphData;

    public EntityLoader(File entitiesFile) {
        try {
            Parser parser = new Parser();
            parser.parse(new FileReader(entitiesFile));
            rawGraphData = parser.getGraphs();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.out.println("some thing went wrong when reading entitiesFile");
        } catch (ParseException e){
            System.out.println(e.getMessage());
            System.out.println("some thing went wrong when parsing entitiesFile");
        }
    }

    public ArrayList<Graph> getRawGraphData() { return rawGraphData; }
}
