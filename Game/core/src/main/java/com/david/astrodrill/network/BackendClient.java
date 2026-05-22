package com.david.astrodrill.network;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.david.astrodrill.network.dto.ErrorResponse;
import com.david.astrodrill.network.dto.LeaderboardEntry;
import com.david.astrodrill.network.dto.LoadResponse;
import com.david.astrodrill.network.dto.LoginRequest;
import com.david.astrodrill.network.dto.LoginResponse;
import com.david.astrodrill.network.dto.RegisterRequest;
import com.david.astrodrill.network.dto.SaveRequest;
import com.david.astrodrill.network.dto.SaveResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Async client for the AstroDrill backend. Uses libGDX Json + Gdx.net so it
 * compiles for both desktop (LWJGL3) and web (GWT). All callbacks fire via
 * Gdx.app.postRunnable so they're safe for screen transitions and stage mutation.
 */
public final class BackendClient {

    // Production backend (Railway). For local dev, point this at http://localhost:8080/api/game.
    public static String baseUrl = "https://astrodrill-backend-production.up.railway.app/api/game";

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(BackendException ex);
    }

    private BackendClient() {}

    public static void register(RegisterRequest req, Callback<LoginResponse> cb) {
        postJson("/register", req, LoginResponse.class, cb);
    }

    public static void login(LoginRequest req, Callback<LoginResponse> cb) {
        postJson("/login", req, LoginResponse.class, cb);
    }

    public static void save(SaveRequest req, Callback<SaveResponse> cb) {
        postJson("/save", req, SaveResponse.class, cb);
    }

    public static void load(Long playerId, Callback<LoadResponse> cb) {
        getJson("/load/" + playerId, LoadResponse.class, cb);
    }

    public static void getLeaderboard(final Callback<List<LeaderboardEntry>> cb) {
        Net.HttpRequest request = new HttpRequestBuilder()
                .newRequest()
                .method(Net.HttpMethods.GET)
                .url(baseUrl + "/leaderboard")
                .header("Accept", "application/json")
                .build();
        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int status = httpResponse.getStatus().getStatusCode();
                String body = httpResponse.getResultAsString();
                if (status >= 200 && status < 300) {
                    List<LeaderboardEntry> list = parseLeaderboard(body);
                    postSuccess(cb, list);
                } else {
                    postError(cb, parseError(status, body));
                }
            }

            @Override
            public void failed(Throwable t) {
                postError(cb, new BackendException(0, "NETWORK_ERROR", t.getMessage()));
            }

            @Override
            public void cancelled() {
                postError(cb, new BackendException(0, "CANCELLED", "Request cancelled"));
            }
        });
    }

    // ───────────────────────── internals ─────────────────────────

    private static Json newJson() {
        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        json.setUsePrototypes(false);
        json.setIgnoreUnknownFields(true);
        return json;
    }

    private static <T> void postJson(String path, Object body, Class<T> responseType, Callback<T> cb) {
        String payload = newJson().toJson(body, body.getClass());
        Net.HttpRequest request = new HttpRequestBuilder()
                .newRequest()
                .method(Net.HttpMethods.POST)
                .url(baseUrl + path)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .content(payload)
                .build();
        send(request, responseType, cb);
    }

    private static <T> void getJson(String path, Class<T> responseType, Callback<T> cb) {
        Net.HttpRequest request = new HttpRequestBuilder()
                .newRequest()
                .method(Net.HttpMethods.GET)
                .url(baseUrl + path)
                .header("Accept", "application/json")
                .build();
        send(request, responseType, cb);
    }

    private static <T> void send(Net.HttpRequest request, final Class<T> responseType, final Callback<T> cb) {
        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int status = httpResponse.getStatus().getStatusCode();
                String body = httpResponse.getResultAsString();
                if (status >= 200 && status < 300) {
                    T parsed = (body == null || body.isEmpty())
                            ? null
                            : newJson().fromJson(responseType, body);
                    postSuccess(cb, parsed);
                } else {
                    postError(cb, parseError(status, body));
                }
            }

            @Override
            public void failed(Throwable t) {
                postError(cb, new BackendException(0, "NETWORK_ERROR", t.getMessage()));
            }

            @Override
            public void cancelled() {
                postError(cb, new BackendException(0, "CANCELLED", "Request cancelled"));
            }
        });
    }

    private static List<LeaderboardEntry> parseLeaderboard(String body) {
        List<LeaderboardEntry> out = new ArrayList<>();
        if (body == null || body.isEmpty()) return out;
        Array<LeaderboardEntry> arr = newJson().fromJson(Array.class, LeaderboardEntry.class, body);
        if (arr == null) return out;
        for (LeaderboardEntry e : arr) out.add(e);
        return out;
    }

    private static BackendException parseError(int status, String body) {
        try {
            if (body != null && !body.isEmpty()) {
                ErrorResponse err = newJson().fromJson(ErrorResponse.class, body);
                if (err != null && err.error != null) {
                    return new BackendException(status, err.error, err.message != null ? err.message : err.error);
                }
            }
        } catch (Exception ignored) {}
        return new BackendException(status, "HTTP_" + status, body != null ? body : "Request failed");
    }

    private static <T> void postSuccess(final Callback<T> cb, final T result) {
        Gdx.app.postRunnable(new Runnable() { @Override public void run() { cb.onSuccess(result); } });
    }

    private static <T> void postError(final Callback<T> cb, final BackendException ex) {
        Gdx.app.postRunnable(new Runnable() { @Override public void run() { cb.onError(ex); } });
    }
}
