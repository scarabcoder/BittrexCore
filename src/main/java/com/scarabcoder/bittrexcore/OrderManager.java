package com.scarabcoder.bittrexcore;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kotlin.Pair;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class OrderManager {

    private List<MarketOrder> orders = Collections.synchronizedList(new ArrayList<>());
    private ServiceThread thread = null;
    private Bittrex bittrex;

    public OrderManager(Bittrex bittrex) {
        this.bittrex = bittrex;
    }

    public List<MarketOrder> getOrders() {
        return Collections.unmodifiableList(orders);
    }

    public void startServiceThread() {
        thread = new ServiceThread();
        thread.start();
    }

    public boolean isServiceThreadRunning() {
        return thread != null && thread.isAlive();
    }

    public void stopServiceThread() {
        thread.end = true;
        thread = null;
    }

    public MarketOrder placeOrder(Market market, MarketOrder.Type type, BigDecimal quantity, BigDecimal rate) {
        String endpoint = type == MarketOrder.Type.SELL ? BittrexEndpoint.SELL : BittrexEndpoint.BUY;
        HttpRequestHandler handler = bittrex.getRequestHandler();
        JsonObject result = handler.sendRequest(handler.buildRequest(endpoint, bittrex, new Pair<>("market", market.getName()), new Pair<>("quantity", quantity), new Pair<>("rate", rate))).getAsJsonObject();

        String orderID = result.get("uuid").getAsString();

        JsonObject data = handler.sendRequest(handler.buildRequest(BittrexEndpoint.GET_ORDER, bittrex, new Pair<>("uuid", orderID))).getAsJsonObject();
        MarketOrder order = MarketOrder.fromJson(data, bittrex);
        orders.add(order);
        return order;
    }

    private void updateOrderData(JsonElement result) {
        for(JsonElement element : result.getAsJsonArray()) {
            JsonObject orderData = element.getAsJsonObject();
            MarketOrder order = orders.stream().filter(o -> o.getUuid().equals(UUID.fromString(orderData.get("OrderUuid").getAsString()))).findFirst().orElse(null);
            if(order == null) {
                orders.add(MarketOrder.fromJson(orderData, bittrex));
            } else {
                order.updateCloseStatus(!orderData.get("Closed").isJsonNull());
            }
        }
    }

    private class ServiceThread extends Thread {

        private boolean end = false;

        @Override
        public void run() {

            while(!end) {
                try {

                    HttpRequestHandler handler = bittrex.getRequestHandler();

                    JsonElement element = handler.sendRequest(handler.buildRequest(BittrexEndpoint.GET_OPEN_ORDERS, bittrex));
                    updateOrderData(element);

                    element = handler.sendRequest(handler.buildRequest(BittrexEndpoint.GET_ORDER_HISTORY, bittrex));
                    updateOrderData(element);

                    Thread.sleep(5000);


                } catch(Throwable exception) {
                    exception.printStackTrace();
                }
            }

        }

    }


}
