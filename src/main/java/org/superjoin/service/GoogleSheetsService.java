package org.superjoin.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.stereotype.Service;
import org.superjoin.dto.CellInfo;
import org.superjoin.dto.SheetData;
import org.superjoin.dto.SpreadsheetData;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoogleSheetsService {

    private final Sheets sheetsService;

    private static final String APPLICATION_NAME = "SuperJoinAI";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    public GoogleSheetsService() throws GeneralSecurityException, IOException {
        // Initialize Google Sheets API client
        this.sheetsService = initializeSheetsService();
    }

    public Sheets initializeSheetsService() throws IOException, GeneralSecurityException {
        // Load credentials from service account key file (adjust path as needed)
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileInputStream("src/main/resources/superjionai-8ec534a70d21.json"))
                .createScoped(List.of(SheetsScopes.SPREADSHEETS_READONLY));

        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                new HttpCredentialsAdapter(credentials)
        )
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public SpreadsheetData readSpreadsheet(String spreadsheetId) {
        try {
            Spreadsheet spreadsheet = sheetsService.spreadsheets()
                    .get(spreadsheetId)
                    .setIncludeGridData(true)
                    .execute();

            return parseSpreadsheetData(spreadsheet);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read spreadsheet", e);
        }
    }

    private SpreadsheetData parseSpreadsheetData(Spreadsheet spreadsheet) {
        SpreadsheetData data = new SpreadsheetData();
        data.setSheets(new ArrayList<>());

        for (Sheet sheet : spreadsheet.getSheets()) {
            SheetData sheetData = new SheetData(sheet.getProperties().getTitle());
            sheetData.setId(String.valueOf(sheet.getProperties().getSheetId()));

            if (sheet.getData() != null) {
                for (GridData gridData : sheet.getData()) {
                    parseGridData(gridData, sheetData);
                }
            }

            data.getSheets().add(sheetData);
        }

        return data;
    }

    private void parseGridData(GridData gridData, SheetData sheetData) {
        List<RowData> rows = gridData.getRowData();
        if (rows == null) return;

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            RowData row = rows.get(rowIndex);
            if (row.getValues() == null) continue;

            for (int colIndex = 0; colIndex < row.getValues().size(); colIndex++) {
                CellData cell = row.getValues().get(colIndex);
                processCellData(cell, rowIndex, colIndex, sheetData);
            }
        }
    }

    private void processCellData(CellData cell, int row, int col, SheetData sheetData) {
        CellInfo cellInfo = new CellInfo();
        cellInfo.setAddress(getCellAddress(row, col));

        if (cell.getUserEnteredValue() != null) {
            if (cell.getUserEnteredValue().getFormulaValue() != null) {
                cellInfo.setFormula(cell.getUserEnteredValue().getFormulaValue());
                //cellInfo.setValue(extractCellValueFromFormula(cell, sheetData));
            } else
                cellInfo.setValue(extractCellValue(cell));
        }

        sheetData.getCells().add(cellInfo);
    }

    private String extractCellValueFromFormula(CellData cell, SheetData sheetData) {
        String formula = cell.getUserEnteredValue().getFormulaValue();
        String range = formula.substring(formula.indexOf('(') + 1, formula.indexOf(')')); // A1:C1
        String sheetRange = sheetData.getName() + "!" + range;
        ValueRange response = null;
        try {
            response = sheetsService.spreadsheets().values()
                    .get("", sheetRange)
                    .setValueRenderOption("UNFORMATTED_VALUE") // or "FORMATTED_VALUE"
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<List<Object>> values = response.getValues();
        return values.get(0).get(0).toString();
    }

    public String getCellAddress(int row, int col) {
        StringBuilder columnLabel = new StringBuilder();
        col++;

        while (col > 0) {
            col--; // Adjust because Excel columns are 1-based, but 'A' starts at 0
            columnLabel.insert(0, (char) ('A' + (col % 26)));
            col /= 26;
        }

        return columnLabel.toString() + (row + 1); // Excel rows start from 1
    }

    public String extractCellValue(CellData cell) {
        if (cell == null || cell.getUserEnteredValue() == null) {
            return null;
        }

        ExtendedValue value = cell.getUserEnteredValue();

        if (value.getStringValue() != null) {
            return value.getStringValue();
        } else if (value.getNumberValue() != null) {
            return value.getNumberValue().toString();
        } else if (value.getBoolValue() != null) {
            return value.getBoolValue().toString();
        } else if (value.getFormulaValue() != null) {
            return value.getFormulaValue();  // You might want to return evaluated value instead
        } else {
            return null; // Could add support for errors or other types if needed
        }
    }
}
