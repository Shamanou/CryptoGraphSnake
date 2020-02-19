package com.shamanou;

import org.apache.commons.lang3.builder.CompareToBuilder;
import java.util.Comparator;

public class WalletComparator implements Comparator<Value> {

    @Override
    public int compare(Value o1, Value o2) {
        return new CompareToBuilder()
                .append(o2.getValueConverted().doubleValue(), o1.getValueConverted().doubleValue())
                .append(o2.getValue().doubleValue(), o1.getValue().doubleValue())
                .toComparison();
    }
}