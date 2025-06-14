package org.superjoin.querymodel;

import lombok.Data;
import org.superjoin.entity.Relationship;
import org.superjoin.entity.SpreadsheetEntity;

import java.util.List;
import java.util.Map;

@Data
public class QueryResult {
    private List<SpreadsheetEntity> entities;
    private List<Relationship> relationships;
    private String explanation;
    private Map<String, Object> metadata;
}