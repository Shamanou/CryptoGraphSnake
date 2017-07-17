package CryptographSnake;

import static org.jenetics.engine.EvolutionResult.toBestPhenotype;

import java.util.ArrayList;
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
	
    private static Double eval(Genotype<AnyGene<Ticker>> chrom) {
    	BigFraction fitConv = new BigFraction(0.0);
    	String start = Evolve.startCurrency; 
		Reference r = new Reference(Evolve.table);
        BigFraction fit = startVolume;
        BigFraction prevConv = startVolumeConv;
        
        for (int z = 0; z < chrom.length(); z++) {
        		Ticker ticker = chrom.get(0, z).getAllele();        		
        		if ( ticker.getTradePair().getBase().equals(start) || ticker.getTradePair().getQuote().equals(start) ){
        			
        			if (ticker.getTradePair().getBase().equals(start)){
        				fit = fit.divide(new BigFraction(ticker.getTickerAsk()));
        			} else if (ticker.getTradePair().getQuote().equals(start)){
        				fit = fit.multiply(new BigFraction(ticker.getTickerAsk()));
        			}
        			
//        			System.out.println(fit.doubleValue());
//        			System.out.println(ticker.getTradePair().getBase()+ticker.getTradePair().getQuote());
//        			
        			r.setReference("BTC");
        			r.setReferenceOf(start);
        			r.setVolume(fit);
        			
        			fitConv = r.getConvertedValue();
        			fitConv = new BigFraction(fitConv.doubleValue() - (fitConv.doubleValue() * 0.0026));
        			
        			r.setReference("BTC");
        			r.setReferenceOf(startCurrency);
        			r.setVolume(startVolume);
        			
        			
        			prevConv = new BigFraction(fitConv.doubleValue() - r.getConvertedValue().doubleValue());

//        			System.out.print(fitConv.doubleValue());
//        			System.out.print(" - ");
//        			System.out.print(r.getConvertedValue().doubleValue());
//        			System.out.print(" = ");
//        			System.out.print(prevConv.doubleValue());
//        			System.out.print("\n");
//        			prevConv = r.getConvertedValue();
//            		System.out.println(prevConv.doubleValue());
        			
        			if (ticker.getTradePair().getBase().equals(start)){
        				start = ticker.getTradePair().getQuote();
        			}else if  (ticker.getTradePair().getQuote().equals(start)){
        				start = ticker.getTradePair().getBase();        				
        			}
        		}
        	}
//        	System.out.println("\n");
//		System.out.print(fitConv.doubleValue());
//		System.out.print(" - ");
//		System.out.print(r.getConvertedValue().doubleValue());
//		System.out.print(" = ");
//		System.out.print(prevConv.doubleValue());
//		System.out.print("\n");
	    	return prevConv.doubleValue();
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
