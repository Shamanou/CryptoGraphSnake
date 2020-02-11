package com.shamanou;

import java.math.BigDecimal;
import java.util.ArrayList;
import org.apache.commons.math3.fraction.BigFraction;
import org.bson.conversions.Bson;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

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
                        Filters.eq("pair.base", this.reference), Filters.eq("pair.quote", this.referenceOf)),
                Filters.and(
                        Filters.eq("pair.base", this.referenceOf), Filters.eq("pair.quote", this.reference)));
        Bson sort = Sorts.descending("ask");
        if (this.table.find(filter, TickerDto.class).sort(sort).into(new ArrayList<>()).size() > 0) {
            TickerDto result = this.table.find(filter, TickerDto.class).sort(sort).into(new ArrayList<>()).get(0);
            if (result.getTradePair().getBase().equals(reference)) {
                BigFraction ask = new BigFraction(result.getTickerAsk());
                return this.volume.divide(ask.bigDecimalValue());
            } else if (result.getTradePair().getQuote().equals(reference)) {
                BigFraction bid = new BigFraction(result.getTickerBid());
                return this.volume.multiply(bid.bigDecimalValue());
            }
        }
        throw new IllegalArgumentException("invalid conversion");
    }

    public void setVolume(BigDecimal bigFraction) {
        this.volume = bigFraction;
    }
}
