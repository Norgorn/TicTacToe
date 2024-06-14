package com.norgorn.service;

import com.norgorn.model.CellSymbol;
import com.norgorn.model.GameBoard;
import com.norgorn.model.GameStatus;
import com.norgorn.model.MoveResponse;
import kotlin.Pair;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Log4j2
@Service
public class GameProcessor {

    @Autowired
    ApplicationContext appContext;
    @Autowired
    GameClient client;

    @Value("${app.move_delay}")
    Duration moveDelay;

    @Value("${app.move_timeout}")
    Duration moveTimeout;

    GameStateBean stateBean;

    private ScheduledExecutorService scheduler;

    public String getState() {
        return stateBean == null
                ? "NOT_STARTED"
                : "Status: %s\n Board:\n%s".formatted(stateBean.getGameStatus().name(), stateBean.getPreviousBoard());
    }

    public int seed() {
        return new Random().nextInt(100); // Seed here may be random or any stable value
    }

    public synchronized void run(Optional<Integer> seedOpt) {
        stateBean = appContext.getBean(GameStateBean.class);
        if (scheduler != null) {
            scheduler.shutdownNow();
            client.resetSilently();
        }
        scheduler = Executors.newScheduledThreadPool(1);

        Pair<CellSymbol, Integer> p;
        try {
            p = negotiateSymbol(seedOpt);
        } catch (Exception e) {
            stateBean.setGameStatus(GameStatus.DETECTED_ERROR);
            throw new IllegalStateException("Symbol negotiation failed", e);
        }
        CellSymbol ourSymbol = p.getFirst();
        Integer seed = p.getSecond();
        init(ourSymbol, seed);

        if (ourSymbol == CellSymbol.CROSS) {
            log.info("We move first");
            makeMove(stateBean.makeFirstMove());
        } else {
            log.info("We wait their move");
        }
    }

    public CellSymbol negotiateSymbol(int seed) {
        Random rand = new Random(seed);
        CellSymbol ourSymbol = rand.nextDouble() > 0.5 ? CellSymbol.CROSS : CellSymbol.NAUGHT;
        if (ourSymbol == CellSymbol.CROSS) {
            log.info("We move first");
            stateBean = appContext.getBean(GameStateBean.class);
            init(ourSymbol, seed);
            // delay here doesn't matter ant may as well be 0
            scheduler.schedule(() -> makeMove(stateBean.makeFirstMove()), moveDelay.toMillis(), TimeUnit.MILLISECONDS);
        }
        return ourSymbol;
    }

    public synchronized void makeMove(MoveResponse move) {
        try {
            MoveResponse moveResponse = client.sendMove(move);
            checkedMakeMove(moveResponse,
                    (mr) -> scheduler.schedule(() ->
                            makeMove(stateBean.makeMove(mr.board())), moveDelay.toMillis(), TimeUnit.MILLISECONDS)
            );
        } catch (Exception e) {
            stateBean.setGameStatus(GameStatus.DETECTED_ERROR);
            throw new IllegalStateException("Failed to make move", e);
        }
    }

    public synchronized ResponseEntity<?> gotMove(MoveResponse move) {
        if (stateBean == null) {
            log.info("They move first");
            stateBean = appContext.getBean(GameStateBean.class);
            scheduler = Executors.newScheduledThreadPool(1);
            init(CellSymbol.NAUGHT, seed());
        }

        Optional<GameBoard> fromHistory = stateBean.getBoardFromHistory(move.board());
        if (fromHistory.isPresent()) {
            return ResponseEntity.ok(new MoveResponse(fromHistory.get(), GameStatus.WAITING));
        }
        return checkedMakeMove(move, this::processMoveRequest)
                .orElseGet(() -> ResponseEntity.ok().build());
    }

    public synchronized void reset() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        scheduler = Executors.newScheduledThreadPool(1);
    }

    private Pair<CellSymbol, Integer> negotiateSymbol(Optional<Integer> seedOpt) {
        int seed = seedOpt.orElseGet(() -> client.seed());
        log.info("Got seed {}", seed);
        CellSymbol theirSymbol = client.negotiateSymbol(seed);
        CellSymbol ourSymbol = theirSymbol == CellSymbol.CROSS ? CellSymbol.NAUGHT : CellSymbol.CROSS;
        return new Pair<>(ourSymbol, seed);
    }

    private void init(CellSymbol ourSymbol, int seed) {
        stateBean.setMySymbol(ourSymbol);
        stateBean.init(ourSymbol, seed);
        log.info("init done with symbol {}", ourSymbol);
    }

    private <T> Optional<T> checkedMakeMove(MoveResponse moveResponse, Function<MoveResponse, T> moveAction) {
        GameStatus newStatus = moveResponse.newStatus();
        if (stateBean.getGameStatus() != GameStatus.WAITING) {
            log.info("We have status {} and not waiting their move", stateBean.getGameStatus());
            return Optional.empty();
        }
        return switch (newStatus) {
            case WON -> {
                stateBean.setGameStatus(GameStatus.LOST);
                log.info("We lost (we moved {}): \n{}", stateBean.getMySymbol(), moveResponse.board());
                scheduler.shutdownNow();
                yield Optional.empty();
            }
            case DRAW -> {
                stateBean.setGameStatus(GameStatus.DRAW);
                log.info("Draw: \n{}", moveResponse.board());
                scheduler.shutdownNow();
                yield Optional.empty();
            }
            case WAITING -> {
                log.info("They wait our move now");
                stateBean.setGameStatus(GameStatus.THINKING);
                yield Optional.of(moveAction.apply(moveResponse));
            }
            case RECEIVED_ERROR -> {
                log.info("Received error (see log entry above)");
                stateBean.setGameStatus(GameStatus.RECEIVED_ERROR);
                scheduler.shutdownNow();
                yield Optional.empty();
            }
            default -> throw new IllegalStateException("Status not supported " + newStatus);
        };
    }

    private ResponseEntity<?> processMoveRequest(MoveResponse move) {
        ResponseEntity<?> response = validateAndMoveToResponse(move);
        int moveCounter = stateBean.incrementAndGetMoveCounter();
        scheduler.schedule(() -> {
            if (stateBean != null && stateBean.getMoveCounter() == moveCounter) {
                stateBean.setGameStatus(GameStatus.DETECTED_ERROR);
                log.error("Their move timeout after {} milliseconds", moveTimeout.toMillis());
            }
        }, moveTimeout.toMillis(), TimeUnit.MILLISECONDS);
        return response;
    }

    private ResponseEntity<?> validateAndMoveToResponse(MoveResponse move) {
        Optional<String> validationError = stateBean.validateMove(move.board());
        if (validationError.isPresent()) {
            stateBean.setGameStatus(GameStatus.DETECTED_ERROR);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(validationError.get());
        }

        MoveResponse moveResult = stateBean.makeMove(move.board());
        return ResponseEntity.ok(moveResult);
    }
}
