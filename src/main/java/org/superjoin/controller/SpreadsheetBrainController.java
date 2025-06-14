package org.superjoin.controller;

import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.superjoin.dto.AnalysisResult;
import org.superjoin.dto.SpreadsheetData;
import org.superjoin.querymodel.QueryResult;
import org.superjoin.querymodel.SemanticQuery;
import org.superjoin.service.GoogleSheetsService;
import org.superjoin.service.KnowledgeGraphService;
import org.superjoin.service.SemanticQueryProcessor;

import java.util.Collections;

@RestController
@RequestMapping("/api/v1")
public class SpreadsheetBrainController {

    @Autowired
    private KnowledgeGraphService graphService;

    @Autowired
    private SemanticQueryProcessor queryProcessor;

    @Autowired
    private GoogleSheetsService sheetsService;
    @Autowired
    private SemanticQueryProcessor semanticQueryProcessor;

    @PostMapping("/spreadsheets/{id}/analyze")
    public ResponseEntity<AnalysisResult> analyzeSpreadsheet(@PathVariable String id) {
        try {
            SpreadsheetData data = sheetsService.readSpreadsheet(id);
            graphService.buildKnowledgeGraph(data);

            AnalysisResult result = new AnalysisResult();
            result.setStatus("SUCCESS");
            result.setMessage("Knowledge graph built successfully");
            result.setEntityCount(data.getSheets().get(0).getCells().size());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            AnalysisResult analysisResult = new AnalysisResult();
            analysisResult.setStatus("FAILURE");
            analysisResult.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(analysisResult);
        }
    }

    @PostMapping("/query")
    public ResponseEntity<QueryResult> executeQuery(@RequestBody SemanticQuery query) {
        try {
            QueryResult result = queryProcessor.processQuery(query);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            QueryResult queryResult = new QueryResult();
            queryResult.setEntities(Collections.emptyList());
            queryResult.setExplanation("Query execution failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(queryResult);
        }
    }
/*
    @GetMapping("/cells/{cellId}/impact")
    public ResponseEntity<ImpactAnalysisResult> analyzeImpact(@PathVariable String cellId) {
        try {
            SemanticQuery impactQuery = new SemanticQuery();
            impactQuery.setQuery("What cells are affected if " + cellId + " changes?");

            QueryResult result = queryProcessor.processQuery(impactQuery);

            ImpactAnalysisResult impactResult = new ImpactAnalysisResult();
            impactResult.setSourceCell(cellId);
            impactResult.setAffectedCells(result.getEntities());

            return ResponseEntity.ok(impactResult);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }*/

    @GetMapping("/graph/visualize")
    public ResponseEntity<QueryResult> getGraphVisualization() {
        try {
            return ResponseEntity.ok(semanticQueryProcessor.visualiseGraph());
        } catch (Exception e) {
            QueryResult result = new QueryResult();
            result.setExplanation(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
}