package com.shamanou;

import java.io.IOException;
import java.util.List;

import io.jenetics.AnyGene;
import io.jenetics.Phenotype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class App {
    final static Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        DbApi api = null;

        String key = args[0];
        String secret = args[1];
        try {
            api = new DbApi(key, secret);
        } catch (Exception e) {
            e.printStackTrace();
        }

        log.info("\n\n			Welcome to the CryptocurrencyGraphSnake\n			Developed by Shamanou van Leeuwen\n\n\n");
        while (true) {
            try {
                assert api != null;
                api.getTickerInformation();
            } catch (Exception e) {
                e.printStackTrace();
            }

            log.info("\n			+-----------------------+\n			GRABBING TRADE START VALUE\n			+-----------------------+\n\n");
            for (int i = 0; i < 4;i++) {
                try {
                    trade(api, key, secret, i);
                }catch(Exception ex){
                    ex.printStackTrace();
                }
            }
        }
    }

    private static void trade(DbApi api, String key, String secret, int i) {
        List<Value> wallet = null;
        try {
            wallet = api.getStart();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        assert wallet != null;
        Value start = wallet.get(i);
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
            try {
                orderExecutor.executeOrder();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
