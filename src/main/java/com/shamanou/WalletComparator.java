package com.shamanou;

import org.apache.commons.lang3.builder.CompareToBuilder;

import java.math.BigDecimal;
import java.util.Comparator;

public class WalletComparator implements Comparator<Value> {

    @Override
    public int compare(Value o1, Value o2) {
        return new CompareToBuilder()
                .append(o1.getValue(), o2.getValue())
                .append(o1.getValueConverted().orElse(new BigDecimal("-1.0")),
                        o2.getValueConverted().orElse(new BigDecimal("-1.0")))
                .toComparison();
    }
}