package org.superjoin.querymodel;

import lombok.Data;
import org.superjoin.entity.RelationshipEntity;
import org.superjoin.entity.SpreadsheetEntity;

import java.util.List;
import java.util.Map;

@Data
public class QueryResult {
    private List<SpreadsheetEntity> entities;
    private List<RelationshipEntity> relationships;
    private String explanation;
    private Map<String, Object> metadata;
}