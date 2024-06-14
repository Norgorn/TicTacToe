package com.norgorn.model;

public enum CellSymbol {

    EMPTY("_"), CROSS("X"), NAUGHT("O");

    private final String symbol;

    CellSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }
}
