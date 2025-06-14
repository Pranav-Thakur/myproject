package org.superjoin.dto;

import java.util.List;

import lombok.Data;

@Data
public class ChangeImpact {
    private String changedCell;
    private String oldValue;
    private String newValue;
    private List<String> affectedCells;
}
