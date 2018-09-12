package com.scarabcoder.bittrexcore;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kotlin.Pair;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.UUID;

public class MarketOrder {

    public static MarketOrder fromJson(JsonObject obj, Bittrex bittrex) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        long time;
        try {
            String tStr = obj.has("TimeStamp") ? obj.get("TimeStamp").getAsString() : obj.get("Opened").getAsString();
            time = format.parse(tStr).getTime();
        } catch (ParseException e) {
            throw new RuntimeException("There was an error parsing a date: " + obj.get("Opened").getAsString());
        }
        return new MarketOrder(
                UUID.fromString(obj.get("OrderUuid").getAsString()),
                bittrex.getMarketManager().getMarket(obj.get("Exchange").getAsString()),
                (obj.get("Type") == null ? obj.get("OrderType") : obj.get("Type")).getAsString().endsWith("SELL") ? Type.SELL : Type.BUY,
                obj.get("Quantity").getAsBigDecimal(),
                obj.get("Limit").getAsBigDecimal(),
                !obj.get("Closed").isJsonNull(),
                time,
                bittrex
        );
    }

    private final UUID uuid;
    private final Market market;
    private final Type type;
    private final BigDecimal quantity;
    private final BigDecimal price;
    private boolean closed;
    private long opened;
    private Bittrex bittrex;

    public MarketOrder(UUID uuid, Market market, Type type, BigDecimal quantity, BigDecimal price, boolean closed, long opened, Bittrex bittrex) {
        this.uuid = uuid;
        this.market = market;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
        this.closed = closed;
        this.opened = opened;
        this.bittrex = bittrex;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Market getMarket() {
        return market;
    }

    public Type getType() {
        return type;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isClosed() {
        return closed;
    }

    void updateCloseStatus(boolean status) {
        this.closed = status;
    }

    public long getOpened() {
        return opened;
    }

    public void cancel() {
        if(closed) throw new IllegalStateException("Order already closed!");
        this.closed = true;

        bittrex.getRequestHandler().sendRequest(bittrex.getRequestHandler().buildRequest(BittrexEndpoint.CANCEL_ORDER, bittrex, new Pair<>("uuid", this.getUuid().toString())), new HttpCallback() {
            @Override
            public void onSuccess(JsonElement result) {}
            @Override
            public void onError(String message) {
                closed = false;
                throw new RuntimeException("Error cancelling order: " + message);
            }
        });

    }

    public enum Type {
        BUY, SELL
    }

}
