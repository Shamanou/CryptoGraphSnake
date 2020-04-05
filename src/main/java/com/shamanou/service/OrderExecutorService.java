package com.shamanou.service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Iterator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.shamanou.config.Configuration;
import com.shamanou.config.CurrencyPairLimit;
import com.shamanou.domain.Reference;
import com.shamanou.domain.TickerDto;
import com.shamanou.domain.Value;
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

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderExecutorService {
    private final MongoCollection<TickerDto> table;
    private Value start;
    private Phenotype<AnyGene<TickerDto>, Double> order;
    private static final Logger log = LoggerFactory.getLogger(OrderExecutorService.class);
    private KrakenTradeService tradeService;
    private AccountService accountService;
    private final Configuration configuration;

    public OrderExecutorService(MongoCollection<TickerDto> table, Value start, String k, String s)
            throws IOException {
        this.start = start;
        this.table = table;

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();
        configuration = mapper.readValue(new File("src/main/resources/config.yml"), Configuration.class);

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
            KrakenStandardOrder order;
            BigDecimal available = accountService.getAccountInfo()
                    .getWallets().get(null).getBalance(new Currency(inval)).getAvailable();

            String base = val.getTradePair().getBase().length() == 4
                    && (val.getTradePair().getBase().startsWith("X")
                    || val.getTradePair().getBase().startsWith("Z")) ? val.getTradePair().getBase().substring(1) : val.getTradePair().getBase();
            String quote = val.getTradePair().getQuote().length() == 4
                    && (val.getTradePair().getQuote().startsWith("X")
                    || val.getTradePair().getQuote().startsWith("Z")) ?  val.getTradePair().getQuote().substring(1) : val.getTradePair().getQuote();


            String pair = base + "/" + quote;
            CurrencyPairLimit currencyPairLimit = configuration.getCurrencyPairLimits().get(pair);
            if (quote.equals(inval)) {
                Reference reference = new Reference(this.table);
                reference.setReference(base);
                reference.setReferenceOf(quote);
                reference.setVolume(available);

                BigDecimal fee = reference.getConvertedValue().divide(new BigDecimal("100"), RoundingMode.DOWN).multiply(new BigDecimal("0.26")).setScale(currencyPairLimit.getPricePrecision(), RoundingMode.DOWN);

                order = getMarketOrder(reference.getConvertedValue().subtract(fee), KrakenType.BUY, new CurrencyPair(pair));
                inval = base;
                executeOrder(val, order);
            } else if (base.equals(inval)) {
                BigDecimal fee = available.divide(new BigDecimal("100"), RoundingMode.DOWN).multiply(new BigDecimal("0.26")).setScale(currencyPairLimit.getPricePrecision(), RoundingMode.DOWN);

                order = getMarketOrder(available.subtract(fee), KrakenType.SELL, new CurrencyPair(pair));
                inval = quote;
                if (sleep()) break;
                executeOrder(val, order);
            }
        }
    }

    private boolean sleep() {
        try {
            Thread.sleep(500);
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
