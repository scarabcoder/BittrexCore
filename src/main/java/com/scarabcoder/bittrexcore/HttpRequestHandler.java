package com.scarabcoder.bittrexcore;


import com.github.kittinunf.fuel.Fuel;
import com.github.kittinunf.fuel.core.FuelError;
import com.github.kittinunf.fuel.core.Request;
import com.github.kittinunf.result.Result;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kotlin.Pair;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class HttpRequestHandler {

    private static final JsonParser PARSER = new JsonParser();

    private final List<HttpRequest> queue = Collections.synchronizedList(new ArrayList<>());
    private ProcessingThread thread = null;
    private ArrayList<Long> averageResponseTime = new ArrayList<>();
    private long requestDelay;

    public HttpRequestHandler(long requestDelay) {
        this.requestDelay = requestDelay;
    }

    @SafeVarargs
    public final Request buildRequest(String endpoint, Bittrex auth, Pair<String, Object>... params) {

        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec spec = new SecretKeySpec(auth.getSecret().getBytes(), mac.getAlgorithm());
            mac.init(spec);

            List<Pair<String, Object>> p = new ArrayList<>(Arrays.asList(params));
            p.add(new Pair<>("apikey", auth.getKey()));
            p.add(new Pair<>("nonce", 1));

            Request request = Fuel.get(BittrexEndpoint.API_URL + endpoint, p);
            HashMap<String, Object> headers = new HashMap<>();
            byte[] apisign = mac.doFinal(request.getUrl().toString().getBytes());
            StringBuilder builder = new StringBuilder();
            for(byte b : apisign) {
                builder.append(String.format("%02x", b));
            }
            String apisignStr = builder.toString();
            headers.put("apisign", apisignStr);
            request = request.header(headers);

            Fuel.get(BittrexEndpoint.API_URL + endpoint, Collections.singletonList(new Pair<>("apisign", mac.doFinal())));
            return request;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e.getMessage());
        }

    }

    public void startProccessingThread() {
        if(thread != null)
            throw new IllegalStateException("Processing thread already working!");
        thread = new ProcessingThread();
        thread.start();
    }

    public void sendRequest(Request request, HttpCallback callback) {

        queue.add(new HttpRequest(request, callback));

    }

    public JsonElement sendRequest(Request request) {
        final JsonElement[] element = {null};
        final boolean[] finished = {false};
        final String[] error = {null};
        sendRequest(request, new HttpCallback() {
            @Override
            public void onSuccess(JsonElement result) {
                element[0] = result;
                finished[0] = true;
            }

            @Override
            public void onError(String message) {
                finished[0] = true;
                error[0] = message;
            }
        });
        while(true) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(finished[0]) break;
        }

        if(error[0] != null) throw new RuntimeException(error[0]);
        return element[0];
    }

    public int getQueueSize() {
        return queue.size();
    }

    public void stopThread() {
        thread.stop = true;
    }

    public long getAverageReponseTime() {
        long sum = 0;
        for(long t : averageResponseTime) sum += t;
        return sum / averageResponseTime.size();
    }


    @SuppressWarnings("InfiniteLoopStatement")
    private class ProcessingThread extends Thread {

        private boolean stop = false;

        @Override
        public void run() {
            while(true) {
                if(stop)
                    return;

                if(queue.isEmpty())
                    continue;

                HttpRequest request = queue.get(0);

                try {
                    Long start = System.currentTimeMillis();
                    Request req = request.request;
                    HashMap<String, Object> headers = new HashMap<>(req.getHeaders());
                    headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36");
                    req = req.header(headers);
                    Result<String, FuelError> response = req.responseString().getThird();
                    JsonObject responseJson = PARSER.parse(response.get()).getAsJsonObject();
                    averageResponseTime.add(System.currentTimeMillis() - start);
                    if(averageResponseTime.size() > 100) averageResponseTime.remove(0);
                    if(!responseJson.get("success").getAsBoolean()) {
                        request.callback.onError(responseJson.get("message").getAsString());
                    } else {
                        request.callback.onSuccess(responseJson.get("result"));
                    }
                } catch(Exception e) {
                    Bittrex.LOGGER.warn("An exception occurred when requesting data from Bittrex!");
                    e.printStackTrace();
                    request.callback.onError(e.getMessage());
                }
                queue.remove(0);
                try {
                    Thread.sleep(requestDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        }
    }

    private class HttpRequest {

        private Request request;
        private HttpCallback callback;

        public HttpRequest(Request request, HttpCallback callback) {
            this.request = request;
            this.callback = callback;
        }
    }





}
