package com.shamanou;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import org.jenetics.AnyGene;
import org.jenetics.Chromosome;
import org.jenetics.Genotype;
import org.jenetics.Phenotype;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.kraken.KrakenExchange;
import org.knowm.xchange.service.account.AccountService;
import org.knowm.xchange.service.trade.TradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mongodb.client.MongoCollection;

public class OrderExecutor {
    private MongoCollection<TickerDto> table;
    private HashMap<String, Object> start;
    private Phenotype<AnyGene<TickerDto>, Double> order;
    private static final Logger log = LoggerFactory.getLogger(OrderExecutor.class);
    private TradeService tradeService;
    private AccountService accountService;


    public OrderExecutor(MongoCollection<TickerDto> table, HashMap<String, Object> start, String k, String s) throws IOException {
        this.start = start;
        this.setTable(table);

        ExchangeSpecification exchangeSpecification = new ExchangeSpecification(KrakenExchange.class.getName());
        exchangeSpecification.setApiKey(k);
        exchangeSpecification.setSecretKey(s);
        Exchange exchange = ExchangeFactory.INSTANCE.createExchange(KrakenExchange.class.getName());
        exchange.applySpecification(exchangeSpecification);
        tradeService = exchange.getTradeService();
        accountService = exchange.getAccountService();
    }

    public void setOrder(Phenotype<AnyGene<TickerDto>, Double> result) {
        this.order = result;
    }

    public void ExecuteOrder() throws IOException {
        Genotype<AnyGene<TickerDto>> gt = this.order.getGenotype();
        Chromosome<AnyGene<TickerDto>> chrom = gt.getChromosome();
        Iterator<AnyGene<TickerDto>> it = chrom.iterator();
        String inval = ((Currency) this.start.get("currency")).getCurrencyCode();
        while (it.hasNext()) {
            TickerDto val = it.next().getAllele();
            MarketOrder order = null;
            BigDecimal available = accountService.getAccountInfo().getWallet("Trading").getBalance(
                    new Currency(inval)).getAvailable();

            log.info(available.toPlainString());

            if (inval.equals(val.getTradePair().getQuote())) {
                order = new MarketOrder(OrderType.BID, available,
                        new CurrencyPair(val.getTradePair().getBase(), val.getTradePair().getQuote()));
                inval = val.getTradePair().getBase();
            } else if (inval.equals(val.getTradePair().getBase())) {
                order = new MarketOrder(OrderType.ASK, available,
                        new CurrencyPair(val.getTradePair().getBase(), val.getTradePair().getQuote()));
                inval = val.getTradePair().getQuote();
            }
            try {
                this.tradeService.placeMarketOrder(order);
                log.info(val.getTradePair().getBase() + " - " + val.getTradePair().getQuote() + " trade executed");
            } catch (Exception ex) {
                log.warn("Could not execute order (" + val.getTradePair().getBase() + " - " + val.getTradePair().getQuote() + "): " + ex.getMessage());
            }
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                log.warn(e.getMessage());
            }
        }
    }

    public MongoCollection<TickerDto> getTable() {
        return table;
    }

    public void setTable(MongoCollection<TickerDto> table) {
        this.table = table;
    }
}
