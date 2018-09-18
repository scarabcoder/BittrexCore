package com.scarabcoder.bittrexcore;

import com.github.kittinunf.fuel.Fuel;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.scarabcoder.bittrexcore.tick.MarketTick;
import com.scarabcoder.bittrexcore.tick.MarketTickList;
import com.scarabcoder.bittrexcore.tick.TickInterval;
import kotlin.Pair;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MarketManager {

    private List<Market> markets = Collections.synchronizedList(new ArrayList<>());
    private ServiceThread service;
    private Bittrex bittrex;
    private float percentLoaded = 0.0f;
    private BigDecimal minimumVolume = new BigDecimal("10.0");

    public MarketManager(Bittrex bittrex) {
        this.bittrex = bittrex;
    }

    public void setMinimumVolume(BigDecimal minimumVolume) {
        this.minimumVolume = minimumVolume;
    }

    public void startServiceThread() {
        if(service != null && service.isAlive()) throw new IllegalStateException("Service thread already running!");
        service = new ServiceThread();
        service.start();
    }

    public void stopServiceThread() {
        if(service == null) throw new IllegalStateException("Service thread is not running!");
        service.stop = true;
        service = null;
    }

    public Market getMarket(String name) {
        return markets.stream().filter(m -> m.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public List<Market> getMarkets() {
        return Collections.unmodifiableList(markets);
    }

    public float getInitialDataProgress() {
        return percentLoaded;
    }

    private class ServiceThread extends Thread {

        private boolean stop = false;

        @Override
        public void run() {
            while(!stop) {
                try {
                    HttpRequestHandler handler = bittrex.getRequestHandler();
                    JsonObject btcPriceObj = handler.sendRequest(Fuel.get("https://bittrex.com/api/v2.0/pub/currencies/GetBTCPrice")).getAsJsonObject();
                    bittrex.updateBitcoinPrice(btcPriceObj.get("bpi").getAsJsonObject().get("USD").getAsJsonObject().get("rate_float").getAsDouble());

                    JsonArray marketsArray = handler.sendRequest(Fuel.get(BittrexEndpoint.API_URL + BittrexEndpoint.GET_MARKET_SUMMARIES)).getAsJsonArray();
                    JsonArray balances = handler.sendRequest(handler.buildRequest(BittrexEndpoint.GET_BALANCES, bittrex)).getAsJsonArray();
                    float totalAmount = 0f;
                    for(JsonElement e : marketsArray) {
                        JsonObject o = e.getAsJsonObject();
                        if(!bittrex.getBaseMarkets().contains(o.get("MarketName").getAsString().split("-")[0])) continue;
                        if(o.get("BaseVolume").getAsBigDecimal().compareTo(new BigDecimal("10.0")) < 0) continue;
                        totalAmount++;
                    }

                    float x = 0f;


                    for(JsonElement balanceElement : balances) {
                        JsonObject obj = balanceElement.getAsJsonObject();
                        String currency = obj.get("Currency").getAsString();
                        bittrex.updateBalance(currency, obj.get("Balance").getAsBigDecimal());
                    }

                    for(JsonElement mElement : marketsArray) {
                        JsonObject marketData = mElement.getAsJsonObject();
                        if(!bittrex.getBaseMarkets().contains(marketData.get("MarketName").getAsString().split("-")[0])) continue;
                        if(marketData.get("BaseVolume").getAsBigDecimal().compareTo(minimumVolume) < 0 || !(bittrex.getBalance(marketData.get("MarketName").getAsString().split("-")[1]).compareTo(new BigDecimal("0.0")) > 0)) continue;
                        Market market = getMarket(marketData.get("MarketName").getAsString());
                        if(market == null) {
                            market = Market.fromJson(marketData, bittrex);
                            markets.add(market);
                        }else {
                            market.updatePrice(marketData.get("Last").getAsBigDecimal());
                            market.updateVolume(marketData.get("BaseVolume").getAsBigDecimal());
                        }

                        JsonArray ticks = handler.sendRequest(Fuel.get("https://bittrex.com/Api/v2.0/pub/market/GetTicks", Arrays.asList(new Pair<>("marketName", market.getName()), new Pair<>("tickInterval", "oneMin")))).getAsJsonArray();
                        MarketTickList list = new MarketTickList(TickInterval.ONE_MIN);
                        for(JsonElement tickElement : ticks) {
                            JsonObject tickObj = tickElement.getAsJsonObject();
                            MarketTick tick = MarketTick.fromJson(tickObj, TickInterval.ONE_MIN);
                            list.add(tick);
                        }
                        market.updateMarketTicks(list);
                        if(percentLoaded != 1.0) {
                            x++;
                            percentLoaded = x / totalAmount;
                        }
                    }

                    if(x > 0) {
                        Bittrex.LOGGER.info("Loaded " + markets.size() + " markets, with " + new DecimalFormat("#,###").format(markets.stream().mapToInt(market -> market.getTicks().size()).sum()) + " data points.");
                    }
                    percentLoaded = 1.0f;

                } catch (Throwable e) {
                    e.printStackTrace();
                }

            }

        }
    }

}