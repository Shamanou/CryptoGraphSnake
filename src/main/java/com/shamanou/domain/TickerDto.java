package com.shamanou.domain;

import java.util.ArrayList;
import java.util.List;

public final class TickerDto {
    private double tickerAsk;
    private double tickerBid;
    private List<ArrayList<Double>> feesRaw;
    private String feesVolumeCurrency;
    private TradePair pair;

    public TickerDto() {}

    public double getTickerAsk() {
        return tickerAsk;
    }

    public void setTickerAsk(double tickerAsk) {
        this.tickerAsk = tickerAsk;
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

    public TradePair getTradePair() {
        return this.pair;
    }

    public void setTradePair(TradePair pair) {
        this.pair = pair;
    }
}
