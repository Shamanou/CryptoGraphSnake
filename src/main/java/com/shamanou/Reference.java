package com.shamanou;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import org.bson.conversions.Bson;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

public class Reference {
    private String reference;
    private String referenceOf;
    private BigDecimal volume;
    private MongoCollection<TickerDto> table;

    public Reference(MongoCollection<TickerDto> table) {
        this.table = table;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public void setReferenceOf(String referenceOf) {
        this.referenceOf = referenceOf;
    }

    public BigDecimal getConvertedValue() {
        Bson filter = Filters.or(
                Filters.and(
                        Filters.regex("tradePair.base", this.reference), Filters.regex("tradePair.quote", this.referenceOf)),
                Filters.and(
                        Filters.regex("tradePair.base", this.referenceOf), Filters.regex("tradePair.quote", this.reference)));
        if (this.table.find(filter, TickerDto.class).into(new ArrayList<>()).size() > 0) {
            TickerDto result = this.table.find(filter, TickerDto.class).into(new ArrayList<>()).get(0);
            BigDecimal factor;
            if (result.getTradePair().getBase().equals(reference)) {
                factor = BigDecimal.valueOf(result.getTickerBid());
                return this.volume.multiply(factor);
            } else if (result.getTradePair().getQuote().equals(reference)) {
                factor = BigDecimal.valueOf(result.getTickerAsk());
                return this.volume.divide(factor, RoundingMode.FLOOR);
            }
        }
        return BigDecimal.ZERO;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }
}
