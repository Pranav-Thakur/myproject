package org.superjoin.dto;

import lombok.Data;

@Data
public class CellInfo {
    private String address;
    private String value;
    private String formula;

    public CellInfo() {}

    public CellInfo(String cellAddress, String newValue, String formula) {
        this.address = cellAddress;
        this.value = newValue;
        this.formula = formula;
    }
}