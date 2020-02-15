package com.shamanou;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.knowm.xchange.kraken.dto.marketdata.KrakenFee;

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
