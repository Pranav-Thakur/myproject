package org.superjoin.querymodel;

import lombok.Data;

import java.util.Map;

@Data
public class SemanticQuery {
    private String query;
    private String intent; // FIND, ANALYZE, IMPACT, SUGGEST
    private Map<String, Object> parameters;
    private String userId;
}
