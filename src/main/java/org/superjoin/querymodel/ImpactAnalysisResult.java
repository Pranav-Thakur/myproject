package org.superjoin.querymodel;

import lombok.Data;
import org.superjoin.entity.SpreadsheetEntity;

import java.util.List;

@Data
public class ImpactAnalysisResult {
    private SpreadsheetEntity source;
    private List<QueryResult> paths;
    private String explanation;
}
