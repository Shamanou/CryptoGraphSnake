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
    private KrakenMarketDataService marketDataService;
    private Map<String, KrakenAssetPair> symbols;
    private AccountService accountService;
    private Map<String, Float> minimumOrderSize = new HashMap<>();

    public DbApi(String key, String secret) throws IOException {
        minimumOrderSize.put("ALGO", 50F);
        minimumOrderSize.put("REP", 0.3F);
        minimumOrderSize.put("BAT", 50F);
        minimumOrderSize.put("XBT", 0.002F);
        minimumOrderSize.put("BCH",0.000002F);
        minimumOrderSize.put("ADA",1F);
        minimumOrderSize.put("LINK",10F);
        minimumOrderSize.put("ATOM",1F);
        minimumOrderSize.put("DAI",10F);
        minimumOrderSize.put("DASH",0.03F);
        minimumOrderSize.put("XDG",3000F);
        minimumOrderSize.put("EOS",3F);
        minimumOrderSize.put("ETH",0.02F);
        minimumOrderSize.put("ETC",0.3F);
        minimumOrderSize.put("GNO",0.02F);
        minimumOrderSize.put("ICX",50F);
        minimumOrderSize.put("LSK",10F);
        minimumOrderSize.put("LTC",0.1F);
        minimumOrderSize.put("XMR",0.1F);
        minimumOrderSize.put("NANO",10F);
        minimumOrderSize.put("OMG",10F);
        minimumOrderSize.put("PAXG",0.01F);
        minimumOrderSize.put("QTUM",0.1F);
        minimumOrderSize.put("XRP",30F);
        minimumOrderSize.put("SC",5000F);
        minimumOrderSize.put("XLM",30F);
        minimumOrderSize.put("USDT",5F);
        minimumOrderSize.put("XTZ",1F);
        minimumOrderSize.put("TRX",500F);
        minimumOrderSize.put("USDC",5F);
        minimumOrderSize.put("MLN",0.1F);
        minimumOrderSize.put("WAVES",10F);
        minimumOrderSize.put("ZEC",0.03F);



        CodecRegistry pojoCodecRegistry = fromRegistries(
                fromProviders(PojoCodecProvider.builder().register(TickerDto.class, TradePair.class).build()),
                MongoClient.getDefaultCodecRegistry());
        MongoClient mongo = new MongoClient("mongo:27017", MongoClientOptions.builder().codecRegistry(pojoCodecRegistry).build());
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
        marketDataService = (KrakenMarketDataService) exchange.getMarketDataService();
        symbols = marketDataService.getKrakenAssetPairs().getAssetPairMap();
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
        Thread.sleep(500);
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
                reference.setReference("EUR");
                String currencyCode = value.getCurrency().getCurrencyCode().length() == 4
                        && (value.getCurrency().getCurrencyCode().startsWith("X")
                        || value.getCurrency().getCurrencyCode().startsWith("Z"))
                        ? value.getCurrency().getCurrencyCode().substring(1) : value.getCurrency().getCurrencyCode();
                reference.setReferenceOf(currencyCode);
                reference.setVolume(wallet.get(key).getAvailable());
                value.setValueConverted(reference.getConvertedValue());
                walletValues.add(value);
            }
        }

        walletValues = (ArrayList<Value>) walletValues.stream()
                .filter(value -> {
                    String currencyCode = value.getCurrency().getCurrencyCode().length() == 4
                            && (value.getCurrency().getCurrencyCode().startsWith("X")
                            || value.getCurrency().getCurrencyCode().startsWith("Z"))
                            ? value.getCurrency().getCurrencyCode().substring(1) : value.getCurrency().getCurrencyCode();

                    if (minimumOrderSize.containsKey(currencyCode)) {
                        return value.getValue().doubleValue() > minimumOrderSize.get(currencyCode);
                    }
                    return true;
                })
                .sorted(Comparator.comparing(Value::getValueConverted).reversed())
                .collect(Collectors.toList());
        return walletValues;
    }

    public void getTickerInformation() throws NotAvailableFromExchangeException,
            NotYetImplementedForExchangeException,
            ExchangeException,
            IOException {
        for (KrakenAssetPair symbol : symbols.values()) {
            try {
                TickerDto tickerDto = new TickerDto();
                String base = symbol.getBase().length() == 4
                        && (symbol.getBase().startsWith("X") || symbol.getBase().startsWith("Z")) ? symbol.getBase().substring(1) : symbol.getBase();
                String quote = symbol.getQuote().length() == 4
                        && (symbol.getQuote().startsWith("X") || symbol.getQuote().startsWith("Z")) ?  symbol.getQuote().substring(1) : symbol.getQuote();

                CurrencyPair pair = new CurrencyPair(base + "/" + quote);
                Ticker tk = marketDataService.getTicker(pair);
                BigDecimal ask = tk.getAsk();
                BigDecimal bid = tk.getBid();

                if (bid != null && ask != null) {
                    tickerDto.setTickerAsk(ask.doubleValue());
                    tickerDto.setTickerBid(bid.doubleValue());
                    tickerDto.setTradePair(new com.shamanou.TradePair(
                            base,
                            quote
                            ));
                    table.insertOne(tickerDto);
                }
            } catch (HttpStatusIOException | ExchangeException ex) {
                log.warn(symbol.getBase() + symbol.getQuote() + " exited with " + ex.getMessage());
            }
        }
    }
}
