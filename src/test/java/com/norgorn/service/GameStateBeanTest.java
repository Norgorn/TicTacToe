package com.norgorn.service;

import com.norgorn.model.CellSymbol;
import com.norgorn.model.GameBoard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GameStateBeanTest {

    GameStateBean sut = new GameStateBean(); // sut -> System Under Test

    @BeforeEach
    public void init() {
        sut.mySymbol = CellSymbol.CROSS;
    }

    @Test
    public void validateMove_whenNoError() {
        GameBoard previousBoard = new GameBoard(List.of(
                List.of(CellSymbol.EMPTY, CellSymbol.CROSS, CellSymbol.EMPTY),
                List.of(CellSymbol.EMPTY, CellSymbol.CROSS, CellSymbol.EMPTY),
                List.of(CellSymbol.EMPTY, CellSymbol.NAUGHT, CellSymbol.EMPTY)
        ));

        GameBoard currentBoard = new GameBoard(List.of(
                List.of(CellSymbol.EMPTY, CellSymbol.CROSS, CellSymbol.EMPTY),
                List.of(CellSymbol.EMPTY, CellSymbol.CROSS, CellSymbol.EMPTY),
                List.of(CellSymbol.NAUGHT, CellSymbol.NAUGHT, CellSymbol.EMPTY)
        ));
        assertEquals(Optional.empty(), sut.validateMove(previousBoard, currentBoard));
    }

    @Test
    public void validateMove_whenNoMove() {
        GameBoard previousBoard = new GameBoard(List.of(
                List.of(CellSymbol.EMPTY, CellSymbol.CROSS, CellSymbol.EMPTY),
                List.of(CellSymbol.EMPTY, CellSymbol.CROSS, CellSymbol.EMPTY),
                List.of(CellSymbol.EMPTY, CellSymbol.NAUGHT, CellSymbol.EMPTY)
        ));
        GameBoard currentBoard = new GameBoard(List.of(
                List.of(CellSymbol.EMPTY, CellSymbol.CROSS, CellSymbol.EMPTY),
                List.of(CellSymbol.EMPTY, CellSymbol.CROSS, CellSymbol.EMPTY),
                List.of(CellSymbol.EMPTY, CellSymbol.NAUGHT, CellSymbol.EMPTY)
        ));

        assertEquals(Optional.of("no move"), sut.validateMove(previousBoard, currentBoard));
    }

    @Test
    public void validateMove_whenWrongSymbol() {
        GameBoard previousBoard = new GameBoard(List.of(
                List.of(CellSymbol.EMPTY, CellSymbol.CROSS, CellSymbol.EMPTY),
                List.of(CellSymbol.EMPTY, CellSymbol.CROSS, CellSymbol.EMPTY),
                List.of(CellSymbol.EMPTY, CellSymbol.NAUGHT, CellSymbol.EMPTY)
        ));
        GameBoard currentBoard = new GameBoard(List.of(
                List.of(CellSymbol.EMPTY, CellSymbol.CROSS, CellSymbol.EMPTY),
                List.of(CellSymbol.EMPTY, CellSymbol.CROSS, CellSymbol.EMPTY),
                List.of(CellSymbol.CROSS, CellSymbol.NAUGHT, CellSymbol.EMPTY)
        ));

        assertEquals(Optional.of("moved wrong symbol CROSS"), sut.validateMove(previousBoard, currentBoard));
    }

    @Test
    public void validateMove_whenInvalidMove() {
        GameBoard previousBoard = new GameBoard(List.of(
                List.of(CellSymbol.EMPTY, CellSymbol.CROSS, CellSymbol.EMPTY),
                List.of(CellSymbol.EMPTY, CellSymbol.CROSS, CellSymbol.EMPTY),
                List.of(CellSymbol.EMPTY, CellSymbol.NAUGHT, CellSymbol.EMPTY)
        ));
        GameBoard currentBoard = new GameBoard(List.of(
                List.of(CellSymbol.EMPTY, CellSymbol.CROSS, CellSymbol.EMPTY),
                List.of(CellSymbol.EMPTY, CellSymbol.NAUGHT, CellSymbol.EMPTY),
                List.of(CellSymbol.EMPTY, CellSymbol.NAUGHT, CellSymbol.EMPTY)
        ));

        assertEquals(Optional.of("invalid move: CROSS->NAUGHT at 1:1"), sut.validateMove(previousBoard, currentBoard));
    }

    @Test
    public void validateMove_whenMultipleMoves() {
        GameBoard previousBoard = new GameBoard(List.of(
                List.of(CellSymbol.EMPTY, CellSymbol.CROSS, CellSymbol.EMPTY),
                List.of(CellSymbol.EMPTY, CellSymbol.CROSS, CellSymbol.EMPTY),
                List.of(CellSymbol.EMPTY, CellSymbol.NAUGHT, CellSymbol.EMPTY)
        ));

        GameBoard currentBoard = new GameBoard(List.of(
                List.of(CellSymbol.EMPTY, CellSymbol.CROSS, CellSymbol.EMPTY),
                List.of(CellSymbol.EMPTY, CellSymbol.CROSS, CellSymbol.EMPTY),
                List.of(CellSymbol.NAUGHT, CellSymbol.NAUGHT, CellSymbol.NAUGHT)
        ));
        assertEquals(Optional.of("multiple moves"), sut.validateMove(previousBoard, currentBoard));
    }

    @Test
    public void validateMove_whenInvalidBoardSize() {
        GameBoard previousBoard = new GameBoard(List.of(
                List.of(CellSymbol.EMPTY, CellSymbol.CROSS, CellSymbol.EMPTY),
                List.of(CellSymbol.EMPTY, CellSymbol.CROSS, CellSymbol.EMPTY),
                List.of(CellSymbol.EMPTY, CellSymbol.NAUGHT, CellSymbol.EMPTY)
        ));

        GameBoard currentBoard = new GameBoard(List.of(
                List.of(CellSymbol.EMPTY, CellSymbol.CROSS, CellSymbol.EMPTY),
                List.of(CellSymbol.EMPTY, CellSymbol.CROSS, CellSymbol.EMPTY),
                List.of(CellSymbol.NAUGHT, CellSymbol.NAUGHT, CellSymbol.NAUGHT),
                List.of(CellSymbol.NAUGHT, CellSymbol.NAUGHT, CellSymbol.NAUGHT)
        ));
        assertEquals(Optional.of("invalid board size"), sut.validateMove(previousBoard, currentBoard));
    }

    // And many more situations
    // ...

    @Test
    public void detectVictory_whenColumnWon() {
        var board = List.of(
                List.of(CellSymbol.CROSS, CellSymbol.EMPTY, CellSymbol.EMPTY),
                List.of(CellSymbol.CROSS, CellSymbol.EMPTY, CellSymbol.EMPTY),
                List.of(CellSymbol.CROSS, CellSymbol.EMPTY, CellSymbol.EMPTY)
        );
        assertTrue(sut.detectVictory(board));

        board = List.of(
                List.of(CellSymbol.EMPTY, CellSymbol.CROSS, CellSymbol.EMPTY),
                List.of(CellSymbol.EMPTY, CellSymbol.CROSS, CellSymbol.EMPTY),
                List.of(CellSymbol.EMPTY, CellSymbol.CROSS, CellSymbol.EMPTY)
        );
        assertTrue(sut.detectVictory(board));
    }

    @Test
    public void detectVictory_whenRowWon() {
        var board = List.of(
                List.of(CellSymbol.CROSS, CellSymbol.CROSS, CellSymbol.CROSS),
                List.of(CellSymbol.EMPTY, CellSymbol.NAUGHT, CellSymbol.EMPTY),
                List.of(CellSymbol.EMPTY, CellSymbol.NAUGHT, CellSymbol.EMPTY)
        );
        assertTrue(sut.detectVictory(board));

        board = List.of(
                List.of(CellSymbol.NAUGHT, CellSymbol.CROSS, CellSymbol.NAUGHT),
                List.of(CellSymbol.EMPTY, CellSymbol.NAUGHT, CellSymbol.EMPTY),
                List.of(CellSymbol.CROSS, CellSymbol.CROSS, CellSymbol.CROSS)
        );
        assertTrue(sut.detectVictory(board));
    }

    @Test
    public void detectVictory_whenNoVictory() {
        var board = List.of(
                List.of(CellSymbol.CROSS, CellSymbol.CROSS, CellSymbol.EMPTY),
                List.of(CellSymbol.EMPTY, CellSymbol.NAUGHT, CellSymbol.EMPTY),
                List.of(CellSymbol.EMPTY, CellSymbol.NAUGHT, CellSymbol.EMPTY)
        );
        assertFalse(sut.detectVictory(board));

        board = List.of(
                List.of(CellSymbol.NAUGHT, CellSymbol.CROSS, CellSymbol.NAUGHT),
                List.of(CellSymbol.EMPTY, CellSymbol.NAUGHT, CellSymbol.EMPTY),
                List.of(CellSymbol.CROSS, CellSymbol.EMPTY, CellSymbol.CROSS)
        );
        assertFalse(sut.detectVictory(board));
    }
}
