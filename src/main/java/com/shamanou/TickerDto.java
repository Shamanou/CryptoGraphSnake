package com.shamanou;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;

public final class TickerDto {
    private double tickerAsk;
    private double tickerBid;
    private List<ArrayList<Double>> feesRaw;
    private String feesVolumeCurrency;
    private com.shamanou.TradePair pair;

    public TickerDto() {}

    public double getTickerAsk() {
        return tickerAsk;
    }

    public void setTickerAsk(double tickerAsk) {
        this.tickerAsk = tickerAsk;
    }

    public List<ArrayList<Double>> getFeesRaw() {
        return feesRaw;
    }

    public void setFeesRaw(List<ArrayList<Double>> feesRaw) {
        this.feesRaw = feesRaw;
    }

    public String getFeesVolumeCurrency() {
        return feesVolumeCurrency;
    }

    public void setFeesVolumeCurrency(String string) {
        this.feesVolumeCurrency = string;
    }

    public double getTickerBid() {
        return tickerBid;
    }

    public void setTickerBid(double tickerBid) {
        this.tickerBid = tickerBid;
    }

    public com.shamanou.TradePair getTradePair() {
        return this.pair;
    }

    public void setTradePair(com.shamanou.TradePair pair) {
        this.pair = pair;
    }
}
