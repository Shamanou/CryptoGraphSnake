package CryptographSnake;

import java.util.ArrayList;

import org.apache.commons.math3.fraction.BigFraction;
import org.bson.conversions.Bson;
<<<<<<< HEAD
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.dto.marketdata.Ticker;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

public class Reference {
	private Currency reference;
	private Currency referenceOf;
	private BigFraction volume;
	private MongoCollection<Ticker> table;

	public Reference( MongoCollection<Ticker> table) {
		this.table = table;
	}
	
	public Currency getReference() {
		return reference;
	}
	public void setReference(Currency reference) {
		this.reference = reference;
	}
	public Currency getReferenceOf() {
		return referenceOf;
	}
	public void setReferenceOf(Currency referenceOf) {
		this.referenceOf = referenceOf;
	}
	
	public BigFraction getConvertedValue() throws Exception {	
		Bson filter = Filters.or(
				Filters.and(
						Filters.eq("pair.base", this.reference), Filters.eq("pair.quote", this.referenceOf)
				), Filters.and(
						Filters.eq("pair.base", this.referenceOf), Filters.eq("pair.quote", this.reference)
				)
			);
		Bson sort = Sorts.descending("ask");
		if (this.table.find(filter, Ticker.class).sort(sort).into(new ArrayList<Ticker>()).size() > 0) {
			Ticker result = this.table.find(filter, Ticker.class).sort(sort).into(new ArrayList<Ticker>()).get(0);
    		if (reference.equals(result.getCurrencyPair().base)){
    			return this.volume.divide(new BigFraction(result.getAsk().doubleValue()));
    		} else if (reference.equals(result.getCurrencyPair().counter)){
    			return this.volume.divide(new BigFraction(1).divide(new BigFraction(result.getBid().doubleValue())));
    		}
		}
		throw new Exception("Cannot convert these currencies! " + "[" + this.referenceOf + " - " + this.reference + "]");
=======

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

public class Reference {
	private String reference;
	private String referenceOf;
	private BigFraction volume;
	private MongoCollection<Ticker> table;

	public Reference( MongoCollection<Ticker> table) {
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
						Filters.eq("pair.base", this.reference), Filters.eq("pair.quote", this.referenceOf)
				), Filters.and(
						Filters.eq("pair.base", this.referenceOf), Filters.eq("pair.quote", this.reference)
				)
			);
		Bson sort = Sorts.descending("ask");
		if (this.table.find(filter, Ticker.class).sort(sort).into(new ArrayList<Ticker>()).size() > 0) {
			Ticker result = this.table.find(filter, Ticker.class).sort(sort).into(new ArrayList<Ticker>()).get(0);
//    		if (reference.equals(result.getTradePair().getBase())){
    			return this.volume.multiply(new BigFraction(1).divide(new BigFraction(result.getTickerAsk())));
//    		} else if (reference.equals(result.getTradePair().getQuote())){
//    			return this.volume.multiply(new BigFraction(1).divide(new BigFraction(1).divide(new BigFraction(result.getTickerBid()))));
//    		}
		}
		return new BigFraction(0.0);
	}

	public BigFraction getVolume() {
		return volume;
	}
	public void setVolume(BigFraction bigFraction) {
		this.volume  = bigFraction;
	}
}