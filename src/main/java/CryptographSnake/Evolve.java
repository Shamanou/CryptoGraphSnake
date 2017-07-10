package CryptographSnake;

import static org.jenetics.engine.EvolutionResult.toBestPhenotype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.function.Consumer;

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
    	ArrayList<Double> list = new ArrayList<Double>();
    	for (int i = 0; i < chrom.length(); i++) {
        	Ticker inval = chrom.getChromosome(0).getGene(0).getAllele();
        	BigFraction fit = startVolume;
        	String start = Evolve.startCurrency; 
        	BigFraction prevConv = startVolumeConv; 
        	int x = 0;
        	for (int z = 0; z < chrom.getChromosome(i).length(); z++) {
        		Ticker ticker = chrom.getChromosome(i).getGene(z).getAllele();
        		if ( ticker.getTradePair().getBase().equals(start) || 
        				ticker.getTradePair().getQuote().equals(start) ){
        			
//        			if (fit.doubleValue() <= 0.01) {
//        				return 0.0;
//        			}
        			
        			if (ticker.getTradePair().getBase().equals(start)){
        				fit = fit.multiply(new BigFraction(ticker.getTickerAsk()));
        			} else if (ticker.getTradePair().getQuote().equals(start)){
        				fit = fit.divide(new BigFraction(ticker.getTickerAsk()));
        			}
        			
//        			if (fit.doubleValue() <= 0.01) {
//        				return 0.0;
//        			}
        			
//        			System.out.println(startVolumeConv.doubleValue());
//        			System.out.println(ticker.getTradePair().getBase()+ticker.getTradePair().getQuote());
//            		System.out.println(fit.doubleValue());
        			
        			Reference r = new Reference(Evolve.table);
        			r.setReference(ticker.getFeesVolumeCurrency());
        			r.setReferenceOf(start);
        			r.setVolume(fit);
        			
        			fitConv = r.getConvertedValue();
        			
        			r.setReference(ticker.getFeesVolumeCurrency());
        			r.setReferenceOf(startCurrency);
        			r.setVolume(startVolume);
        			
        			fitConv = fitConv.subtract(new BigFraction(fitConv.doubleValue() * ticker.getFeesRaw().get(0).get(0)));        			
        			prevConv = fitConv.subtract(r.getConvertedValue());
        			
        			r.setReference("ZUSD");
        			r.setReferenceOf(ticker.getFeesVolumeCurrency());
        			r.setVolume(prevConv);        			
        			
        			prevConv = r.getConvertedValue();
        			
//            		System.out.println(prevConv.doubleValue());
        			if (ticker.getTradePair().getBase().equals(start)){
        				start = ticker.getTradePair().getQuote();
        			}else if  (ticker.getTradePair().getQuote().equals(start)){
        				start = ticker.getTradePair().getBase();        				
        			}
        		} 
        		else {
        			return 0.0;
        		}
        	}
    		list.add( prevConv.doubleValue());
//        	System.out.println("\n");
    	}
    	if (list.size() > 0) {
    		return Collections.max(list);
    	} else {
    		return 0.0;
    	}
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
