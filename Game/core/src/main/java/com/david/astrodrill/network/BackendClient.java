package com.david.astrodrill.network;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.david.astrodrill.network.dto.ErrorResponse;
import com.david.astrodrill.network.dto.LeaderboardEntry;
import com.david.astrodrill.network.dto.LoadResponse;
import com.david.astrodrill.network.dto.LoginRequest;
import com.david.astrodrill.network.dto.LoginResponse;
import com.david.astrodrill.network.dto.RegisterRequest;
import com.david.astrodrill.network.dto.SaveRequest;
import com.david.astrodrill.network.dto.SaveResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Async client for the AstroDrill backend. All callbacks fire on the GL/render thread
 * via Gdx.app.postRunnable, so they're safe for screen transitions and stage mutation.
 */
public final class BackendClient {

    public static String baseUrl = "http://localhost:8080/api/game";

    private static final Gson GSON = new Gson();

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

    /**
     * Blocking save used by save-on-exit. Skips the Gdx.app.postRunnable hop because
     * Main.dispose() runs after the render loop has stopped, so any runnable posted
     * to it would never fire. Returns true if the save completed successfully within
     * the timeout, false otherwise.
     */
    public static boolean saveAndWait(SaveRequest req, long timeoutMs) {
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = { false };
        Net.HttpRequest request = new HttpRequestBuilder()
                .newRequest()
                .method(Net.HttpMethods.POST)
                .url(baseUrl + "/save")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .content(GSON.toJson(req))
                .build();
        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int status = httpResponse.getStatus().getStatusCode();
                success[0] = status >= 200 && status < 300;
                latch.countDown();
            }
            @Override
            public void failed(Throwable t) { latch.countDown(); }
            @Override
            public void cancelled() { latch.countDown(); }
        });
        try {
            return latch.await(timeoutMs, TimeUnit.MILLISECONDS) && success[0];
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public static void load(Long playerId, Callback<LoadResponse> cb) {
        getJson("/load/" + playerId, LoadResponse.class, cb);
    }

    public static void getLeaderboard(Callback<List<LeaderboardEntry>> cb) {
        Type listType = new TypeToken<List<LeaderboardEntry>>() {}.getType();
        getJsonList("/leaderboard", listType, cb);
    }

    // ───────────────────────── internals ─────────────────────────

    private static <T> void postJson(String path, Object body, Class<T> responseType, Callback<T> cb) {
        Net.HttpRequest request = new HttpRequestBuilder()
                .newRequest()
                .method(Net.HttpMethods.POST)
                .url(baseUrl + path)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .content(GSON.toJson(body))
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

    @SuppressWarnings("unchecked")
    private static <T> void getJsonList(String path, Type listType, Callback<T> cb) {
        Net.HttpRequest request = new HttpRequestBuilder()
                .newRequest()
                .method(Net.HttpMethods.GET)
                .url(baseUrl + path)
                .header("Accept", "application/json")
                .build();
        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int status = httpResponse.getStatus().getStatusCode();
                String body = httpResponse.getResultAsString();
                if (status >= 200 && status < 300) {
                    T parsed = GSON.fromJson(body, listType);
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

    private static <T> void send(Net.HttpRequest request, Class<T> responseType, Callback<T> cb) {
        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int status = httpResponse.getStatus().getStatusCode();
                String body = httpResponse.getResultAsString();
                if (status >= 200 && status < 300) {
                    T parsed = responseType == Void.class || body == null || body.isEmpty()
                            ? null
                            : GSON.fromJson(body, responseType);
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

    private static BackendException parseError(int status, String body) {
        try {
            ErrorResponse err = GSON.fromJson(body, ErrorResponse.class);
            if (err != null && err.error != null) {
                return new BackendException(status, err.error, err.message != null ? err.message : err.error);
            }
        } catch (Exception ignored) {}
        return new BackendException(status, "HTTP_" + status, body != null ? body : "Request failed");
    }

    private static <T> void postSuccess(Callback<T> cb, T result) {
        Gdx.app.postRunnable(() -> cb.onSuccess(result));
    }

    private static <T> void postError(Callback<T> cb, BackendException ex) {
        Gdx.app.postRunnable(() -> cb.onError(ex));
    }
}
