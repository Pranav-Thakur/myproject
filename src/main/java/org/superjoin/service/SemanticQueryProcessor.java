package org.superjoin.service;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.superjoin.dto.ParsedQuery;
import org.superjoin.entity.SpreadsheetEntity;
import org.superjoin.querymodel.QueryResult;
import org.superjoin.querymodel.SemanticQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SemanticQueryProcessor {

    @Autowired
    private KnowledgeGraphService graphService;

    @Autowired
    private NLPService nlpService;

    @Autowired
    private Driver neo4jDriver;

    public QueryResult visualiseGraph() {
        try (Session session = neo4jDriver.session()) {
            String cypher = "MATCH (n)-[r]->(m) " +
                    "RETURN n, r, m " +
                    "LIMIT 100";

            Result result = session.run(cypher);
            return buildQueryResult(result);
        }
    }

    public QueryResult processQuery(SemanticQuery query) {
        // Parse natural language query
        ParsedQuery parsedQuery = nlpService.parseQuery(query.getQuery());
        parsedQuery.setParameters(query.getParameters());

        // Convert to Cypher query
        String cypherQuery = buildCypherQuery(parsedQuery);

        // Execute query
        return executeQuery(cypherQuery, parsedQuery);
    }

    private String buildCypherQuery(ParsedQuery parsedQuery) {
        switch (parsedQuery.getIntent()) {
            case FIND_ENTITIES:
                return buildFindEntitiesQuery(parsedQuery);
            case ANALYZE_DEPENDENCIES:
                return buildDependencyAnalysisQuery(parsedQuery);
            case IMPACT_ANALYSIS:
                return buildImpactAnalysisQuery(parsedQuery);
            default:
                throw new UnsupportedOperationException("Query intent not supported: " +
                        parsedQuery.getIntent());
        }
    }

    private String buildFindEntitiesQuery(ParsedQuery parsedQuery) {
        StringBuilder cypher = new StringBuilder("MATCH (c:Cell)");

        List<String> conditions = new ArrayList<>();

        if (parsedQuery.hasSemanticFilter()) {
            conditions.add("c.semanticLabel CONTAINS $semanticLabel");
        }

        if (parsedQuery.hasValueFilter()) {
            conditions.add("c.value = $value");
        }

        if (!conditions.isEmpty()) {
            cypher.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        cypher.append(" RETURN c");

        return cypher.toString();
    }

    private String buildImpactAnalysisQuery(ParsedQuery parsedQuery) {
        return "MATCH (source:Cell {id: $cellId})\n" +
                "MATCH (source)<-[:DEPENDS_ON*1..5]-(affected)\n" +
                "RETURN source, affected,\n" +
                "       [path in (source)<-[:DEPENDS_ON*1..5]-(affected) | path] as paths";
    }

    private QueryResult executeQuery(String cypherQuery, ParsedQuery parsedQuery) {
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypherQuery, parsedQuery.getParameters());

            return buildQueryResult(result);
        }
    }
    public String buildDependencyAnalysisQuery(ParsedQuery parsedQuery) {
        // Assume the main concept or cell is in filters with key "cellId" or "concept"
        String cellId = (String) parsedQuery.getFilters().get("cellId");
        if (cellId == null) {
            // Fallback: try semantic concept if specific cellId not found
            List<String> concepts = parsedQuery.getConcepts();
            if (concepts != null && !concepts.isEmpty()) {
                return "MATCH (c:Cell) " +
                        "WHERE toLower(c.semanticLabel) IN $concepts " +
                        "WITH c " +
                        "MATCH (c)<-[:DEPENDS_ON*1..5]-(dependent:Cell) " +
                        "RETURN c.id AS source, collect(DISTINCT dependent.id) AS dependents";
            }
            // Else generic dependency trace
            return "MATCH (c:Cell)<-[:DEPENDS_ON*1..5]-(dependent) " +
                    "RETURN c.id AS source, collect(dependent.id) AS dependents " +
                    "LIMIT 10";
        }

        // If we have a specific cell
        return "MATCH (source:Cell {id: $cellId}) " +
                "MATCH (source)<-[:DEPENDS_ON*1..5]-(affected) " +
                "RETURN source.id AS source, collect(affected.id) AS affectedCells";
    }

    public QueryResult buildQueryResult(Result result) {
        QueryResult queryResult = new QueryResult();
        List<SpreadsheetEntity> entities = new ArrayList<>();

        while (result.hasNext()) {
            SpreadsheetEntity entity = mapRecordToEntity(result.next());
            if (entity != null)
                entities.add(entity);
        }

        queryResult.setEntities(entities);
        queryResult.setExplanation("Query executed based on semantic filters: ");

        return queryResult;
    }

    private SpreadsheetEntity mapRecordToEntity(Record node) {
        if (node.get("c") == null) return null;
        Node record = node.get("c").asNode();
        if (record == null) return null;

        SpreadsheetEntity entity = new SpreadsheetEntity();
        entity.setId(record.get("id").asString());
        //entity.setEntityId(record.get("<id>").asLong());

        entity.setValue(record.get("value").asString(null));
        entity.setDataType(record.get("dataType").asString(null));
        entity.setFormula(record.get("formula").asString(null));
        entity.setFormulaType(record.get("formulaType").asString());
        entity.setSemanticLabel(record.get("semanticLabel").asString(null));
        return entity;
    }
}