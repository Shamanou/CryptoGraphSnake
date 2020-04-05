package com.shamanou.service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.mongodb.client.model.Filters;
import com.shamanou.config.Configuration;
import com.shamanou.domain.Reference;
import com.shamanou.domain.TickerDto;
import com.shamanou.domain.Value;
import io.jenetics.*;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.engine.EvolutionStatistics;
import io.jenetics.engine.Limits;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mongodb.client.MongoCollection;

public class EvolveService {
    private static BigDecimal startVolume;
    private static String startCurrency;
    private static String currentCurrency;
    private static MongoCollection<TickerDto> table;
    private static int N = 0;
    private static final Logger LOG = LoggerFactory.getLogger(EvolveService.class);
    private static Configuration CONFIGURATION = null;

    public EvolveService(Value start, MongoCollection<TickerDto> table) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();
        CONFIGURATION = mapper.readValue(new File("src/main/resources/config.yml"), Configuration.class);

        final Object[] currencyCodes = start.getCurrency().getCurrencyCodes().toArray();
        EvolveService.startCurrency = (String)currencyCodes[currencyCodes.length-1];
        EvolveService.currentCurrency = EvolveService.startCurrency;
        EvolveService.startVolume = start.getValue();
        EvolveService.table = table;
    }

    private static double eval(Genotype<AnyGene<TickerDto>> genome) {
        Reference volumeReference = new Reference(EvolveService.table);
        volumeReference.setReferenceOf(startCurrency);
        volumeReference.setVolume(startVolume);
        volumeReference.setReference(CONFIGURATION.getReferenceCurrency());

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
            if (EvolveService.currentCurrency.equals(tickerDto.getTradePair().getBase())) {
                fit.updateAndGet(fitness -> {
                    fitness /= tickerDto.getTickerBid();
                    fitness -= (fitness / 100)  * feePercentage;
                    return returnFitnessIfValid(fitness);
                });
                EvolveService.currentCurrency  = tickerDto.getTradePair().getQuote();
            } else if (EvolveService.currentCurrency.equals(tickerDto.getTradePair().getQuote())) {
                fit.updateAndGet(fitness -> {
                    fitness *= tickerDto.getTickerAsk();
                    fitness -= (fitness/ 100) * feePercentage;;
                    return returnFitnessIfValid(fitness);
                });
                EvolveService.currentCurrency = tickerDto.getTradePair().getBase();
            } else {
                return fit.getAndSet(0D);
            }
            return fit.get();
        }).collect(Collectors.toList());

        Reference reference = new Reference(EvolveService.table);
        reference.setReference(CONFIGURATION.getReferenceCurrency());
        reference.setReferenceOf(EvolveService.currentCurrency);
        reference.setVolume(BigDecimal.valueOf(fitnessOfGenes.get(fitnessOfGenes.size()-1)));
        return volumeReference.getConvertedValue().doubleValue() - reference.getConvertedValue().doubleValue();
    }

    private static Double returnFitnessIfValid(Double fitness) {
        if (!CONFIGURATION.getCurrencyLimits().containsKey(EvolveService.currentCurrency)){
            return fitness;
        } else if (CONFIGURATION.getCurrencyLimits().get(EvolveService.currentCurrency).getMinimumOrderSize() > fitness ) {
            return fitness;
        } else {
            return 0D;
        }
    }

    private static TickerDto getRandomTicker() {
        if (N == 3) {
            N = 0;
            EvolveService.currentCurrency = EvolveService.startCurrency;
        }

        Random randomGenerator = new Random();
        Bson filter = Filters.or(
                Filters.eq("tradePair.base", EvolveService.currentCurrency),
                Filters.eq("tradePair.quote", EvolveService.currentCurrency));
        ArrayList<TickerDto> result = table.find(TickerDto.class).filter(filter).into(new ArrayList<>());

        TickerDto t = result.get(randomGenerator.nextInt(result.size()));
        if (t.getTradePair().getQuote().equals(EvolveService.currentCurrency)) {
            EvolveService.currentCurrency = t.getTradePair().getBase();
        } else if (t.getTradePair().getBase().equals(EvolveService.currentCurrency)) {
            EvolveService.currentCurrency = t.getTradePair().getQuote();
        }
        EvolveService.N++;
        return t;
    }

    final Consumer<? super EvolutionResult<AnyGene<TickerDto>, Double>> statistics = EvolutionStatistics.ofNumber();

    public Phenotype<AnyGene<TickerDto>, Double> run() {
        AnyChromosome<TickerDto> chromosome = AnyChromosome.of(EvolveService::getRandomTicker, CONFIGURATION.getGenomeSize());

        final Engine<AnyGene<TickerDto>, Double> engine = Engine
                .builder(EvolveService::eval, chromosome)
                .populationSize(1000)
                .optimize(Optimize.MAXIMUM)
                .survivorsSelector(new TournamentSelector<>(10))
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
