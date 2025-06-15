package org.superjoin.service;

import org.neo4j.driver.*;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.superjoin.dto.ParsedQuery;
import org.superjoin.entity.RelationshipEntity;
import org.superjoin.entity.SpreadsheetEntity;
import org.superjoin.querymodel.AnalyseQueryResult;
import org.superjoin.querymodel.ImpactAnalysisResult;
import org.superjoin.querymodel.QueryResult;
import org.superjoin.querymodel.SemanticQuery;

import java.util.ArrayList;
import java.util.List;

@Service
public class SemanticQueryProcessor {

    @Autowired
    private KnowledgeGraphService graphService;

    @Autowired
    private NLPService nlpService;

    @Autowired
    private Driver neo4jDriver;

    public AnalyseQueryResult visualiseGraph() {
        try (Session session = neo4jDriver.session()) {
            String cypher = "MATCH (n)-[r]->(m) " +
                    "RETURN n, r, m " +
                    "LIMIT 100";

            Result result = session.run(cypher);
            return buildAnalyseQueryResult(result);
        }
    }

    public ImpactAnalysisResult processImpactQuery(SemanticQuery query) {
        // Parse natural language query
        ParsedQuery parsedQuery = nlpService.parseQuery(query.getQuery());
        parsedQuery.setParameters(query.getParameters());

        // Convert to Cypher query
        String cypherQuery = buildCypherQuery(parsedQuery);
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypherQuery, parsedQuery.getParameters());
            if (!result.hasNext()) {
                ImpactAnalysisResult analysisResult = new ImpactAnalysisResult();
                analysisResult.setExplanation("No impact on other cell.");
                return analysisResult;
            }

            return buildImpactAnalysisResult(result.single());
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
            //conditions.add("c.semanticLabel CONTAINS $semanticLabel");
            parsedQuery.getFilters().values().forEach(filter -> {
                if (filter.getField().equals("value")) {
                    conditions.add("c.value IS NOT NULL AND NOT c.value =~ '.*[a-zA-Z]+'");
                    conditions.add("toFloat(c." + filter.getField() + ") " + filter.getOperator() + " " + filter.getValue());
                } else
                    conditions.add("c." + filter.getField() + " " + filter.getOperator() + " " + filter.getValue());
            });
        }

        if (parsedQuery.hasValueFilter()) {
            conditions.add("toFloat(c.value) = $value");
        }

        if (!conditions.isEmpty()) {
            cypher.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        cypher.append(" RETURN c");

        return cypher.toString();
    }

    private String buildImpactAnalysisQuery(ParsedQuery parsedQuery) {
        return "MATCH (c:Cell {id: $cellId})\n" +
                "WITH c\n" +
                "MATCH path = (c)<-[:DEPENDS_ON*1..5]-(affected)\n" +
                "RETURN c, collect(path) as paths";
    }

    private QueryResult executeQuery(String cypherQuery, ParsedQuery parsedQuery) {
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypherQuery, parsedQuery.getParameters());

            return buildQueryResult(result);
        }
    }
    public String buildDependencyAnalysisQuery(ParsedQuery parsedQuery) {
        // Assume the main concept or cell is in filters with key "cellId" or "concept"
        String cellId = (String) parsedQuery.getParameters().get("cellId");
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
        return "MATCH (c:Cell {id: $cellId}) " +
                "MATCH (c)<-[:DEPENDS_ON*1..5]-(dependent) " +
                "RETURN c.id AS source, collect(dependent.id) AS dependents";
    }

    private ImpactAnalysisResult buildImpactAnalysisResult(Record record) {
        ImpactAnalysisResult analysisResult = new ImpactAnalysisResult();
        analysisResult.setPaths(new ArrayList<>());
        analysisResult.setSource(mapRecordToEntity(record.get("c").asNode()));

        List<Path> pathList = record.get("paths").asList(Value::asPath);
        for (Path path : pathList) {
            QueryResult result = new QueryResult();
            result.setRelationships(new ArrayList<>());
            result.setEntities(new ArrayList<>());

            for (Node n : path.nodes()) {
                result.getEntities().add(mapRecordToEntity(n));
            }

            for (Relationship r : path.relationships()) {
                result.getRelationships().add(mapRecordToEntity(r));
            }

            analysisResult.getPaths().add(result);
        }

        return analysisResult;
    }

    public QueryResult buildQueryResult(Result result) {
        QueryResult queryResult = new QueryResult();
        List<SpreadsheetEntity> entities = new ArrayList<>();

        while (result.hasNext()) {
            SpreadsheetEntity entity = mapRecordToEntity(result.next().get("c").asNode());
            if (entity != null)
                entities.add(entity);
        }

        queryResult.setEntities(entities);
        queryResult.setExplanation("Query executed based on semantic filters: ");

        return queryResult;
    }

    private SpreadsheetEntity mapRecordToEntity(Node record) {
        if (record == null) return null;
        SpreadsheetEntity entity = new SpreadsheetEntity();
        entity.setId(record.get("id").asString());
        entity.setEntityId(record.id());
        entity.setValue(record.get("value").asString(null));
        entity.setDataType(record.get("dataType").asString(null));
        entity.setFormula(record.get("formula").asString(null));
        entity.setFormulaType(record.get("formulaType").asString(null));
        entity.setSemanticLabel(record.get("semanticLabel").asString(null));
        entity.setSheet(record.get("sheet").asString(null));
        entity.setEntityType(record.labels().iterator().next());
        return entity;
    }

    private RelationshipEntity mapRecordToEntity(Relationship record) {
        RelationshipEntity entity = new RelationshipEntity();
        entity.setId(String.valueOf(record.id()));
        entity.setSourceEntityId(String.valueOf(record.startNodeId()));
        entity.setTargetEntityId(String.valueOf(record.endNodeId()));
        entity.setRelationshipType(record.type());
        entity.setStrength((Double) record.asMap().get("weight"));
        return entity;
    }

    private AnalyseQueryResult buildAnalyseQueryResult(Result result) {
        AnalyseQueryResult analyseQueryResult = new AnalyseQueryResult();
        analyseQueryResult.setPaths(new ArrayList<>());

        while (result.hasNext()) {
            Record record = result.next();
            Node n = record.get("n").asNode();

            QueryResult queryResult = new QueryResult();
            queryResult.setEntities(new ArrayList<>());
            queryResult.setRelationships(new ArrayList<>());

            queryResult.getEntities().add(mapRecordToEntity(n));
            Node m = record.get("m").asNode();
            queryResult.getEntities().add(mapRecordToEntity(m));

            Relationship r = record.get("r").asRelationship();
            queryResult.getRelationships().add(mapRecordToEntity(r));

            analyseQueryResult.getPaths().add(queryResult);
        }

        return analyseQueryResult;
    }
}