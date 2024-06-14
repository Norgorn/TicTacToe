package com.norgorn.service;

import com.norgorn.model.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Scope("prototype")
@Log4j2
public class GameStateBean {

    private static final int ROWS_COUNT = 3;
    private static final int COLS_COUNT = 3;

    @Getter
    @Setter
    CellSymbol mySymbol;

    @Getter
    @Setter
    private GameBoard previousBoard = initEmptyBoard();

    @Getter
    @Setter
    private GameStatus gameStatus = GameStatus.WAITING;

    private final Map<String, HistoryItem> history = new ConcurrentHashMap<>();

    private Random random;
    private final AtomicInteger moveCounter = new AtomicInteger(0);

    public void init(CellSymbol mySymbol, int seed) {
        this.mySymbol = mySymbol;
        random = new Random(seed);
    }

    public Optional<GameBoard> getBoardFromHistory(GameBoard incomingBoard) {
        return Optional.ofNullable(history.get(incomingBoard.toString()))
                .map(HistoryItem::outgoingBoard);
    }

    public Optional<String> validateMove(GameBoard currentBoard) {
        return validateMove(previousBoard, currentBoard);
    }

    public MoveResponse makeFirstMove() {
        return makeMove(initEmptyBoard());
    }

    public MoveResponse makeMove(GameBoard currentBoard) {
        MoveResponse newBoard = makeMoveInternal(currentBoard);
        log.info("Moving from \n{}\n To \n{}\nWith status {}", currentBoard, newBoard.board(), newBoard.newStatus());
        previousBoard = newBoard.board();
        return newBoard;
    }

    public int getMoveCounter() {
        return moveCounter.get();
    }

    public int incrementAndGetMoveCounter() {
        return moveCounter.incrementAndGet();
    }

    Optional<String> validateMove(GameBoard previousBoard, GameBoard currentBoard) {
        if (currentBoard.rows().size() != ROWS_COUNT
                || currentBoard.rows().stream().anyMatch(r -> r.size() != COLS_COUNT)) {
            return Optional.of("invalid board size");
        }
        int additions = 0;
        for (int rowNum = 0; rowNum < ROWS_COUNT; rowNum++) {
            List<CellSymbol> previousRow = previousBoard.rows().get(rowNum);
            List<CellSymbol> currentRow = currentBoard.rows().get(rowNum);
            for (int colNum = 0; colNum < COLS_COUNT; colNum++) {
                CellSymbol prevVal = previousRow.get(colNum);
                CellSymbol currentVal = currentRow.get(colNum);
                if (prevVal == CellSymbol.EMPTY && currentVal != CellSymbol.EMPTY) {
                    if (currentVal == mySymbol)
                        return Optional.of("moved wrong symbol " + currentVal);
                    additions++;
                } else if (prevVal != currentVal) {
                    return Optional.of("invalid move: " + prevVal + "->" + currentVal + " at " + rowNum + ":" + colNum);
                }
            }
        }
        if (additions == 0)
            return Optional.of("no move");
        if (additions != 1)
            return Optional.of("multiple moves");
        return Optional.empty();
    }

    private MoveResponse makeMoveInternal(GameBoard currentBoard) {
        record Slot(int row, int column) {
        }

        List<Slot> emptySlots = new ArrayList<>();

        List<List<CellSymbol>> newBoardValues = new ArrayList<>();
        for (int rowNum = 0; rowNum < ROWS_COUNT; rowNum++) {
            var newRow = new ArrayList<CellSymbol>();
            newBoardValues.add(newRow);
            List<CellSymbol> currentRow = currentBoard.rows().get(rowNum);
            for (int colNum = 0; colNum < COLS_COUNT; colNum++) {
                CellSymbol currentVal = currentRow.get(colNum);
                newRow.add(currentVal);
                if (currentVal == CellSymbol.EMPTY) {
                    emptySlots.add(new Slot(rowNum, colNum));
                }
            }
        }

        if (!emptySlots.isEmpty()) {
            int slotNum = random.nextInt(emptySlots.size()); // cache Random
            Slot slot = emptySlots.get(slotNum);
            newBoardValues.get(slot.row).set(slot.column, mySymbol);
        }

        if (emptySlots.isEmpty())
            gameStatus = GameStatus.DRAW;
        else if (detectVictory(newBoardValues))
            gameStatus = GameStatus.WON;
        else
            gameStatus = GameStatus.WAITING;
        GameBoard newBoard = new GameBoard(newBoardValues);
        MoveResponse moveResponse = new MoveResponse(newBoard, gameStatus);
        history.put(moveResponse.board().toString(), new HistoryItem(currentBoard, newBoard));
        return moveResponse;
    }

    private static GameBoard initEmptyBoard() {
        List<List<CellSymbol>> emptyRows = new ArrayList<>();
        for (int i = 0; i < ROWS_COUNT; i++) {
            List<CellSymbol> row = new ArrayList<>(COLS_COUNT);
            for (int j = 0; j < COLS_COUNT; j++) {
                row.add(CellSymbol.EMPTY);
            }
            emptyRows.add(row);
        }
        return new GameBoard(emptyRows);
    }

    boolean detectVictory(List<List<CellSymbol>> newBoard) {
        boolean won = false;
        // iterate rows
        for (int rowNum = 0; rowNum < ROWS_COUNT && !won; rowNum++) {
            List<CellSymbol> row = newBoard.get(rowNum);
            won = true;
            for (int columnNum = 0; columnNum < COLS_COUNT && won; columnNum++) {
                won = row.get(columnNum) == mySymbol;
            }
        }

        // iterate columns
        for (int rowNum = 0; rowNum < ROWS_COUNT && !won; rowNum++) {
            won = true;
            for (int columnNum = 0; columnNum < COLS_COUNT && won; columnNum++) {
                List<CellSymbol> row = newBoard.get(columnNum);
                CellSymbol symbol = row.get(rowNum);
                won = symbol == mySymbol;
            }
        }
        return won;
    }
}
