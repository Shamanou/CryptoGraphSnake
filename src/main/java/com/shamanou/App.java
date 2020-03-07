package com.shamanou;

import java.io.IOException;
import java.util.List;

import io.jenetics.AnyGene;
import io.jenetics.Phenotype;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class App {

    public static void main(String[] args) {
        final Logger log = LoggerFactory.getLogger(App.class);
        DbApi api = null;

        String key = args[0];
        String secret = args[1];
        try {
            api = new DbApi(key, secret);
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }

        log.info("\n\n			Welcome to the CryptocurrencyGraphSnake\n			Developed by Shamanou van Leeuwen\n\n\n");
        while (true) {
            try {
                assert api != null;
                api.getTickerInformation();
            } catch (JSONException | IOException e1) {
                e1.printStackTrace();
            }

            log.info("\n			+-----------------------+\n			GRABBING TRADE START VALUE\n			+-----------------------+\n\n");
            try {
                List<Value> wallet = api.getStart();

                Value start = wallet.get(0);
                OrderExecutor orderExecutor = new OrderExecutor(api.getTable(), start, key, secret);

                log.info("\n			+-----------------------+\n			EVOLVING TRADE TRAJECTORY - " + start.getCurrency().getDisplayName() + "\n			+-----------------------+\n\n");
                Evolve e = new Evolve(start, api.getTable());
                Phenotype<AnyGene<TickerDto>, Double> result = e.run();
                StringBuilder resultString = new StringBuilder();

                for (AnyGene<TickerDto> tickerAnyGene : result.getGenotype().getChromosome()) {
                    if(tickerAnyGene.getAllele() != null) {
                        TickerDto val = tickerAnyGene.getAllele();
                        resultString.append(val.getTradePair().getBase()).append(val.getTradePair().getQuote()).append("\n");
                    }
                }

                log.info("Results:\n" + resultString + "\n");
                if (result.getFitness() > 0.0) {
                    orderExecutor.setOrder(result);
                    orderExecutor.executeOrder();
                }
            } catch (Exception ex) {
                log.warn(ex.getMessage());
            }
        }
    }
}
