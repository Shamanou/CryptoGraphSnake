package com.shamanou;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.exceptions.NotAvailableFromExchangeException;
import org.knowm.xchange.exceptions.NotYetImplementedForExchangeException;
import org.knowm.xchange.kraken.KrakenExchange;
import org.knowm.xchange.kraken.dto.marketdata.KrakenAssetPair;
import org.knowm.xchange.kraken.service.KrakenMarketDataService;
import org.knowm.xchange.service.account.AccountService;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import si.mazi.rescu.HttpStatusIOException;

public class DbApi {
    private MongoCollection<TickerDto> table;
    private static final Logger log = LoggerFactory.getLogger(DbApi.class);
    private MarketDataService marketDataService;
    private Collection<KrakenAssetPair> symbols;
    private AccountService accountService;

    public DbApi(String key, String secret) throws IOException {

        CodecRegistry pojoCodecRegistry = fromRegistries(
                fromProviders(PojoCodecProvider.builder().register(TickerDto.class, TradePair.class).build()),
                MongoClient.getDefaultCodecRegistry());
        MongoClient mongo = new MongoClient("localhost", MongoClientOptions.builder().codecRegistry(pojoCodecRegistry).build());
        MongoDatabase db = mongo.getDatabase("trade");
        this.table = db.getCollection("trade", TickerDto.class);
        try {
            this.table.drop();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        this.table = db.getCollection("trade", TickerDto.class);

        ExchangeSpecification exchangeSpecification = new KrakenExchange().getDefaultExchangeSpecification();
        exchangeSpecification.setApiKey(key);
        exchangeSpecification.setSecretKey(secret);
        Exchange exchange = ExchangeFactory.INSTANCE.createExchange(KrakenExchange.class.getName());
        exchange.applySpecification(exchangeSpecification);
        marketDataService = exchange.getMarketDataService();
        symbols = ((KrakenMarketDataService) exchange.getMarketDataService()).getKrakenAssetPairs().getAssetPairMap().values();
        accountService = exchange.getAccountService();
    }

    public MongoCollection<TickerDto> getTable() {
        return this.table;
    }

    public List<Value> getStart()
            throws NotAvailableFromExchangeException,
            NotYetImplementedForExchangeException,
            ExchangeException,
            IOException,
            InterruptedException {
        Thread.sleep(5000);
        Map<Currency, Balance> wallet = accountService.getAccountInfo().getWallets().get(null).getBalances();

        Iterator<Currency> keyIt = wallet.keySet().iterator();
        ArrayList<Value> walletValues = new ArrayList<>();

        while (keyIt.hasNext()) {
            Currency key = keyIt.next();
            Value value = new Value();

            if (wallet.get(key).getAvailable().doubleValue() > 0.0) {
                value.setCurrency(key);
                value.setValue(wallet.get(key).getAvailable());

                Reference reference = new Reference(this.table);
                reference.setReference("XXBT");
                reference.setReferenceOf(key.getIso4217Currency().getSymbol());
                reference.setVolume(wallet.get(key).getAvailable());
                value.setValueConverted(reference.getConvertedValue());
                walletValues.add(value);
            }
        }

        walletValues = (ArrayList<Value>) walletValues.stream()
                .filter(value -> value.getValueConverted().doubleValue() != 0.0)
                .sorted(Comparator.comparing(Value::getValueConverted).thenComparing(Value::getValue).reversed())
                .collect(Collectors.toList());
        return walletValues;
    }

    public void getTickerInformation() throws NotAvailableFromExchangeException,
            NotYetImplementedForExchangeException,
            ExchangeException,
            IOException {
        for (KrakenAssetPair symbol : symbols) {
            try {
                TickerDto tickerDto = new TickerDto();
                if (symbol.getWsName() != null) {
                    Ticker tk = marketDataService.getTicker(new CurrencyPair(symbol.getWsName()));
                    BigDecimal ask = tk.getAsk();
                    BigDecimal bid = tk.getBid();

                    if (bid != null && ask != null) {
                        tickerDto.setTickerAsk(ask.doubleValue());
                        tickerDto.setTickerBid(bid.doubleValue());
                        tickerDto.setTradePair(new com.shamanou.TradePair(symbol.getBase(), symbol.getQuote()));
                        table.insertOne(tickerDto);
                    }
                }
            } catch (HttpStatusIOException | ExchangeException ex) {
                log.warn(symbol.getBase() + symbol.getQuote() + " exited with " + ex.getMessage());
            }
        }
    }
}
