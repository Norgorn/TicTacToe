package com.norgorn.model;

import java.util.Optional;

/*
Error could be a more complex structure, but keep it simple
 */
public record MoveResult(Optional<GameBoard> eitherNextBoard, Optional<String> orError) {

    public MoveResult(GameBoard board) {
        this(Optional.of(board), Optional.empty());
    }

    public MoveResult(String error) {
        this(Optional.empty(), Optional.of(error));
    }

    public MoveResult(Optional<String> error) {
        this(Optional.empty(), error);
    }
}
