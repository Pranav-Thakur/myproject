package org.superjoin;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.superjoin.component.FormulaAnalyzer;
import org.superjoin.component.SemanticAnalyzer;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
@SpringBootApplication
@EnableJpaRepositories
@EnableWebSocket
public class SpreadsheetBrainApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpreadsheetBrainApplication.class, args);
    }
/*
    @Bean
    public Driver neo4jDriver() {
        return GraphDatabase.driver("bolt://localhost:7687",
                AuthTokens.basic("neo4j", "test12345"));
    }*/
}