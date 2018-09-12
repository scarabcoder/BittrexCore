package com.scarabcoder.bittrexcore.tick;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MarketTickList extends ArrayList<MarketTick> {

    private static MarketTick mergeTicks(List<MarketTick> ticks, TickInterval newInterval) {
        long time;
        BigDecimal open;
        BigDecimal close;
        BigDecimal high;
        BigDecimal low;

        ticks.sort(new SortByDate());

        time = ticks.get(0).getTime();
        open = ticks.get(0).getOpen();
        close = ticks.get(ticks.size() - 1).getClose();
        ticks.sort(Comparator.comparing(MarketTick::getHigh));
        low = ticks.get(0).getLow();
        high = ticks.get(ticks.size() - 1).getHigh();
        return new MarketTick(time, newInterval, open, close, high, low);
    }

    private final TickInterval tickInterval;

    public MarketTickList(TickInterval interval) {

        this.tickInterval = interval;

    }


    public MarketTickList copy() {
        MarketTickList list = new MarketTickList(this.getTickInterval());
        list.addAll(this);
        return list;
    }


    public MarketTickList limit(long time, long timeBefore) {
        MarketTickList list = new MarketTickList(this.getTickInterval());

        list.addAll(this.stream().filter(tick -> tick.getTime() > time - timeBefore && tick.getTime() <= time).collect(Collectors.toList()));
        return list;
    }

    public BigDecimal getVolatility() {
        if(size() < 2) throw new IllegalStateException("There must be at least two market ticks to call this function!");

        int x = this.size() - 1;
        int amount = this.size();
        BigDecimal total = new BigDecimal("0.0");
        while(x != 0) {
            MarketTick t1 = this.get(x);
            MarketTick t2 = this.get(x - 1);
            total = total.add(
                    (t1.getHigh().subtract(t1.getLow())).max(t2.getClose().subtract(t1.getHigh()).abs())
                            .max(t2.getClose().subtract(t1.getLow()).abs())
            );
            x--;
        }
        return total.setScale(15, RoundingMode.HALF_UP).divide(new BigDecimal(amount), RoundingMode.HALF_UP);
    }

    public BigDecimal getHigh() {
        return this.copy().stream().max(Comparator.comparing(MarketTick::getHigh)).orElse(null).getHigh();
    }

    public BigDecimal getLow() {
        return this.copy().stream().min(Comparator.comparing(MarketTick::getLow)).orElse(null).getLow();
    }


    public TickInterval getTickInterval() {
        return tickInterval;
    }

    public MarketTickList asTickInterval(TickInterval interval) {
        List<TickInterval> ticks = Collections.singletonList(interval);

        if(ticks.indexOf(interval) < ticks.indexOf(tickInterval)) throw new IllegalArgumentException("Cannot create smaller tick types from larger! (" + getTickInterval() + " to " + interval + ")");

        MarketTickList list = new MarketTickList(interval);
        long current = get(this.size() - 1).getTime();

        while(true) {
            List<MarketTick> group = limit(current, interval.timeLength);
            if(group.isEmpty()) break;
            list.add(mergeTicks(group, interval));
            current -= interval.timeLength;
        }

        return list;

    }




    private static class SortByDate implements Comparator<MarketTick> {
        @Override
        public int compare(MarketTick t1, MarketTick t2) {
            return (int) (t2.getTime() - t1.getTime());
        }
    }

}
