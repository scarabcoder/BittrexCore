package com.scarabcoder.bittrexcore;

public class BittrexEndpoint {

    public static final String API_URL = "https://bittrex.com/api/v1.1";

    /**
     * Used to get the open and available trading markets at Bittrex along with other meta data.
     * Parameters: none
     */
    public static final String GET_MARKETS = "/public/getmarkets";
    /**
     * Used to get all supported currencies at Bittrex along with other meta data.
     * Parameters: none
     */
    public static final String GET_CURRENCIES = "/public/getcurrencies";

    /**
     * Used to get the current tick values for a market.
     * Parameters: market
     */
    public static final String GET_TICKER = "/public/getticker";

    /**
     * Used to get the last 24 hour summary of all active markets.
     * Parameters: none
     */
    public static final String GET_MARKET_SUMMARIES = "/public/getmarketsummaries";
    public static final String GET_ORDER_BOOK = "/public/getorderbook";
    public static final String GET_MARKET_HISTORY = "/public/getmarkethistory";

    public static final String BUY = "/market/buylimit";
    public static final String SELL = "/market/selllimit";
    public static final String CANCEL_ORDER = "/market/cancel";

    /**
     * Get all orders that you currently have opened. A specific market can be requested.
     * Parameters: market (optional)
     */
    public static final String GET_OPEN_ORDERS = "/market/getopenorders";

    public static final String GET_BALANCES = "/account/getbalances";
    public static final String GET_BALANCE = "/account/getbalance";
    public static final String GET_DEPOSIT_ADDRESS = "/account/getdepositaddress";
    public static final String WITHDRAW = "/account/withdraw";
    public static final String GET_ORDER = "/account/getorder";

    /**
     * Used to retrieve your order history. Does not include open orders.
     */
    public static final String GET_ORDER_HISTORY = "/account/getorderhistory";
    public static final String GET_WITHDRAWAL_HISTORY = "/account/getwithdrawalhistory";
    public static final String GET_DEPOSIT_HISTORY = "/account/getdeposithistory";




}
