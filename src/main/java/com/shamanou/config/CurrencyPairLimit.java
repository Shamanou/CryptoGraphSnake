package com.shamanou.config;

public class CurrencyPairLimit {
    private int pricePrecision;
    private int volumePrecision;
    private int minimumVolume;

    public int getPricePrecision() {
        return pricePrecision;
    }

    public void setPricePrecision(int pricePrecision) {
        this.pricePrecision = pricePrecision;
    }

    public int getVolumePrecision() {
        return volumePrecision;
    }

    public void setVolumePrecision(int volumePrecision) {
        this.volumePrecision = volumePrecision;
    }

    public int getMinimumVolume() {
        return minimumVolume;
    }

    public void setMinimumVolume(int minimumVolume) {
        this.minimumVolume = minimumVolume;
    }
}

