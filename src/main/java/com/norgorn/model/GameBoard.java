package com.norgorn.model;

import java.util.ArrayList;
import java.util.List;


public record GameBoard(List<List<CellSymbol>> rows) {

    public GameBoard(List<List<CellSymbol>> rows) {
        this.rows = new ArrayList<>(rows); // TODO deep copy
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (List<CellSymbol> row : rows) {
            for (CellSymbol cellSymbol : row) {
                builder.append(cellSymbol.symbol()).append("  ");
            }
            builder.append("\n");
        }
        return builder.toString();
    }
}
