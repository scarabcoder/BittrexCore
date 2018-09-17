package com.scarabcoder.bittrexcore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Bittrex {

    public static final Logger LOGGER = LoggerFactory.getLogger("BittrexCore");
    private final String key;
    private final String secret;
    private final HttpRequestHandler requestHandler;
    private final OrderManager orderManager = new OrderManager(this);
    private final MarketManager marketManager = new MarketManager(this);
    private double bitcoinPrice;
    private List<String> baseMarkets;
    private Map<String, BigDecimal> balances;


    public Bittrex(String key, String secret, long requestDelay, String... baseMarkets) {
        this.key = key;
        this.secret = secret;
        requestHandler = new HttpRequestHandler(requestDelay);
        requestHandler.startProccessingThread();
        marketManager.startServiceThread();
        this.baseMarkets = Arrays.asList(baseMarkets);
    }

    public boolean testAuth() {
        try {
            requestHandler.sendRequest(requestHandler.buildRequest(BittrexEndpoint.GET_OPEN_ORDERS, this));
            return true;
        } catch(RuntimeException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<String> getBaseMarkets() {
        return baseMarkets;
    }

    public String getKey() {
        return key;
    }

    public String getSecret() {
        return secret;
    }

    public HttpRequestHandler getRequestHandler() {
        return requestHandler;
    }


    public OrderManager getOrderManager() {
        return orderManager;
    }

    public MarketManager getMarketManager() {
        return marketManager;
    }

    public double getBitcoinPrice() {
        return bitcoinPrice;
    }

    void updateBitcoinPrice(double bitcoinPrice) {
        this.bitcoinPrice = bitcoinPrice;
    }

    public BigDecimal getBitcoinBalance() {
        return getBalance("BTC");
    }

    public BigDecimal getBalance(String coin) {
        return balances.getOrDefault(coin, new BigDecimal("0.0"));
    }

    public Map<String, BigDecimal> getBalances() {
        return balances;
    }

    void updateBalance(String coin, BigDecimal amount) {
        balances.put(coin, amount);
    }
}