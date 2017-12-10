package CryptographSnake;

import static org.jenetics.engine.EvolutionResult.toBestPhenotype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.function.Consumer;
import java.math.BigDecimal;

import org.apache.commons.math3.fraction.BigFraction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.conversions.Bson;
import org.jenetics.AnyChromosome;
import org.jenetics.AnyGene;
import org.jenetics.Genotype;
import org.jenetics.Optimize;
import org.jenetics.Phenotype;
import org.jenetics.RouletteWheelSelector;
import org.jenetics.SinglePointCrossover;
import org.jenetics.TournamentSelector;
import org.jenetics.engine.Engine;
import org.jenetics.engine.EvolutionResult;
import org.jenetics.engine.EvolutionStatistics;
import org.jenetics.engine.limit;
import org.knowm.xchange.currency.Currency;
<<<<<<< HEAD
import org.knowm.xchange.dto.marketdata.Ticker;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

public class Evolve {
	private static BigFraction startVolume;
	private static Currency startCurrency;
	private static int n;
	private static MongoCollection<Ticker> table;
	private final static Logger log = LogManager.getLogger(Evolve.class);

	
	public Evolve(HashMap<String,Object> start, MongoCollection<Ticker> table) {
		Evolve.startCurrency = new Currency( ((String)start.get("currency")));
		Evolve.startVolume =  new BigFraction(((BigDecimal)start.get("value")).doubleValue());
		Evolve.table = table;
		Evolve.n = 0;
		
	}
	
	private static Double eval(Genotype<AnyGene<Ticker>> g) {
    	BigFraction fitConv = new BigFraction(0.0);
        BigFraction fit = startVolume;
        BigFraction volume = null;
        
        
        if (! Currency.BTC.equals(startCurrency)) {
        	Reference r2 = new Reference(Evolve.table);
        	r2.setReferenceOf(startCurrency);
        	r2.setVolume(startVolume);
        	r2.setReference(Currency.BTC);
        	try {
				volume = r2.getConvertedValue();
			} catch (Exception e) {	
				log.warn(e.getMessage());
			}
        }else {
        	volume = startVolume;
        }
		ArrayList<Double> fitnesses = new ArrayList<Double>();
		
        for (int z = 0; z < g.length(); z++) {
        	Currency start = Evolve.startCurrency; 
        	for (int i = 0; i < g.getChromosome(z).length(); i++) {
        		Ticker ticker = g.getChromosome(z).getGene(i).getAllele();
        		        		
        		if ( ticker.getCurrencyPair().base.toString().equals(start) || ticker.getCurrencyPair().counter.toString().equals(start) ){
        			
        			fit = fit.multiply(new BigFraction(1).divide(new BigFraction(ticker.getAsk().doubleValue())));
        			fit = fit.subtract(fit.multiply(new BigFraction(0.0026)));        		
        		        			
        			if (ticker.getCurrencyPair().base.equals(start)){
        				start = ticker.getCurrencyPair().counter;
        			} else if (ticker.getCurrencyPair().counter.toString().equals(start)){
        				start = ticker.getCurrencyPair().base;        				
        			}else {
        				fitnesses.add(0.0);
        				break;
        			}
        			        			        			
        		}
        	}
			Reference r = new Reference(Evolve.table);
			if (!start.equals(Currency.BTC)) {
				r.setReference(Currency.BTC);
				r.setReferenceOf(start);
				r.setVolume(fit);
				try {
					fitConv = r.getConvertedValue();
				} catch (Exception e) {
					log.warn(e.getMessage());
				}
			} else {
				fitConv = fit;
			}	

    	    fitnesses.add(fitConv.subtract(volume).doubleValue());
        }
        return Collections.max(fitnesses);    
=======

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

public class Evolve {
	private static BigFraction startVolume;
	private static String startCurrency;
	private static int n;
	private static MongoCollection<Ticker> table;
	private static BigFraction startVolumeConv;
	private final static Logger log = LogManager.getLogger(Evolve.class);

	
	public Evolve(HashMap<String,Object> start, MongoCollection<Ticker> table) {
		Evolve.startCurrency = ((Currency)start.get("currency")).getCurrencyCode();
		Evolve.startVolume =  new BigFraction(((BigDecimal)start.get("value")).doubleValue());
		Evolve.table = table;
		Evolve.n = 0;
		
	}
	
    private static Double eval(Genotype<AnyGene<Ticker>> g) {
    	BigFraction fitConv = new BigFraction(0.0);
        BigFraction fit = startVolume;
        BigFraction prevConv = startVolumeConv;
        BigFraction volume;
        
        
        if (!"BTC".equals(startVolume)) {
        	Reference r2 = new Reference(Evolve.table);
        	r2.setReferenceOf(startCurrency);
        	r2.setVolume(startVolume);
        	r2.setReference("BTC");
        	volume = r2.getConvertedValue();
        }else {
        	volume = startVolume;
        }
		ArrayList<Double> fitnesses = new ArrayList<Double>();
		
        for (int z = 0; z < g.length(); z++) {
        	String start = Evolve.startCurrency; 
        	for (int i = 0; i < g.getChromosome(z).length(); i++) {
        		Ticker ticker = g.getChromosome(z).getGene(i).getAllele();
        		        		
        		if ( ticker.getTradePair().getBase().equals(start) || ticker.getTradePair().getQuote().equals(start) ){
        			
        			fit = fit.multiply(new BigFraction(1).divide(new BigFraction(ticker.getTickerAsk())));
        			fit = fit.subtract(fit.multiply(new BigFraction(0.0026)));        		
        			
        			Reference r = new Reference(Evolve.table);
        			if (!start.equals("BTC")) {
        				r.setReference("BTC");
        				r.setReferenceOf(start);
        				r.setVolume(fit);
        				fitConv = r.getConvertedValue();
        			} else {
        				fitConv = fit;
        			}	
        			
        			if (ticker.getTradePair().getBase().equals(start)){
        				start = ticker.getTradePair().getQuote();
        			} else if (ticker.getTradePair().getQuote().equals(start)){
        				start = ticker.getTradePair().getBase();        				
        			}else {
        				fitnesses.add(0.0);
        				break;
        			}
        			        			        			
        			if  (fitConv.doubleValue() > 0.0) { 
        				prevConv = fitConv.subtract(volume);
        			}else {
        				fitnesses.add(fitConv.doubleValue());
        				break;
        			}
        			
        		} else {
        			fitnesses.add(0.0);
        			break;
        		}
        	}
    	    fitnesses.add(prevConv.doubleValue());
        }
        return Collections.max(fitnesses);
        
    }	
	private static Ticker getRandomTicker() {
		Random randomGenerator = new Random();

		if (n == 0) {
			Bson filter = Filters.or(
					Filters.eq("pair.base", Evolve.startCurrency),
					Filters.eq("pair.quote", Evolve.startCurrency)
			);
			Bson sort = Sorts.descending("ask");
			ArrayList<Ticker> result = table.find(Ticker.class).filter(filter).sort(sort).into(new ArrayList<Ticker>());	
			Ticker t = result.get(randomGenerator.nextInt(result.size()));
			n++;
			return t;
		} else {
			Bson sort = Sorts.descending("ask");
			ArrayList<Ticker> result = table.find(Ticker.class).sort(sort).into(new ArrayList<Ticker>());
			Ticker t = result.get(randomGenerator.nextInt(result.size()));
			n++;
			if (n >= 3) {
				n = 0;
			}
			return t;
		}
	}
	
	final Consumer<? super EvolutionResult<AnyGene<Ticker>, Double>> statistics = EvolutionStatistics.ofNumber();
	
	public Phenotype<AnyGene<Ticker>, Double> run() {
        AnyChromosome<Ticker> chrom = AnyChromosome.of(Evolve::getRandomTicker, 3);
		
		final Engine<AnyGene<Ticker>, Double> engine = Engine
                .builder(Evolve::eval, chrom)
                .populationSize(100)
                .optimize(Optimize.MAXIMUM)
                .survivorsSelector(new TournamentSelector<>(10))
                .offspringSelector(new RouletteWheelSelector<>())
                .alterers(new SinglePointCrossover<>(0.05))
                .build();
		
	    Phenotype<AnyGene<Ticker>,Double> result = engine.stream()
	            .limit(limit.bySteadyFitness(5))
	    		.parallel()
	    		.limit(100)
	            .peek(statistics)
	            .collect(toBestPhenotype());
	    log.debug(statistics);
		return result;
	}
}
