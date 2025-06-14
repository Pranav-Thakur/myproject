package org.superjoin.dto;

import lombok.Data;

@Data
public class AnalysisResult {
    private String status;
    private String message;
    private int entityCount;
}
