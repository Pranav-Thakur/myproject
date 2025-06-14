package org.superjoin.component;

import org.springframework.stereotype.Component;
import org.superjoin.constants.FormulaType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FormulaAnalyzer {

    private static final Pattern CELL_REFERENCE_PATTERN =
            Pattern.compile("([A-Z]+[0-9]+|[A-Z]+:[A-Z]+[0-9]+:[0-9]+)");

    public List<String> extractDependencies(String formula) {
        List<String> dependencies = new ArrayList<>();

        if (formula == null || formula.isEmpty()) {
            return dependencies;
        }

        // Remove leading = if present
        String cleanFormula = formula.startsWith("=") ? formula.substring(1) : formula;

        Matcher matcher = CELL_REFERENCE_PATTERN.matcher(cleanFormula);
        while (matcher.find()) {
            String reference = matcher.group();
            dependencies.add(normalizeReference(reference));
        }

        return dependencies;
    }

    public FormulaType analyzeFormulaType(String formula) {
        if (formula == null || formula.isEmpty()) {
            return FormulaType.NONE;
        }

        String upperFormula = formula.toUpperCase();

        if (upperFormula.contains("SUM")) return FormulaType.AGGREGATION;
        if (upperFormula.contains("VLOOKUP") || upperFormula.contains("HLOOKUP") || upperFormula.contains("INDEX")) return FormulaType.LOOKUP;
        if (upperFormula.contains("IF")) return FormulaType.CONDITIONAL;
        if (upperFormula.contains("+") || upperFormula.contains("-") ||
                upperFormula.contains("*") || upperFormula.contains("/")) return FormulaType.ARITHMETIC;

        return FormulaType.OTHER;
    }

    public String normalizeReference(String reference) {
        if (reference == null || reference.trim().isEmpty()) {
            return "";
        }

        // Remove extra whitespace and convert to uppercase
        reference = reference.trim().toUpperCase();

        // If sheet is present (e.g., Sheet1!A1), split and normalize
        if (reference.contains("!")) {
            String[] parts = reference.split("!", 2);
            String sheet = parts[0].replaceAll("[^A-Z0-9]", ""); // Clean sheet name
            String cell = parts[1].replaceAll("\\$", "");        // Remove $ from cell ref
            return sheet + "!" + cell;
        } else {
            // If no sheet provided, just clean cell ref
            return reference.replaceAll("\\$", "");
        }
    }
}