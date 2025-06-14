package org.superjoin;

import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.superjoin.dto.CellInfo;
import org.superjoin.dto.SheetData;
import org.superjoin.dto.SpreadsheetData;
import org.superjoin.querymodel.QueryResult;
import org.superjoin.querymodel.SemanticQuery;
import org.superjoin.service.KnowledgeGraphService;
import org.superjoin.service.SemanticQueryProcessor;

import java.util.ArrayList;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
public class SpreadsheetBrainIntegrationTest {

    @Autowired
    private KnowledgeGraphService graphService;

    @Autowired
    private SemanticQueryProcessor queryProcessor;

    @Test
    public void testKnowledgeGraphBuilding() {
        // Create test spreadsheet data
        SpreadsheetData testData = createTestSpreadsheetData();

        // Build knowledge graph
        graphService.buildKnowledgeGraph(testData);

        // Verify graph was built correctly
        SemanticQuery query = new SemanticQuery();
        query.setQuery("Show all cells");

        QueryResult result = queryProcessor.processQuery(query);
        assertThat(result.getEntities(), is(not(empty())));
    }
/*

    @Test
    void testSemanticQuerying() {
        SemanticQuery query = new SemanticQuery();
        query.setQuery("Find all revenue cells");

        QueryResult result = queryProcessor.processQuery(query);

        assertThat(result.getEntities()).isNotNull();
        assertThat(result.getExplanation()).isNotBlank();
    }

    @Test
    void testImpactAnalysis() {
        SemanticQuery query = new SemanticQuery();
        query.setQuery("If A1 changes, what is affected?");

        QueryResult result = queryProcessor.processQuery(query);

        assertThat(result.getEntities()).isNotNull();
    }
*/

    private SpreadsheetData createTestSpreadsheetData() {
        SpreadsheetData data = new SpreadsheetData();
        data.setSheets(new ArrayList<>());

        SheetData sheet = new SheetData();
        sheet.setName("Test Sheet");
        sheet.setCells(new ArrayList<>());

        // Add test cells with formulas
        CellInfo a1 = new CellInfo("A1", "100", null);
        CellInfo a2 = new CellInfo("A2", "200", null);
        CellInfo a3 = new CellInfo("A3", null, "=A1+A2");

        sheet.getCells().add(a1);
        sheet.getCells().add(a2);
        sheet.getCells().add(a3);

        data.getSheets().add(sheet);
        return data;
    }
}
