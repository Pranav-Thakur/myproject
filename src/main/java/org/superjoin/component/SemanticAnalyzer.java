package org.superjoin.component;

import org.superjoin.constants.SemanticLabel;
import org.superjoin.dto.CellInfo;
import org.superjoin.dto.SheetData;
import org.springframework.stereotype.Component;

@Component
public class SemanticAnalyzer {

    public SemanticLabel inferSemanticLabel(CellInfo cell, SheetData sheet) {
        String value = cell.getValue();
        String address = cell.getAddress();

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        value = value.trim();

        // 1. If it's in the first row, it's probably a header
        if (address.matches("^[A-Z]+1$")) {
            return SemanticLabel.HEADER;
        }

        // 4. If it looks like an ID or code
        if (value.matches("^[A-Za-z]{2,5}-\\d{2,6}$") || value.toLowerCase().contains("id")) {
            return SemanticLabel.ID;
        }

        // 5. Fallback to general text label
        if (value.length() <= 20 && value.matches("^[A-Za-z\\s]+$")) {
            return SemanticLabel.LABEL;
        }

        return null;
    }
}