package com.shamanou;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Iterator;

import io.jenetics.AnyGene;
import io.jenetics.Chromosome;
import io.jenetics.Genotype;
import io.jenetics.Phenotype;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.kraken.KrakenExchange;
import org.knowm.xchange.kraken.service.KrakenTradeService;
import org.knowm.xchange.service.account.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mongodb.client.MongoCollection;

public class OrderExecutor {
    private final MongoCollection<TickerDto> table;
    private Value start;
    private Phenotype<AnyGene<TickerDto>, Double> order;
    private static final Logger log = LoggerFactory.getLogger(OrderExecutor.class);
    private KrakenTradeService tradeService;
    private AccountService accountService;

    public OrderExecutor(MongoCollection<TickerDto> table, Value start, String k, String s) {
        this.start = start;
        this.table = table;

        ExchangeSpecification exchangeSpecification = new ExchangeSpecification(KrakenExchange.class.getName());
        exchangeSpecification.setApiKey(k);
        exchangeSpecification.setSecretKey(s);
        Exchange exchange = ExchangeFactory.INSTANCE.createExchange(KrakenExchange.class.getName());
        exchange.applySpecification(exchangeSpecification);
        tradeService = (KrakenTradeService)exchange.getTradeService();
        accountService = exchange.getAccountService();
    }

    public void setOrder(Phenotype<AnyGene<TickerDto>, Double> result) {
        this.order = result;
    }

    public void executeOrder() throws IOException {
        Genotype<AnyGene<TickerDto>> gt = this.order.getGenotype();
        Chromosome<AnyGene<TickerDto>> chromosome = gt.getChromosome();
        Iterator<AnyGene<TickerDto>> it = chromosome.iterator();
        Object[] currencyLabels = this.start.getCurrency().getCurrencyCodes().toArray();
        String inval = (String)currencyLabels[currencyLabels.length - 1];
        while (it.hasNext()) {
            TickerDto val = it.next().getAllele();
            MarketOrder order;
            BigDecimal available = accountService.getAccountInfo()
                    .getWallets().get(null).getBalance(new Currency(inval)).getAvailable();
            if ((val.getTradePair().getQuote()).contains(inval)) {

//                Reference reference = new Reference(this.table);
//                reference.setReference(val.getTradePair().getQuote());
//                reference.setReferenceOf(inv);
//                reference.setVolume(available);
                available = available
                        .subtract(available.multiply(new BigDecimal("0.26"))).setScale(3, RoundingMode.DOWN);

                order = getMarketOrder(val, available, OrderType.ASK);
                inval = val.getTradePair().getBase();
                executeOrder(val, order);
            } else if (val.getTradePair().getBase().contains(inval)) {
                available = available
                        .subtract(available.multiply(new BigDecimal("0.26"))).setScale(3, RoundingMode.DOWN);

                order = getMarketOrder(val, available, OrderType.BID);
                inval = val.getTradePair().getQuote();
                executeOrder(val, order);
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                log.warn(e.getMessage());
                break;
            }
        }
    }

    private MarketOrder getMarketOrder(TickerDto val, BigDecimal available, OrderType orderType) {
        return new MarketOrder(orderType, available,
                new CurrencyPair(val.getTradePair().getBase().length() == 4
                        && (val.getTradePair().getBase().startsWith("X")
                        || val.getTradePair().getBase().startsWith("Z"))

                        ? val.getTradePair().getBase().substring(1, 4)
                        : val.getTradePair().getBase(),
                        val.getTradePair().getQuote().length() == 4
                                && (val.getTradePair().getQuote().startsWith("X")
                                || val.getTradePair().getQuote().startsWith("Z"))
                                ? val.getTradePair().getQuote().substring(1, 4)
                                : val.getTradePair().getQuote()));
    }

    private void executeOrder(TickerDto val, MarketOrder order) throws IOException {
        try {
            tradeService.placeKrakenMarketOrder(order);
            log.warn( order.getType().name()+ " -> " + val.getTradePair().getBase() + " - " + val.getTradePair().getQuote() + " trade executed");
        } catch (ExchangeException ex) {
            log.warn("Could not execute order (" + order.getType().name()+ " -> " + order.getCurrencyPair().toString() + "): " + ex.getMessage());
        }
    }
}
