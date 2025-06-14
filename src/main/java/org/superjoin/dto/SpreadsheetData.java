package org.superjoin.dto;

import lombok.Data;

import java.util.List;

@Data
public class SpreadsheetData {
    private List<SheetData> sheets;
}