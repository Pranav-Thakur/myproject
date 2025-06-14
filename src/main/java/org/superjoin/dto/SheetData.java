package org.superjoin.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SheetData {
    private String id;
    private String name;
    private List<CellInfo> cells;

    public SheetData() {
        this.name = "";
        this.cells = new ArrayList<>();
    }

    public SheetData(String sheetName) {
        this.name = sheetName;
        cells = new ArrayList<>();
    }
}