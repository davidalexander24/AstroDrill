package com.david.astrodrill.network;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
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
 * Async client for the AstroDrill backend. Uses Gdx.net for HTTP and
 * JsonReader/JsonValue for parsing — no reflection, so it compiles for both
 * desktop (LWJGL3) and web (GWT) without gdx.reflect.include registrations.
 * All callbacks fire via Gdx.app.postRunnable so they're safe for screen
 * transitions and stage mutation.
 */
public final class BackendClient {

    // Production backend (Railway). For local dev, point this at http://localhost:8080/api/game.
    public static String baseUrl = "https://astrodrill-backend-production.up.railway.app/api/game";

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(BackendException ex);
    }

    private interface Parser<T> {
        T parse(JsonValue root);
    }

    private BackendClient() {}

    public static void register(RegisterRequest req, Callback<LoginResponse> cb) {
        postJson("/register", writeRegisterRequest(req), BackendClient::parseLoginResponse, cb);
    }

    public static void login(LoginRequest req, Callback<LoginResponse> cb) {
        postJson("/login", writeLoginRequest(req), BackendClient::parseLoginResponse, cb);
    }

    public static void save(SaveRequest req, Callback<SaveResponse> cb) {
        postJson("/save", writeSaveRequest(req), BackendClient::parseSaveResponse, cb);
    }

    public static void load(Long playerId, Callback<LoadResponse> cb) {
        getJson("/load/" + playerId, BackendClient::parseLoadResponse, cb);
    }

    public static void getLeaderboard(Callback<List<LeaderboardEntry>> cb) {
        getJson("/leaderboard", BackendClient::parseLeaderboardList, cb);
    }

    // ───────────────────────── outgoing JSON writers ─────────────────────────

    private static String writeLoginRequest(LoginRequest r) {
        return "{\"username\":\"" + escape(r.username)
                + "\",\"password\":\"" + escape(r.password) + "\"}";
    }

    private static String writeRegisterRequest(RegisterRequest r) {
        return "{\"username\":\"" + escape(r.username)
                + "\",\"password\":\"" + escape(r.password) + "\"}";
    }

    private static String writeSaveRequest(SaveRequest r) {
        StringBuilder sb = new StringBuilder(64 + (r.data == null ? 0 : r.data.length()));
        sb.append('{');
        sb.append("\"playerId\":").append(r.playerId == null ? 0L : r.playerId.longValue()).append(',');
        sb.append("\"data\":\"").append(escape(r.data)).append("\",");
        sb.append("\"credits\":").append(r.credits).append(',');
        sb.append("\"currentPlanet\":\"").append(escape(r.currentPlanet)).append("\",");
        sb.append("\"maxDepthMined\":").append(r.maxDepthMined).append(',');
        sb.append("\"fastestLaunchTime\":").append(r.fastestLaunchTime);
        sb.append('}');
        return sb.toString();
    }

    private static String escape(String s) {
        return JsonUtil.escape(s);
    }

    // ───────────────────────── incoming JSON parsers ─────────────────────────

    private static LoginResponse parseLoginResponse(JsonValue root) {
        LoginResponse r = new LoginResponse();
        if (root == null) return r;
        if (root.has("playerId") && !root.get("playerId").isNull()) {
            r.playerId = root.getLong("playerId");
        }
        r.username = root.getString("username", null);
        return r;
    }

    private static SaveResponse parseSaveResponse(JsonValue root) {
        SaveResponse r = new SaveResponse();
        if (root != null) r.savedAt = root.getString("savedAt", null);
        return r;
    }

    private static LoadResponse parseLoadResponse(JsonValue root) {
        LoadResponse r = new LoadResponse();
        if (root == null) return r;
        if (root.has("playerId") && !root.get("playerId").isNull()) {
            r.playerId = root.getLong("playerId");
        }
        r.username = root.getString("username", null);
        r.data = root.getString("data", null);
        r.updatedAt = root.getString("updatedAt", null);
        return r;
    }

    private static LeaderboardEntry parseLeaderboardEntry(JsonValue node) {
        LeaderboardEntry e = new LeaderboardEntry();
        e.username = node.getString("username", null);
        e.maxDepthMined = node.getInt("maxDepthMined", 0);
        e.fastestLaunchTime = node.getLong("fastestLaunchTime", 0L);
        return e;
    }

    private static List<LeaderboardEntry> parseLeaderboardList(JsonValue root) {
        List<LeaderboardEntry> out = new ArrayList<>();
        if (root == null) return out;
        for (JsonValue child = root.child; child != null; child = child.next) {
            out.add(parseLeaderboardEntry(child));
        }
        return out;
    }

    private static BackendException parseError(int status, String body) {
        try {
            if (body != null && !body.isEmpty()) {
                JsonValue root = new JsonReader().parse(body);
                if (root != null) {
                    String err = root.getString("error", null);
                    if (err != null) {
                        String msg = root.getString("message", err);
                        return new BackendException(status, err, msg);
                    }
                }
            }
        } catch (Exception ignored) {}
        return new BackendException(status, "HTTP_" + status, body != null ? body : "Request failed");
    }

    // ───────────────────────── HTTP plumbing ─────────────────────────

    // libGDX HttpRequestBuilder defaults to a 1-second timeout, which Railway's
    // cold-start latency routinely exceeds. Give every request a much longer cap.
    private static final int HTTP_TIMEOUT_MS = 15000;

    private static <T> void postJson(String path, String payload, Parser<T> parser, Callback<T> cb) {
        Net.HttpRequest request = new HttpRequestBuilder()
                .newRequest()
                .method(Net.HttpMethods.POST)
                .url(baseUrl + path)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(HTTP_TIMEOUT_MS)
                .content(payload)
                .build();
        send(request, parser, cb);
    }

    private static <T> void getJson(String path, Parser<T> parser, Callback<T> cb) {
        Net.HttpRequest request = new HttpRequestBuilder()
                .newRequest()
                .method(Net.HttpMethods.GET)
                .url(baseUrl + path)
                .header("Accept", "application/json")
                .timeout(HTTP_TIMEOUT_MS)
                .build();
        send(request, parser, cb);
    }

    private static <T> void send(Net.HttpRequest request, final Parser<T> parser, final Callback<T> cb) {
        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int status = httpResponse.getStatus().getStatusCode();
                String body = httpResponse.getResultAsString();
                if (status >= 200 && status < 300) {
                    T parsed;
                    if (body == null || body.isEmpty()) {
                        parsed = parser.parse(null);
                    } else {
                        JsonValue root = new JsonReader().parse(body);
                        parsed = parser.parse(root);
                    }
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

    private static <T> void postSuccess(final Callback<T> cb, final T result) {
        Gdx.app.postRunnable(new Runnable() { @Override public void run() { cb.onSuccess(result); } });
    }

    private static <T> void postError(final Callback<T> cb, final BackendException ex) {
        Gdx.app.postRunnable(new Runnable() { @Override public void run() { cb.onError(ex); } });
    }
}
