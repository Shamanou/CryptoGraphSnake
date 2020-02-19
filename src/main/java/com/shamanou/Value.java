package com.shamanou;

import org.knowm.xchange.currency.Currency;

import java.math.BigDecimal;

public class Value {
    private Currency currency;
    private BigDecimal value;
    private BigDecimal valueConverted;

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public BigDecimal getValueConverted() {
        return valueConverted;
    }

    public void setValueConverted(BigDecimal valueConverted) {
        this.valueConverted = valueConverted;
    }
}
