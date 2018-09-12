package com.scarabcoder.bittrexcore;

import com.google.gson.JsonElement;

public interface HttpCallback {

    void onSuccess(JsonElement result);

    void onError(String message);

}
