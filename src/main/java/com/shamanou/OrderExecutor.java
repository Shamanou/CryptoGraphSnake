package com.shamanou;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Iterator;
import java.util.List;

import io.jenetics.AnyGene;
import io.jenetics.Chromosome;
import io.jenetics.Genotype;
import io.jenetics.Phenotype;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.kraken.KrakenExchange;
import org.knowm.xchange.kraken.dto.trade.KrakenStandardOrder;
import org.knowm.xchange.kraken.dto.trade.KrakenType;
import org.knowm.xchange.kraken.service.KrakenTradeService;
import org.knowm.xchange.service.account.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mongodb.client.MongoCollection;

public class OrderExecutor {
    private final MongoCollection<TickerDto> table;
    private final List<CurrencyPair> symbols;
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
        symbols = ((KrakenExchange) exchange).getExchangeSymbols();
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
            KrakenStandardOrder order;
            BigDecimal available = accountService.getAccountInfo()
                    .getWallets().get(null).getBalance(new Currency(inval)).getAvailable();

            String base = val.getTradePair().getBase().length() == 4
                    && (val.getTradePair().getBase().startsWith("X")
                    || val.getTradePair().getBase().startsWith("Z")) ? val.getTradePair().getBase().substring(1) : val.getTradePair().getBase();
            String quote = val.getTradePair().getQuote().length() == 4
                    && (val.getTradePair().getQuote().startsWith("X")
                    || val.getTradePair().getQuote().startsWith("Z")) ?  val.getTradePair().getQuote().substring(1) : val.getTradePair().getQuote();


            if (quote.equals(inval)) {

                Reference reference = new Reference(this.table);
                reference.setReference(quote);
                reference.setReferenceOf(base);
                reference.setVolume(available);
                available = reference.getConvertedValue()
                        .subtract(available.divide(new BigDecimal("100"), RoundingMode.FLOOR).multiply(new BigDecimal("0.26")));

                order = getMarketOrder(available, KrakenType.BUY, new CurrencyPair(base + "/" + quote));
                inval = base;
                executeOrder(val, order);
            } else if (base.equals(inval)) {
                available = available
                        .subtract(available.divide(new BigDecimal("100"), RoundingMode.FLOOR)).multiply(new BigDecimal("0.26")));

                order = getMarketOrder(available, KrakenType.SELL, new CurrencyPair(base + "/" + quote));
                inval = quote;
                if (sleep()) break;
                executeOrder(val, order);
            }
        }
    }

    private boolean sleep() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            log.warn(e.getMessage());
            return true;
        }
        return false;
    }

    private KrakenStandardOrder getMarketOrder(BigDecimal available, KrakenType type, CurrencyPair currencyPair) {
        return KrakenStandardOrder.getMarketOrderBuilder(currencyPair, type,available).buildOrder();
    }

    private void executeOrder(TickerDto val, KrakenStandardOrder order) throws IOException {
        try {
            tradeService.placeKrakenOrder(order);
            log.warn( order.getType().name()+ " -> " + val.getTradePair().getBase() + " - " + val.getTradePair().getQuote() + " trade executed");
        } catch (ExchangeException ex) {
            log.warn("Could not execute order (" + order.getType().name()+ " -> " + order.getAssetPair().toString() + "): " + ex.getMessage());
        }
    }
}
