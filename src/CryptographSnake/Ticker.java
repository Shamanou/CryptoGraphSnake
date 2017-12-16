package CryptographSnake;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;

public final class Ticker {
    private double tickerAsk;
    private double tickerBid;
    private List<ArrayList<Double>> feesRaw;
    private String feesVolumeCurrency;
    private TradePair pair;


    public Ticker() {}

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

    public void setFeesRaw(JSONArray feesRaw) {
        ArrayList<ArrayList<Double>> out = new ArrayList<ArrayList<Double>>();
        for (int i = 0; i < feesRaw.length(); i++) {
            ArrayList<Double> tmp = new ArrayList<Double>();
            tmp.add(feesRaw.getJSONArray(i).getDouble(1));
            tmp.add(feesRaw.getJSONArray(i).getDouble(0));
            out.add(tmp);
        }
        this.feesRaw = out;
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

    @Override
    public String toString() {
        return this.pair.getBase() + this.pair.getQuote() + " ask:" + String.valueOf(this.tickerAsk) + " bid:" + String.valueOf(this.tickerBid);
    }
}
