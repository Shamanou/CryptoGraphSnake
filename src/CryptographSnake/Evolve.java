package CryptographSnake;

import static org.jenetics.engine.EvolutionResult.toBestPhenotype;
import java.math.BigDecimal;
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
import org.knowm.xchange.currency.Currency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

public class Evolve {
    private static BigFraction startVolume;
    private static String startCurrency;
    private static String currentCurrency;
    private static MongoCollection<Ticker> table;
    private static int N = 0;
    private final static Logger log = LoggerFactory.getLogger(Evolve.class);


    public Evolve(HashMap<String, Object> start, MongoCollection<Ticker> table) {
        Evolve.startCurrency = ((Currency) start.get("currency")).getCurrencyCode();
        Evolve.currentCurrency = Evolve.startCurrency;
        Evolve.startVolume = new BigFraction(((BigDecimal) start.get("value")).doubleValue());
        Evolve.table = table;
    }

    private static Double eval(Genotype<AnyGene<Ticker>> g) {
        BigFraction fitConv = new BigFraction(0.0);
        BigFraction fit = startVolume;
        BigFraction volume;

        if (!Currency.BTC.getSymbol().equals(startCurrency)) {
            Reference r2 = new Reference(Evolve.table);
            r2.setReferenceOf(startCurrency);
            r2.setVolume(startVolume);
            r2.setReference(Currency.BTC.getSymbol());
            volume = r2.getConvertedValue();
        } else {
            volume = startVolume;
        }

        ArrayList<Double> fitnesses = new ArrayList<Double>();

        for (int z = 0; z < g.length(); z++) {
            String start = Evolve.startCurrency;
            String end = Evolve.startCurrency;
            for (int i = 0; i < g.getChromosome(z).length(); i++) {
                Ticker ticker = g.getChromosome(z).getGene(i).getAllele();

                if (ticker.getTradePair().getBase().equals(start) || ticker.getTradePair().getQuote().equals(start)) {

                    if (ticker.getTradePair().getBase().equals(end)) {
                        fit = fit.multiply(new BigFraction(ticker.getTickerAsk()));
                    } else if (ticker.getTradePair().getQuote().equals(end)) {
                        fit = new BigFraction(Math.sqrt(fit.multiply(new BigFraction(ticker.getTickerBid())).doubleValue()));
                    }
                    end = ticker.getTradePair().getQuote();
                    fit = fit.subtract(fit.multiply(new BigFraction(0.1)).add(fit.multiply(new BigFraction(0.01))));

                    Reference r = new Reference(Evolve.table);
                    if (!start.equals(Currency.BTC.getSymbol())) {
                        r.setReference(Currency.BTC.getSymbol());
                        r.setReferenceOf(end);
                        r.setVolume(fit);
                        fitConv = r.getConvertedValue();
                    } else {
                        fitConv = fit;
                    }

                    if (fitConv.doubleValue() > 0.0) {
                        fitConv = fitConv.subtract(volume);
                    } else {
                        fitConv = new BigFraction(0.0);
                        break;
                    }
                } else {
                    break;
                }
            }
            fitnesses.add(fitConv.doubleValue());
        }
        return Collections.max(fitnesses);

    }

    private static Ticker getRandomTicker() {
        if (N == 3) {
            N = 0;
            Evolve.currentCurrency = Evolve.startCurrency;
        }

        Random randomGenerator = new Random();
        Bson filter = Filters.or(
                Filters.eq("pair.base", Evolve.currentCurrency),
                Filters.eq("pair.quote", Evolve.currentCurrency));
        Bson sort = Sorts.descending("ask");
        ArrayList<Ticker> result = table.find(Ticker.class).filter(filter).sort(sort).into(new ArrayList<Ticker>());
        Ticker t = result.get(randomGenerator.nextInt(result.size()));

        if (t.getTradePair().getQuote().equals(Evolve.currentCurrency)) {
            Evolve.currentCurrency = t.getTradePair().getBase();
        } else if (t.getTradePair().getBase().equals(Evolve.currentCurrency)) {
            Evolve.currentCurrency = t.getTradePair().getQuote();
        }
        Evolve.N++;
        return t;
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

        Phenotype<AnyGene<Ticker>, Double> result = engine.stream()
                .limit(limit.bySteadyFitness(5))
                .parallel()
                .limit(100)
                .peek(statistics)
                .collect(toBestPhenotype());
        log.info(statistics.toString());
        return result;
    }
}
