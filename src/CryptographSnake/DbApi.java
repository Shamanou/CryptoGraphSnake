package CryptographSnake;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.fraction.BigFraction;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.exceptions.NotAvailableFromExchangeException;
import org.knowm.xchange.exceptions.NotYetImplementedForExchangeException;
import org.knowm.xchange.hitbtc.v2.HitbtcExchange;
import org.knowm.xchange.hitbtc.v2.dto.HitbtcSymbol;
import org.knowm.xchange.hitbtc.v2.service.HitbtcMarketDataService;
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
    private MongoClient mongo;
    private MongoDatabase db;
    private MongoCollection<Ticker> table;
    private CodecRegistry pojoCodecRegistry;
    private static final Logger log = LoggerFactory.getLogger(DbApi.class);
    private Exchange exchange = ExchangeFactory.INSTANCE.createExchange(HitbtcExchange.class.getName());
    private MarketDataService marketDataService;
    private List<HitbtcSymbol> symbols;
    private AccountService accountService;

    public DbApi(String key, String secret) throws IOException {

        this.pojoCodecRegistry = fromRegistries(
                fromProviders(PojoCodecProvider.builder().register(Ticker.class, TradePair.class).build()),
                MongoClient.getDefaultCodecRegistry());
        this.mongo = new MongoClient("localhost", MongoClientOptions.builder().codecRegistry(pojoCodecRegistry).build());
        this.db = this.mongo.getDatabase("trade");
        this.table = this.db.getCollection("trade", Ticker.class);
        try {
            this.table.drop();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        this.table = this.db.getCollection("trade", Ticker.class);

        ExchangeSpecification exchangeSpecification = new HitbtcExchange().getDefaultExchangeSpecification();
        exchangeSpecification.setApiKey(key);
        exchangeSpecification.setSecretKey(secret);
        exchange.applySpecification(exchangeSpecification);
        marketDataService = exchange.getMarketDataService();
        symbols = ((HitbtcMarketDataService) exchange.getMarketDataService()).getHitbtcSymbols();
        accountService = exchange.getAccountService();
    }

    public MongoCollection<Ticker> getTable() {
        return this.table;
    }

    public ArrayList<HashMap<String, Object>> getStart() throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException, ExchangeException, IOException {
        Map<Currency, Balance> wallet = accountService.getAccountInfo().getWallet("Trading").getBalances();

        Iterator<Currency> keyIt = wallet.keySet().iterator();
        ArrayList<HashMap<String, Object>> wv = new ArrayList<HashMap<String, Object>>();

        while (keyIt.hasNext()) {
            Currency key = keyIt.next();

            if (wallet.get(key).getAvailable().doubleValue() > 0.0) {
                HashMap<String, Object> map = new HashMap<String, Object>();
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

        wv.sort((HashMap<String, Object> z1, HashMap<String, Object> z2) -> {
            if (((BigFraction) z1.get("value_conv")).doubleValue() > ((BigFraction) z2.get("value_conv")).doubleValue()) {
                return 1;
            }
            if (((BigFraction) z1.get("value_conv")).doubleValue() < ((BigFraction) z2.get("value_conv")).doubleValue()) {
                return -1;
            }
            return 0;
        });
        Collections.reverse(wv);
        return wv;
    }

    public void getTickerInformation() throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException, ExchangeException, IOException {
        for (HitbtcSymbol symbol : symbols) {
            try {
                Ticker ticker = new Ticker();
                org.knowm.xchange.dto.marketdata.Ticker tk = marketDataService.getTicker(new CurrencyPair(symbol.getBaseCurrency(), symbol.getQuoteCurrency()));
                BigDecimal ask = tk.getAsk();
                BigDecimal bid = tk.getBid();

                if ((bid != null) && (ask != null)) {
                    ticker.setTickerAsk(ask.doubleValue());
                    ticker.setTickerBid(bid.doubleValue());
                    ticker.setTradePair(new TradePair(symbol.getBaseCurrency(), symbol.getQuoteCurrency()));
                    table.insertOne(ticker);
                }
            } catch (HttpStatusIOException ex) {
                log.warn(symbol.getBaseCurrency() + symbol.getQuoteCurrency() + " exited with " + ex.getMessage());
            }
        }
    }
}
