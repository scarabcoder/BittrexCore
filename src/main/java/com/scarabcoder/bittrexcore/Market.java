package com.scarabcoder.bittrexcore;

import com.google.gson.JsonObject;
import com.scarabcoder.bittrexcore.tick.MarketTick;
import com.scarabcoder.bittrexcore.tick.MarketTickList;
import com.scarabcoder.bittrexcore.tick.TickInterval;

import java.math.BigDecimal;

public class Market {

    public static Market fromJson(JsonObject json, Bittrex bittrex) {

        return new Market(json.get("MarketName").getAsString(), json.get("Last").getAsBigDecimal(), json.get("BaseVolume").getAsBigDecimal(), bittrex);

    }

    private final String name;
    private MarketTickList marketTicks = new MarketTickList(TickInterval.ONE_MIN);
    private BigDecimal volume;
    private BigDecimal actualBalance = new BigDecimal("0.0");
    private BigDecimal price;
    private Bittrex bittrex;

    public Market(String name, BigDecimal lastPrice, BigDecimal volume, Bittrex bittrex) {
        this.name = name;
        this.price = lastPrice;
        this.volume = volume;
        this.bittrex = bittrex;
    }

    public void updateMarketTicks(MarketTickList ticks) {
        if(ticks.getTickInterval() != TickInterval.ONE_MIN) throw new IllegalArgumentException("Update tick list must be of type TickInterval.ONE_MIN!");
        for(MarketTick tick : ticks) {
            if(!marketTicks.contains(tick)) marketTicks.add(tick);
        }
    }


    public String getName() {
        return name;
    }

    public String getCoin() {
        return name.split("-")[1];
    }

    public String getBaseMarket() {
        return name.split("-")[0];
    }

    public MarketTickList getTicks() {
        return marketTicks;
    }

    public MarketTickList getTicks(TickInterval tickInterval) {
        return marketTicks.asTickInterval(tickInterval);
    }

    public BigDecimal getPrice() {
        return price;
    }

    void updatePrice(BigDecimal price) {
        this.price = price;
    }

    void updateVolume(BigDecimal volume) {
        this.volume = volume;
    }

    void updateBalance(BigDecimal balance) {
        this.actualBalance = balance;
    }

    public BigDecimal getActualBalance() {
        return this.actualBalance == null ? new BigDecimal("0.0") : actualBalance;
    }

    public BigDecimal getBalance() {
        return getActualBalance().add(getPending());
    }

    public BigDecimal getPending() {
        return bittrex.getOrderManager().getOrders().stream().filter(order -> order.getMarket() != null && order.getMarket().equals(this) && !order.isClosed()).map(MarketOrder::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof Market) {
            return ((Market)other).getName().equals(this.getName());
        }
        return super.equals(other);
    }


    public BigDecimal getVolume() {
        return volume;
    }
}
