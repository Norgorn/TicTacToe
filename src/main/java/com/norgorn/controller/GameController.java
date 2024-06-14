package com.norgorn.controller;


import com.norgorn.model.CellSymbol;
import com.norgorn.model.MoveResponse;
import com.norgorn.service.GameClient;
import com.norgorn.service.GameProcessor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Log4j2
@RestController
public class GameController {

    @Autowired
    GameProcessor gameProcessor;

    @Autowired
    GameClient client;

    @GetMapping(value = "/ping")
    public String pingPong() {
        log.info("ping");
        return "pong";
    }

    @PostMapping(value = "/ping")
    public void doPingPong() {
        client.sendPing();
    }

    @PostMapping(value = "/run")
    public void runGame() {
        gameProcessor.run(Optional.empty());
    }

    @PostMapping(value = "/run/{seed}")
    public void runGame(@PathVariable(value = "seed", required = false) Optional<Integer> seed) {
        gameProcessor.run(seed);
    }

    @GetMapping(value = "/seed")
    public int getSeed() {
        int seed = gameProcessor.seed();
        log.info("Returning seed {}", seed);
        return seed;
    }

    @GetMapping(value = "/symbol/{seed}")
    public CellSymbol getSymbol(@PathVariable("seed") int seed) {
        return gameProcessor.negotiateSymbol(seed);
    }

    @PostMapping(value = "/move")
    public ResponseEntity<?> gotMove(@RequestBody MoveResponse board) {
        return gameProcessor.gotMove(board);
    }

    @PostMapping(value = "/reset")
    public void gotMove() {
        gameProcessor.reset();
    }

    @GetMapping(value = "/status")
    public String status() {
        return gameProcessor.getState();
    }
}
