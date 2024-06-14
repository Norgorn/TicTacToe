package com.norgorn.service;

import com.google.gson.Gson;
import com.norgorn.model.CellSymbol;
import com.norgorn.model.GameStatus;
import com.norgorn.model.MoveResponse;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class GameClient {

    @Value("${server.port}")
    int selfPort;

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson(); // No config required

    @SneakyThrows
    public int seed() {
        try (Response response = get("seed")) {
            generalCheckResponseCode(response);
            return gson.fromJson(response.body().string(), Integer.class);
        }
    }

    @SneakyThrows
    public CellSymbol negotiateSymbol(int seed) {
        try (Response response = get("symbol/" + seed)) {
            generalCheckResponseCode(response);
            return gson.fromJson(response.body().string(), CellSymbol.class);
        }
    }

    @SneakyThrows
    public void sendPing() {
        try (Response response = get("ping")) {
            generalCheckResponseCode(response);
            log.info("Got ping response: {}", response.body().string());
        }
    }

    @SneakyThrows
    public MoveResponse sendMove(MoveResponse gameBoard) {
        try (Response response = post("move", gameBoard)) {
            log.info("Got move response: {}", response.code());

            if (response.code() == 400) {
                log.error("We got bad request, validation failed: {}", response.body().string());
                return new MoveResponse(null, GameStatus.RECEIVED_ERROR);
            }
            generalCheckResponseCode(response);

            String json = response.body().string();
            return gson.fromJson(json, MoveResponse.class);
        }
    }

    public void resetSilently() {
        try {
            try (Response response = post("reset", "")) {
                log.info("Reset them: {}", response.code());
            }
        } catch (Exception ignore) {
        }
    }

    private Response get(String path) {
        Request.Builder builder = new Request.Builder().get();
        return executeRequest(path, builder);
    }

    private <T> Response post(String path, T body) {
        String json = gson.toJson(body);
        Request.Builder builder = new Request.Builder().post(RequestBody.create(json, MediaType.get("application/json")));
        return executeRequest(path, builder);
    }

    @SneakyThrows
    private Response executeRequest(String path, Request.Builder builder) {
        int otherPort = getOtherPort();
        Request request = builder
                .url("http://localhost:" + otherPort + "/" + path)
                .build();
        Call call = client.newCall(request);
        return call.execute();
    }

    private int getOtherPort() {
        // Very primitive, but works for the demo. In reality the other server port should be taken from config params.
        return selfPort == 8080 ? 8081 : 8080;
    }

    @SneakyThrows
    private void generalCheckResponseCode(Response response) {
        if (response.code() != 200 || response.body() == null)
            throw new IllegalStateException("Unexpected response " + response.code() + ": "
                    + (response.body() == null ? "no body" : response.body().string())
            );
    }
}
