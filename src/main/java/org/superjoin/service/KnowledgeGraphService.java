package org.superjoin.service;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import static org.neo4j.driver.Values.parameters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.superjoin.component.FormulaAnalyzer;
import org.superjoin.component.SemanticAnalyzer;
import org.superjoin.constants.DataType;
import org.superjoin.constants.FormulaType;
import org.superjoin.constants.SemanticLabel;
import org.superjoin.dto.CellInfo;
import org.superjoin.dto.ChangeImpact;
import org.superjoin.dto.SheetData;
import org.superjoin.dto.SpreadsheetData;
import org.superjoin.events.SpreadsheetChangeEvent;

import java.text.SimpleDateFormat;
import java.util.List;

@Service
public class KnowledgeGraphService {

    @Autowired
    private Driver neo4jDriver;

    @Autowired
    private FormulaAnalyzer formulaAnalyzer;

    @Autowired
    private SemanticAnalyzer semanticAnalyzer;

    public void buildKnowledgeGraph(SpreadsheetData spreadsheetData) {
        try (Session session = neo4jDriver.session()) {
            // Clear existing graph
            session.run("MATCH (n) DETACH DELETE n");

            // Create nodes for each cell/entity
            createEntityNodes(session, spreadsheetData);

            // Analyze and create relationships
            createRelationships(session, spreadsheetData);

            // Add semantic labels
            addSemanticLabels(session, spreadsheetData);
        }
    }

    private void createEntityNodes(Session session, SpreadsheetData data) {
        for (SheetData sheet : data.getSheets()) {
            // Create sheet node
            session.run("CREATE (s:Sheet {name: $name, id: $id})",
                    parameters("name", sheet.getName(), "id", sheet.getId()));

            for (CellInfo cell : sheet.getCells()) {
                String cypher =
                        "CREATE (c:Cell {\n" +
                                "    id: $id,\n" +
                                "    value: $value,\n" +
                                "    formula: $formula,\n" +
                                "    formulaType: $formulaType,\n" +
                                "    dataType: $dataType,\n" +
                                "    sheet: $sheet\n" +
                                "})";

                FormulaType formulaType = formulaAnalyzer.analyzeFormulaType(cell.getFormula());
                DataType dataType = determineDataType(cell.getValue());
                session.run(cypher, parameters(
                        "id", cell.getAddress(),
                        "value", cell.getValue(),
                        "formula", cell.getFormula(),
                        "formulaType", formulaType == FormulaType.NONE ? null : formulaType.name(),
                        "dataType", dataType == null ? null : dataType.name(),
                        "sheet", sheet.getName()
                ));
            }
        }
    }

    private DataType determineDataType(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        // Try number
        try {
            Double.parseDouble(value);
            return DataType.NUMBER;
        } catch (NumberFormatException ignored) {}

        // Try boolean
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return DataType.BOOLEAN;
        }

        // Try date (basic check)
        try {
            // You can use more robust date formats as needed
            new SimpleDateFormat("yyyy-MM-dd").parse(value);
            return DataType.DATE;
        } catch (Exception ignored) {}

        return DataType.TEXT;
    }

    private void createRelationships(Session session, SpreadsheetData data) {
        for (SheetData sheet : data.getSheets()) {
            for (CellInfo cell : sheet.getCells()) {
                if (cell.getFormula() != null && !cell.getFormula().isEmpty()) {
                    analyzeFormulaDependencies(session, cell, sheet);
                }
            }
        }
    }

    private void analyzeFormulaDependencies(Session session, CellInfo cell, SheetData sheet) {
        List<String> dependencies = formulaAnalyzer.extractDependencies(cell.getFormula());

        for (String dependency : dependencies) {
            String cypher =
                    "MATCH (source:Cell {id: $sourceId})\n" +
                            "MATCH (target:Cell {id: $targetId})\n" +
                            "CREATE (target)-[:DEPENDS_ON {formula: $formula}]->(source)";

            session.run(cypher, parameters(
                    "sourceId", dependency,
                    "targetId", cell.getAddress(),
                    "formula", cell.getFormula()
            ));
        }
    }

    private void addSemanticLabels(Session session, SpreadsheetData data) {
        for (SheetData sheet : data.getSheets()) {
            for (CellInfo cell : sheet.getCells()) {
                SemanticLabel semanticLabel = semanticAnalyzer.inferSemanticLabel(cell, sheet);
                if (semanticLabel != null) {
                    session.run("MATCH (c:Cell {id: $id}) SET c.semanticLabel = $label",
                            parameters("id", cell.getAddress(), "label", semanticLabel.name()));
                }
            }
        }
    }

    public void buildKnowledgeGraphFromChange(SpreadsheetChangeEvent event, ChangeImpact impact) {
        try (Session session = neo4jDriver.session()) {
            // Update the changed cell
            session.run("MATCH (c:Cell {id: $id}) " +
                            "SET c.value = $newValue",
                    parameters("id", event.getCellAddress(), "newValue", event.getNewValue()));

            // Optionally, remove old dependencies and recalculate if needed
            session.run("MATCH (c:Cell {id: $id})<-[r:DEPENDS_ON]-() " +
                            "DELETE r",
                    parameters("id", event.getCellAddress()));

            // Recalculate dependencies if formula exists
            if (event.getFormula() != null && !event.getFormula().isEmpty()) {
                List<String> dependencies = formulaAnalyzer.extractDependencies(event.getFormula());
                for (String dep : dependencies) {
                    session.run("MATCH (source:Cell {id: $sourceId}) " +
                                    "MATCH (target:Cell {id: $targetId}) " +
                                    "CREATE (target)-[:DEPENDS_ON {formula: $formula}]->(source)",
                            parameters(
                                    "sourceId", dep,
                                    "targetId", event.getCellAddress(),
                                    "formula", event.getFormula()
                            ));
                }
            }

            // Optional: re-assign semantic label if applicable
            CellInfo changedCell = new CellInfo(event.getCellAddress(), event.getNewValue(), event.getFormula());
            SemanticLabel semanticLabel = semanticAnalyzer.inferSemanticLabel(changedCell, new SheetData(event.getSheetName()));
            if (semanticLabel != null) {
                session.run("MATCH (c:Cell {id: $id}) SET c.semanticLabel = $label",
                        parameters("id", event.getCellAddress(), "label", semanticLabel));
            }
        }
    }
}
