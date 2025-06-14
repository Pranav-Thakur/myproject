package org.superjoin.entity;

import javax.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "spreadsheet_entities")
public class SpreadsheetEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    private Long entityId; // Unique identifier
    private String entityType; // CELL, RANGE, FORMULA, SHEET, WORKBOOK
    @Column(name = "`value`")
    private String value;
    private String formula;
    private String formulaType;
    private String semanticLabel; // e.g., "Revenue", "Marketing Budget"
    private String dataType; // NUMBER, TEXT, DATE, BOOLEAN
    private String sheet;

    @Temporal(TemporalType.TIMESTAMP)
    private Date lastModified;
}