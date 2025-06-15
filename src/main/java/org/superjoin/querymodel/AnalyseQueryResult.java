package org.superjoin.querymodel;

import lombok.Data;

import java.util.List;

@Data
public class AnalyseQueryResult {
    private List<QueryResult> paths;
    private String explanation;
}
