package com.scarabcoder.bittrexcore.tick;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class MarketTick {

    @NotNull
    public static MarketTick fromJson(JsonObject object, TickInterval tickInterval) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        long time;
        try {
            time = format.parse(object.get("T").getAsString()).getTime();
        } catch (ParseException e) {
            throw new RuntimeException("There was an error parsing a date: " + object.get("T").getAsString());
        }
        return new MarketTick(
                time,
                tickInterval,
                object.get("O").getAsBigDecimal(),
                object.get("C").getAsBigDecimal(),
                object.get("H").getAsBigDecimal(),
                object.get("L").getAsBigDecimal()
        );

    }

    private final long time;

    private final TickInterval tickInterval;

    private final BigDecimal open;
    private final BigDecimal close;
    private final BigDecimal high;
    private final BigDecimal low;

    public MarketTick(Long time, TickInterval interval, BigDecimal open, BigDecimal close, BigDecimal high, BigDecimal low) {
        this.time = time;
        this.tickInterval = interval;
        this.open = open;
        this.close = close;
        this.high = high;
        this.low = low;
    }

    public long getTime() {
        return time;
    }

    public BigDecimal getOpen() {
        return open;
    }

    public BigDecimal getClose() {
        return close;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public TickInterval getTickInterval() {
        return tickInterval;
    }

    @Override
    public String toString() {
        return "MarketTick{time=" + time + ", tickInterval=" + tickInterval + ", open="  + open + ", close=" + close + ", high=" + high + ", low=" + low + "}";
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof MarketTick) {
            MarketTick tickOther = (MarketTick) other;
            return tickOther.getTime() == getTime() && tickOther.getTickInterval() == this.getTickInterval();
        }
        return super.equals(other);
    }
}
