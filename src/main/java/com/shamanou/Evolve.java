package com.shamanou;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.mongodb.client.model.Filters;
import io.jenetics.*;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.engine.EvolutionStatistics;
import io.jenetics.engine.Limits;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mongodb.client.MongoCollection;

public class Evolve {
    private static BigDecimal startVolume;
    private static String startCurrency;
    private static String currentCurrency;
    private static MongoCollection<TickerDto> table;
    private static int N = 0;
    private static final Logger LOG = LoggerFactory.getLogger(Evolve.class);
    private static String referenceCurrency;

    public Evolve(Value start, MongoCollection<TickerDto> table) {
        final Object[] currencyCodes = start.getCurrency().getCurrencyCodes().toArray();
        Evolve.startCurrency = (String)currencyCodes[currencyCodes.length-1];
        Evolve.currentCurrency = Evolve.startCurrency;
        Evolve.startVolume = start.getValue();
        Evolve.table = table;
        referenceCurrency = "XBT";
    }

    private static double eval(Genotype<AnyGene<TickerDto>> genome) {
        BigDecimal fitConv;
        Reference volumeReference = new Reference(Evolve.table);
        volumeReference.setReferenceOf(startCurrency);
        volumeReference.setVolume(startVolume);
        volumeReference.setReference(referenceCurrency);

        List<TickerDto> genes = genome.stream().flatMap(chromosome -> {
            ArrayList<TickerDto> out = new ArrayList<>();
            for (int i = 0; i < chromosome.length(); i++){
                out.add(chromosome.getGene(i).getAllele());
            }
            return out.stream();
        }).collect(Collectors.toList());

        if (genes.stream().anyMatch(Objects::isNull)){
            return 0.0;
        }

        AtomicReference<Double> fit = new AtomicReference<>(startVolume.doubleValue());
        List<Double> fitnessOfGenes = genes.stream().map(tickerDto -> {
            double feePercentage = 0.26;
            if (Evolve.currentCurrency.contains(tickerDto.getTradePair().getBase())) {
                fit.updateAndGet(fitness -> {
                    fitness *= tickerDto.getTickerBid();
                    fitness -= fitness * feePercentage;
                    return fitness;
                });
                Evolve.currentCurrency  = tickerDto.getTradePair().getQuote();
            } else if (Evolve.currentCurrency.contains(tickerDto.getTradePair().getQuote())) {
                fit.updateAndGet(fitness -> {
                    fitness /= tickerDto.getTickerAsk();
                    fitness -= fitness * feePercentage;;
                    return fitness;
                });
                Evolve.currentCurrency = tickerDto.getTradePair().getBase();
            }
            return fit.get();
        }).collect(Collectors.toList());

        Reference reference = new Reference(Evolve.table);
        reference.setReference(referenceCurrency);
        reference.setReferenceOf(Evolve.currentCurrency);
        reference.setVolume(BigDecimal.valueOf(fitnessOfGenes.get(2)));
        fitConv = reference.getConvertedValue();
        return fitConv.doubleValue();
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
        ArrayList<TickerDto> result = table.find(TickerDto.class).filter(filter).into(new ArrayList<>());
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
        AnyChromosome<TickerDto> chromosome = AnyChromosome.of(Evolve::getRandomTicker, 3);

        final Engine<AnyGene<TickerDto>, Double> engine = Engine
                .builder(Evolve::eval, chromosome)
                .populationSize(500)
                .optimize(Optimize.MAXIMUM)
                .survivorsSelector(new TournamentSelector<>(10))
                .offspringSelector(new RouletteWheelSelector<>())
                .alterers(new SinglePointCrossover<>(0.05))
                .build();

        Phenotype<AnyGene<TickerDto>, Double> result = engine.stream()
                .limit(Limits.bySteadyFitness(50))
                .parallel()
                .limit(100)
                .peek(statistics)
                .collect(EvolutionResult.toBestPhenotype());
        LOG.info(statistics.toString());
        return result;
    }
}
