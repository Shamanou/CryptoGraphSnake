package com.shamanou.config;

import java.util.Map;

public class Configuration {
    private String referenceCurrency;
    private Map<String, CurrencyLimit> currencyLimits;
    private String databaseHost;
    private int genomeSize;
    private String key;
    private String secret;
    private Map<String, CurrencyPairLimit> currencyPairLimits;

    public String getReferenceCurrency() {
        return referenceCurrency;
    }

    public void setReferenceCurrency(String referenceCurrency) {
        this.referenceCurrency = referenceCurrency;
    }

    public Map<String, CurrencyLimit> getCurrencyLimits() {
        return currencyLimits;
    }

    public void setCurrencyLimits(Map<String, CurrencyLimit> currencyLimits) {
        this.currencyLimits = currencyLimits;
    }

    public String getDatabaseHost() {
        return databaseHost;
    }

    public void setDatabaseHost(String databaseHost) {
        this.databaseHost = databaseHost;
    }

    public int getGenomeSize() {
        return genomeSize;
    }

    public void setGenomeSize(int genomeSize) {
        this.genomeSize = genomeSize;
    }

    public Map<String, CurrencyPairLimit> getCurrencyPairLimits() {
        return currencyPairLimits;
    }

    public void setCurrencyPairLimits(Map<String, CurrencyPairLimit> currencyPairLimits) {
        this.currencyPairLimits = currencyPairLimits;
    }

    public String getSecret() {
        return secret;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
