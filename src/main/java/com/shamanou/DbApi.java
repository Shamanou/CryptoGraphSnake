package com.shamanou;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import org.apache.commons.math3.fraction.BigFraction;
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

    public ArrayList<HashMap<String, Object>> getStart()
            throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException, ExchangeException, IOException {
        Map<Currency, Balance> wallet = accountService.getAccountInfo().getWallet("null").getBalances();

        Iterator<Currency> keyIt = wallet.keySet().iterator();
        ArrayList<HashMap<String, Object>> wv = new ArrayList<>();

        while (keyIt.hasNext()) {
            Currency key = keyIt.next();

            if (wallet.get(key).getAvailable().doubleValue() > 0.0) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("currency", key);
                map.put("value", wallet.get(key).getAvailable());

                Reference r = new Reference(this.table);
                if (!((Currency) map.get("currency")).getCurrencyCode().equals(Currency.BTC.getCurrencyCode())) {
                    r.setReference(Currency.BTC.getCurrencyCode());
                    r.setReferenceOf(((Currency) map.get("currency")).getCurrencyCode());
                    r.setVolume(new BigFraction(((BigDecimal) map.get("value")).doubleValue()));
                    map.put("value_conv", r.getConvertedValue());
                } else {
                    map.put("value_conv", new BigFraction(((BigDecimal) map.get("value")).doubleValue()));
                }

                wv.add(map);
            }
        }

        wv.sort(Comparator.comparingDouble((HashMap<String, Object> z) -> ((BigFraction) z.get("value_conv")).doubleValue()));
        Collections.reverse(wv);
        return wv;
    }

    public void getTickerInformation() throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException, ExchangeException, IOException {
        for (KrakenAssetPair symbol : symbols) {
            try {
                TickerDto tickerDto = new TickerDto();
                if (symbol.getWsName() != null) {
                    Ticker tk = marketDataService.getTicker(new CurrencyPair(symbol.getWsName()));
                    BigDecimal ask = tk.getAsk();
                    BigDecimal bid = tk.getBid();

                    if (bid != null
                            && ask != null) {
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
