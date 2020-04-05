package com.shamanou;

import java.io.IOException;
import java.util.List;

import com.shamanou.domain.TickerDto;
import com.shamanou.domain.Value;
import com.shamanou.service.DatabaseService;
import com.shamanou.service.EvolveService;
import com.shamanou.service.OrderExecutorService;
import io.jenetics.AnyGene;
import io.jenetics.Phenotype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;


public class App {
    final static Logger LOGGER = LoggerFactory.getLogger(App.class);

    @Inject
    private DatabaseService databaseService;

    public static void main(String[] args) {
        DatabaseService databaseService = null;
        String key = args[0];
        String secret = args[1];

        LOGGER.info("\n\n			Welcome to the CryptocurrencyGraphSnake\n			Developed by Shamanou van Leeuwen\n\n\n");
        while (true) {
            try {
                databaseService.getTickerInformation();
                LOGGER.info("\n			+-----------------------+\n			GRABBING TRADE START VALUE\n			+-----------------------+\n\n");
                for (int i = 0; i < 3;i++) {
                    trade(databaseService, key, secret, i);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void trade(DatabaseService api, String key, String secret, int i) throws IOException {
        List<Value> wallet;
        wallet = api.getStart();
        Value start = wallet.get(i);
        OrderExecutorService orderExecutorService = new OrderExecutorService(api.getDatabaseCollection(), start, key, secret);

        LOGGER.info("\n			+-----------------------+\n			EVOLVING TRADE TRAJECTORY - " + start.getCurrency().getDisplayName() + "\n			+-----------------------+\n\n");
        EvolveService e = new EvolveService(start, api.getDatabaseCollection());
        Phenotype<AnyGene<TickerDto>, Double> result = e.run();
        StringBuilder resultString = new StringBuilder();

        for (AnyGene<TickerDto> tickerAnyGene : result.getGenotype().getChromosome()) {
            if(tickerAnyGene.getAllele() != null) {
                TickerDto val = tickerAnyGene.getAllele();
                resultString.append(val.getTradePair().getBase()).append(val.getTradePair().getQuote()).append("\n");
            }
        }

        LOGGER.info("Results:\n" + resultString + "\n");
        if (result.getFitness() > 0.0) {
            orderExecutorService.setOrder(result);
            try {
                orderExecutorService.executeOrder();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
