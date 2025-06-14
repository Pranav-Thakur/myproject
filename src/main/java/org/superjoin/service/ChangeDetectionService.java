package org.superjoin.service;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.superjoin.dto.CellInfo;
import org.superjoin.dto.ChangeImpact;
import org.superjoin.dto.SheetData;
import org.superjoin.events.SpreadsheetChangeEvent;

import java.util.List;

import static org.neo4j.driver.Values.parameters;

@Service
public class ChangeDetectionService {

    @Autowired
    private KnowledgeGraphService graphService;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private Driver neo4jDriver;

    @EventListener
    public void handleSpreadsheetChange(SpreadsheetChangeEvent event) {
        // Analyze the change
        ChangeImpact impact = analyzeChangeImpact(event);

        // Update knowledge graph
        updateKnowledgeGraph(event, impact);

        // Notify connected clients
        webSocketService.broadcastUpdate(impact);
    }

    private void updateKnowledgeGraph(SpreadsheetChangeEvent event, ChangeImpact impact) {
        // You could update only the impacted nodes for efficiency
        graphService.buildKnowledgeGraphFromChange(event, impact);
    }

    private ChangeImpact analyzeChangeImpact(SpreadsheetChangeEvent event) {
        ChangeImpact impact = new ChangeImpact();
        impact.setChangedCell(event.getCellAddress());
        impact.setOldValue(event.getOldValue());
        impact.setNewValue(event.getNewValue());

        // Find all dependent cells
        List<String> affectedCells = findAffectedCells(event.getCellAddress());
        impact.setAffectedCells(affectedCells);

        return impact;
    }

    private List<String> findAffectedCells(String cellAddress) {
        try (Session session = neo4jDriver.session()) {
            String cypher =  "MATCH (source:Cell {id: $cellId}) " +
                    "MATCH (source)<-[:DEPENDS_ON*1..10]-(affected) " +
                    "RETURN affected.id as affectedId";

            Result result = session.run(cypher, parameters("cellId", cellAddress));

            return result.list(record -> record.get("affectedId").asString());
        }
    }
}