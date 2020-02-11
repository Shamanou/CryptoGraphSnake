package com.shamanou;

import org.knowm.xchange.currency.Currency;

import java.math.BigDecimal;
import java.util.Optional;

public class Value {
    private Currency currency;
    private BigDecimal value;
    private Optional<BigDecimal> valueConverted;

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

    public Optional<BigDecimal> getValueConverted() {
        return valueConverted;
    }

    public void setValueConverted(BigDecimal valueConverted) {
        if (valueConverted == null || valueConverted.equals(BigDecimal.valueOf(0))){
            this.valueConverted = Optional.empty();
        } else {
            this.valueConverted = Optional.of(valueConverted);
        }
    }
}
