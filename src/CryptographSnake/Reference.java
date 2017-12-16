package CryptographSnake;

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
    private MongoCollection<Ticker> table;

    public Reference(MongoCollection<Ticker> table) {
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
        if (this.table.find(filter, Ticker.class).sort(sort).into(new ArrayList<Ticker>()).size() > 0) {
            Ticker result = this.table.find(filter, Ticker.class).sort(sort).into(new ArrayList<Ticker>()).get(0);
            if (reference.equals(result.getTradePair().getBase())) {
                BigFraction ask = new BigFraction(result.getTickerAsk());
                return this.volume.divide(ask);
            } else if (reference.equals(result.getTradePair().getQuote())) {
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
