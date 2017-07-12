package CryptographSnake;

import static org.jenetics.engine.EvolutionResult.toBestPhenotype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.math3.fraction.BigFraction;
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
import org.jenetics.util.ISeq;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

public class Evolve {
	private static BigFraction startVolume;
	private static String startCurrency;
	private static int n;
	private static MongoCollection<Ticker> table;
	private static BigFraction startVolumeConv;
	
	public Evolve(HashMap<String,Object> start, MongoCollection<Ticker> table) {
		Evolve.startCurrency = (String)start.get("currency");
		Evolve.startVolume =  (BigFraction)start.get("volume");
		Evolve.startVolumeConv =  (BigFraction)start.get("volume_converted");
		Evolve.table = table;
		Evolve.n = 0;
		
	}
	
    private static Double eval(Genotype<AnyGene<Ticker>> chrom) {
    	BigFraction fitConv = new BigFraction(0.0);
    	String start = Evolve.startCurrency; 
		Reference r = new Reference(Evolve.table);
        Ticker inval = chrom.getChromosome().getGene(0).getAllele();
        BigFraction fit = startVolume;
        BigFraction prevConv = startVolumeConv; 
        List<AnyGene<Ticker>> list = chrom.getChromosome().toSeq().asList();
        for (int z = 0; z < list.size(); z++) {
        		Ticker ticker = list.get(z).getAllele();
        		if ( ticker.getTradePair().getBase().equals(start) || 
        				ticker.getTradePair().getQuote().equals(start) ){
        			
        			if (ticker.getTradePair().getBase().equals(start)){
        				fit = fit.divide(new BigFraction(ticker.getTickerAsk()));
        			} else if (ticker.getTradePair().getQuote().equals(start)){
        				fit = fit.multiply(new BigFraction(ticker.getTickerAsk()));
        			}
        			
        			
//        			System.out.println(fit.doubleValue());
//        			System.out.println(ticker.getTradePair().getBase()+ticker.getTradePair().getQuote());
//        			
        			r.setReference("XXBT");
        			r.setReferenceOf(start);
        			r.setVolume(fit);
        			
        			fitConv = r.getConvertedValue();
        			fitConv = fitConv.subtract(new BigFraction(fitConv.doubleValue() * ticker.getFeesRaw().get(0).get(0)));
        			
        			r.setReference("XXBT");
        			r.setReferenceOf(startCurrency);
        			r.setVolume(startVolume);
        			
        			prevConv = fitConv.subtract(r.getConvertedValue());

        			
//        			System.out.println(r.getConvertedValue());
        			      			
        			
        			prevConv = r.getConvertedValue();
        			
//            		System.out.println(prevConv.doubleValue());
        			if (ticker.getTradePair().getBase().equals(start)){
        				start = ticker.getTradePair().getQuote();
        			}else if  (ticker.getTradePair().getQuote().equals(start)){
        				start = ticker.getTradePair().getBase();        				
        			}
        		} 
        		else {
//        			return 0.0;
        		}
        	}
			r.setReference("XXBT");
			r.setReferenceOf(start);
			r.setVolume(prevConv);  
//        	System.out.println("\n");
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
                .populationSize(5000)
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
	    System.out.println(statistics);
		return result;
	}
}
