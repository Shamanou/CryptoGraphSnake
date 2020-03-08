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
        if (this.reference.equals(this.referenceOf)){
            return this.volume;
        }
        Bson filterBase =
                Filters.and(Filters.eq("tradePair.base", this.reference), Filters.eq("tradePair.quote", this.referenceOf));
        Bson filterQuote =
                Filters.and(Filters.eq("tradePair.base", this.referenceOf), Filters.eq("tradePair.quote", this.reference));
        BigDecimal factor;
        if (this.table.find(filterBase, TickerDto.class).into(new ArrayList<>()).size() > 0) {
            TickerDto result = this.table.find(filterBase, TickerDto.class).into(new ArrayList<>()).get(0);
            factor = BigDecimal.valueOf(result.getTickerAsk());
            return this.volume.divide(factor, RoundingMode.FLOOR);
        } else if (this.table.find(filterQuote, TickerDto.class).into(new ArrayList<>()).size() > 0) {
            TickerDto result = this.table.find(filterQuote, TickerDto.class).into(new ArrayList<>()).get(0);
            factor = BigDecimal.valueOf(result.getTickerBid());
            return this.volume.multiply(factor);
        } else {
            return BigDecimal.ZERO;
        }
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }
}
