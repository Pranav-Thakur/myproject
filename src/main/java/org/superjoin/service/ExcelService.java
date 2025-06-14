package org.superjoin.service;

import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.ExtendedValue;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.superjoin.dto.CellInfo;
import org.superjoin.dto.SheetData;
import org.superjoin.dto.SpreadsheetData;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

@Service
public class ExcelService {

    public SpreadsheetData readExcelFile(String filePath) {
        try (Workbook workbook = WorkbookFactory.create(new File(filePath))) {
            return parseWorkbook(workbook);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read Excel file", e);
        }
    }

    private SpreadsheetData parseWorkbook(Workbook workbook) {
        SpreadsheetData data = new SpreadsheetData();
        data.setSheets(new ArrayList<>());

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            SheetData sheetData = parseSheet(sheet);
            data.getSheets().add(sheetData);
        }

        return data;
    }

    private SheetData parseSheet(Sheet sheet) {
        SheetData sheetData = new SheetData(sheet.getSheetName());
        sheetData.setCells(new ArrayList<>());

        for (Row row : sheet) {
            for (Cell cell : row) {
                CellInfo cellInfo = parseCellInfo(cell);
                sheetData.getCells().add(cellInfo);
            }
        }

        return sheetData;
    }

    private CellInfo parseCellInfo(Cell cell) {
        CellInfo cellInfo = new CellInfo();
        cellInfo.setAddress(cell.getAddress().formatAsString());

        switch (cell.getCellType()) {
            case FORMULA:
                cellInfo.setFormula(cell.getCellFormula());
                cellInfo.setValue(getCellValue(cell));
                break;
            case NUMERIC:
                cellInfo.setValue(String.valueOf(cell.getNumericCellValue()));
                break;
            case STRING:
                cellInfo.setValue(cell.getStringCellValue());
                break;
            default:
                cellInfo.setValue(cell.toString());
        }

        return cellInfo;
    }

    public String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        CellType cellType = cell.getCellType();

        switch (cellType) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                // Evaluate the formula or return as string, depending on need
                return cell.getCellFormula(); // Or evaluate it if you have a FormulaEvaluator
            case BLANK:
                return "";
            case ERROR:
                return "ERROR";
            default:
                return "";
        }
    }
}
