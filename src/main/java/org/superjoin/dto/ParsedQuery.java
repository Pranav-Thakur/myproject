package org.superjoin.dto;

import lombok.Data;
import org.superjoin.constants.QueryIntent;

import java.util.List;
import java.util.Map;

@Data
public class ParsedQuery {
    private QueryIntent intent;
    private Map<String, Filter> filters;
    private List<String> concepts;
    private Map<String, Object> parameters;

    public boolean hasSemanticFilter() {
        return (filters != null && !filters.isEmpty());
    }

    public boolean hasValueFilter() {
        return (parameters != null && !parameters.isEmpty() && parameters.containsKey("value"));
    }
}
