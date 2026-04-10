package com.bajansdk.aiobingo;

import com.bajansdk.aiobingo.model.BingoBoard;
import com.bajansdk.aiobingo.model.GameEvent;
import com.bajansdk.aiobingo.model.LeaderboardEntry;
import com.bajansdk.aiobingo.model.TeamProgress;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import javax.inject.Inject;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

@Slf4j
public class BingoApiClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient http;
    private final Gson gson;
    private final AioBingoConfig config;

    @Inject
    public BingoApiClient(OkHttpClient http, Gson ignoredGson, AioBingoConfig config) {
        this.http = http;
        this.gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();
        this.config = config;
    }

    /**
     * Fetches the bingo board definition (tiles, grid size, event metadata).
     */
    public BingoBoard fetchBoard(String boardToken) throws IOException {
        String url = config.apiBaseUrl() + "/api/board/" + boardToken;
        return get(url, BingoBoard.class);
    }

    /**
     * Fetches this team's per-tile progress on the given board.
     */
    public TeamProgress fetchTeamProgress(String boardToken, String teamToken) throws IOException {
        String url = config.apiBaseUrl() + "/api/board/" + boardToken + "/progress/" + teamToken;
        return get(url, TeamProgress.class);
    }

    /**
     * Fetches the ranked leaderboard for all teams on this board.
     */
    public List<LeaderboardEntry> fetchLeaderboard(String boardToken) throws IOException {
        String url = config.apiBaseUrl() + "/api/board/" + boardToken + "/leaderboard";
        Type listType = new TypeToken<List<LeaderboardEntry>>() {}.getType();
        String body = getBody(url);
        if (body == null) return Collections.emptyList();
        return gson.fromJson(body, listType);
    }

    /**
     * Submits a single game event to the API.
     * The API will evaluate whether this event contributes to any bingo tile.
     */
    public void submitEvent(String teamToken, GameEvent event) throws IOException {
        String url = config.apiBaseUrl() + "/api/events/" + teamToken;
        post(url, gson.toJson(event));
    }

    /**
     * Submits a batch of game events in one HTTP request (preferred over many single calls).
     */
    public void submitEventBatch(String teamToken, List<GameEvent> events) throws IOException {
        if (events.isEmpty()) return;
        String url = config.apiBaseUrl() + "/api/events/" + teamToken + "/batch";
        post(url, gson.toJson(events));
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private <T> T get(String url, Class<T> clazz) throws IOException {
        String body = getBody(url);
        if (body == null) throw new IOException("Empty response from " + url);
        return gson.fromJson(body, clazz);
    }

    private String getBody(String url) throws IOException {
        Request request = new Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json")
            .get()
            .build();

        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new BingoApiException(response.code(), "HTTP " + response.code() + " from " + url);
            }
            ResponseBody body = response.body();
            return body != null ? body.string() : null;
        }
    }

    private void post(String url, String jsonBody) throws IOException {
        RequestBody body = RequestBody.create(JSON, jsonBody);
        Request request = new Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json")
            .post(body)
            .build();

        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new BingoApiException(response.code(), "POST " + url + " returned HTTP " + response.code());
            }
        }
    }
}
