package org.superjoin.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SpreadsheetChangeEvent extends ApplicationEvent {

    private final String cellAddress;
    private final String oldValue;
    private final String newValue;
    private final String formula;
    private final String sheetName;

    public SpreadsheetChangeEvent(Object source, String cellAddress, String oldValue, String newValue, String formula, String sheetName) {
        super(source);
        this.cellAddress = cellAddress;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.formula = formula;
        this.sheetName = sheetName;
    }
}
