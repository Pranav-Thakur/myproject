package org.superjoin.service;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import org.springframework.stereotype.Service;
import org.superjoin.constants.QueryIntent;
import org.superjoin.dto.ParsedQuery;
import org.superjoin.dto.Filter;

import javax.annotation.PostConstruct;
import java.util.*;

@Service
public class NLPService {

    private StanfordCoreNLP pipeline;

    @PostConstruct
    public void NLPServiceConstruct() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
        props.setProperty("pos.model", "models/english-left3words-distsim.tagger"); // path inside resources

        this.pipeline = new StanfordCoreNLP(props);
    }

    public ParsedQuery parseQuery(String naturalLanguageQuery) {
        CoreDocument document = new CoreDocument(naturalLanguageQuery);
        pipeline.annotate(document);

        ParsedQuery parsedQuery = new ParsedQuery();

        // Extract intent
        QueryIntent intent = extractIntent(document);
        parsedQuery.setIntent(intent);

        // Extract entities and filters
        Map<String, Object> filters = extractFilters(document);
        parsedQuery.setFilters(filters);

        // Extract semantic concepts
        List<String> concepts = extractSemanticConcepts(document);
        parsedQuery.setConcepts(concepts);

        return parsedQuery;
    }

    private QueryIntent extractIntent(CoreDocument document) {
        String text = document.text().toLowerCase();

        if (text.contains("show") || text.contains("find") || text.contains("list")) {
            return QueryIntent.FIND_ENTITIES;
        }
        if (text.contains("impact") || text.contains("affect") || text.contains("change")) {
            return QueryIntent.IMPACT_ANALYSIS;
        }
        if (text.contains("depend") || text.contains("connect") || text.contains("relate")) {
            return QueryIntent.ANALYZE_DEPENDENCIES;
        }

        return QueryIntent.GENERAL_QUERY;
    }

    private List<String> extractSemanticConcepts(CoreDocument document) {
        List<String> concepts = new ArrayList<>();

        // Business-specific concept mapping
        Map<String, List<String>> conceptMap = Map.of(
                "revenue", List.of("revenue", "sales", "income", "earnings"),
                "cost", List.of("cost", "expense", "expenditure", "spending"),
                "profit", List.of("profit", "margin", "earnings", "net income"),
                "marketing", List.of("marketing", "advertising", "promotion", "campaign")
        );

        String text = document.text().toLowerCase();

        for (Map.Entry<String, List<String>> entry : conceptMap.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (text.contains(keyword)) {
                    concepts.add(entry.getKey());
                    break;
                }
            }
        }

        return concepts;
    }

    public Map<String, Object> extractFilters(CoreDocument document) {
        Map<String, Object> filters = new HashMap<>();
        List<CoreLabel> tokens = document.tokens();

        for (int i = 0; i < tokens.size() - 2; i++) {
            String word = tokens.get(i).word();
            String next = tokens.get(i + 1).word();
            String nextNext = tokens.get(i + 2).word();

            if (isComparisonOperator(next)) {
                Filter filter = new Filter();
                filter.setField(word);
                filter.setOperator(next);
                filter.setValue(nextNext);
                filters.put(word, filter);
            }
        }

        return filters;
    }

    private boolean isComparisonOperator(String token) {
        return token.equals("=") || token.equals("==") ||
                token.equals(">") || token.equals("<") ||
                token.equals(">=") || token.equals("<=") ||
                token.equals("!=");
    }
}