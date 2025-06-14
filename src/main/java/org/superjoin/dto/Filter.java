package org.superjoin.dto;

import lombok.Data;

@Data
public class Filter {
    private String field;
    private String operator;
    private String value;
}
