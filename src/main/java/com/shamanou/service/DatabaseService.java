package com.shamanou.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.shamanou.config.Configuration;
import com.shamanou.domain.Reference;
import com.shamanou.domain.TickerDto;
import com.shamanou.domain.TradePair;
import com.shamanou.domain.Value;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.exceptions.NotAvailableFromExchangeException;
import org.knowm.xchange.exceptions.NotYetImplementedForExchangeException;
import org.knowm.xchange.kraken.dto.marketdata.KrakenAssetPair;
import org.knowm.xchange.kraken.service.KrakenMarketDataService;
import org.knowm.xchange.service.account.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mongodb.client.MongoCollection;
import si.mazi.rescu.HttpStatusIOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class DatabaseService {
    private MongoCollection<TickerDto> databaseCollection;
    private static final Logger log = LoggerFactory.getLogger(DatabaseService.class);
    private KrakenMarketDataService marketDataService;
    private Map<String, KrakenAssetPair> symbols;
    private AccountService accountService;
    private Configuration configuration;

    public DatabaseService(
            ConfigurationService configurationService,
            InitializeService initializeService)
            throws IOException {

        configuration = configurationService.constructConfiguration();
        databaseCollection = initializeService.initializeDatabase(configuration);
        Exchange exchange = initializeService.initializeKrakenApiServices(configuration.getKey(), configuration.getSecret());
        marketDataService = (KrakenMarketDataService) exchange.getMarketDataService();
        symbols = marketDataService.getKrakenAssetPairs().getAssetPairMap();
        accountService = exchange.getAccountService();
    }

    public MongoCollection<TickerDto> getDatabaseCollection() {
        return this.databaseCollection;
    }

    public List<Value> getStart() {
        Map<Currency, Balance> wallet = null;

        try {
            Thread.sleep(500);
            wallet = accountService.getAccountInfo().getWallets().get(null).getBalances();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }

        assert wallet != null;
        Iterator<Currency> keyIt = wallet.keySet().iterator();
        ArrayList<Value> walletValues = new ArrayList<>();

        while (keyIt.hasNext()) {
            Currency key = keyIt.next();
            Value value = new Value();

            if (wallet.get(key).getAvailable().doubleValue() > 0.0) {
                value.setCurrency(key);
                value.setValue(wallet.get(key).getAvailable());

                Reference reference = new Reference(this.databaseCollection);
                reference.setReference(configuration.getReferenceCurrency());
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

                    if (configuration.getCurrencyLimits().containsKey(currencyCode)) {
                        return value.getValue().floatValue() > configuration.getCurrencyLimits().get(currencyCode).getMinimumOrderSize();
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
                    tickerDto.setTradePair(new TradePair(
                            base,
                            quote
                            ));
                    databaseCollection.insertOne(tickerDto);
                }
            } catch (HttpStatusIOException | ExchangeException ex) {
                log.warn(symbol.getBase() + symbol.getQuote() + " exited with " + ex.getMessage());
            }
        }
    }
}
