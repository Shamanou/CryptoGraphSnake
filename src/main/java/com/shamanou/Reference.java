package com.shamanou;

import java.util.ArrayList;
import org.apache.commons.math3.fraction.BigFraction;
import org.bson.conversions.Bson;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

public class Reference {
    private String reference;
    private String referenceOf;
    private BigFraction volume;
    private MongoCollection<TickerDto> table;

    public Reference(MongoCollection<TickerDto> table) {
        this.table = table;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getReferenceOf() {
        return referenceOf;
    }

    public void setReferenceOf(String referenceOf) {
        this.referenceOf = referenceOf;
    }

    public BigFraction getConvertedValue() {
        Bson filter = Filters.or(
                Filters.and(
                        Filters.eq("pair.base", this.reference), Filters.eq("pair.quote", this.referenceOf)),
                Filters.and(
                        Filters.eq("pair.base", this.referenceOf), Filters.eq("pair.quote", this.reference)));
        Bson sort = Sorts.descending("ask");
        if (this.table.find(filter, TickerDto.class).sort(sort).into(new ArrayList<TickerDto>()).size() > 0) {
            TickerDto result = this.table.find(filter, TickerDto.class).sort(sort).into(new ArrayList<TickerDto>()).get(0);
            if (result.getTradePair().getBase().equals(reference)) {
                BigFraction ask = new BigFraction(result.getTickerAsk());
                return this.volume.divide(ask);
            } else if (result.getTradePair().getQuote().equals(reference)) {
                BigFraction bid = new BigFraction(result.getTickerBid());
                return this.volume.multiply(bid);
            }
        }
        return new BigFraction(0.0);
    }

    public BigFraction getVolume() {
        return volume;
    }

    public void setVolume(BigFraction bigFraction) {
        this.volume = bigFraction;
    }
}
