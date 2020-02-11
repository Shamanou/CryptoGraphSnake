package com.shamanou;

import static org.jenetics.engine.EvolutionResult.toBestPhenotype;
import java.math.BigDecimal;
import java.util.*;
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
    private static BigDecimal startVolume;
    private static String startCurrency;
    private static String currentCurrency;
    private static MongoCollection<TickerDto> table;
    private static int N = 0;
    private final static Logger LOG = LoggerFactory.getLogger(Evolve.class);

    public Evolve(Value start, MongoCollection<TickerDto> table) {
        Object[] currencyCodes = start.getCurrency().getCurrencyCodes().toArray();
        Evolve.startCurrency = (String)currencyCodes[currencyCodes.length-1];
        Evolve.currentCurrency = Evolve.startCurrency;
        Evolve.startVolume = start.getValue();
        Evolve.table = table;
    }

    private static Double eval(Genotype<AnyGene<TickerDto>> g) {
        BigDecimal fitConv = BigDecimal.valueOf(0.0);
        BigDecimal fit = startVolume;
        BigDecimal volume = startVolume;

        if (startCurrency.contains((String)Currency.BTC.getCurrencyCodes().toArray()[1])) {
            Reference r2 = new Reference(Evolve.table);
            r2.setReferenceOf(startCurrency);
            r2.setVolume(startVolume);
            r2.setReference(startCurrency);
            try {
                volume = r2.getConvertedValue();
            } catch (IllegalArgumentException ignored){ }
        }

        ArrayList<Double> fitnesses = new ArrayList<>();
        for (int z = 0; z < g.length(); z++) {
            String end = Evolve.startCurrency;
            for (int i = 0; i < g.getChromosome(z).length(); i++) {
                TickerDto tickerDto = g.getChromosome(z).getGene(i).getAllele();

                if (is(end, tickerDto.getTradePair().getBase())) {
                    fit = fit.multiply(BigDecimal.valueOf(tickerDto.getTickerAsk()));
                } else if (is(end, tickerDto.getTradePair().getQuote())) {
                    fit = fit.divideToIntegralValue(BigDecimal.valueOf(tickerDto.getTickerBid()));
                }

                if (is(end, tickerDto.getTradePair().getBase()) || is(end, tickerDto.getTradePair().getQuote())) {
                    if (tickerDto.getTradePair().getQuote().contains(end)) {
                        end = tickerDto.getTradePair().getBase();
                    } else {
                        end = tickerDto.getTradePair().getQuote();
                    }
                    BigDecimal feeA = fit.multiply(new BigFraction(0.1).bigDecimalValue());
                    BigDecimal feeB = fit.multiply(new BigFraction(0.01).bigDecimalValue());

                    fit = fit.subtract(feeA).subtract(feeB);

                    Reference r = new Reference(Evolve.table);
                    if (end.equals(Currency.BTC.getCurrencyCodes().toArray()[1])) {
                        r.setReference((String)Currency.BTC.getCurrencyCodes().toArray()[1]);
                        r.setReferenceOf(end);
                        r.setVolume(fit);
                        try {
                            fitConv = r.getConvertedValue();
                        }catch (IllegalArgumentException ex){
                            break;
                        }
                    } else {
                        fitConv = fit;
                    }
                    if (fitConv.doubleValue() > 0.0) {
                        fitConv = fitConv.subtract(volume);
                    } else {
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

    private static boolean is(String end, String quote) {
        return quote.contains(end);
    }

    private static TickerDto getRandomTicker() {
        if (N == 3) {
            N = 0;
            Evolve.currentCurrency = Evolve.startCurrency;
        }

        Random randomGenerator = new Random();
        Bson filter = Filters.or(
                Filters.regex("tradePair.base", Evolve.currentCurrency),
                Filters.regex("tradePair.quote", Evolve.currentCurrency));
        Bson sort = Sorts.descending("tickerAsk");
        ArrayList<TickerDto> result = table.find(TickerDto.class).filter(filter).sort(sort).into(new ArrayList<>());
        TickerDto t = result.get(randomGenerator.nextInt(result.size()));

        if (t.getTradePair().getQuote().equals(Evolve.currentCurrency)) {
            Evolve.currentCurrency = t.getTradePair().getBase();
        } else if (t.getTradePair().getBase().equals(Evolve.currentCurrency)) {
            Evolve.currentCurrency = t.getTradePair().getQuote();
        }
        Evolve.N++;
        return t;
    }

    final Consumer<? super EvolutionResult<AnyGene<TickerDto>, Double>> statistics = EvolutionStatistics.ofNumber();

    public Phenotype<AnyGene<TickerDto>, Double> run() {
        AnyChromosome<TickerDto> chrom = AnyChromosome.of(Evolve::getRandomTicker, 3);

        final Engine<AnyGene<TickerDto>, Double> engine = Engine
                .builder(Evolve::eval, chrom)
                .populationSize(500)
                .optimize(Optimize.MAXIMUM)
                .survivorsSelector(new TournamentSelector<>(10))
                .offspringSelector(new RouletteWheelSelector<>())
                .alterers(new SinglePointCrossover<>(0.05))
                .build();

        Phenotype<AnyGene<TickerDto>, Double> result = engine.stream()
                .limit(limit.bySteadyFitness(5))
                .parallel()
                .limit(100)
                .peek(statistics)
                .collect(toBestPhenotype());
        LOG.info(statistics.toString());
        return result;
    }
}
