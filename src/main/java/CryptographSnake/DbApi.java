package CryptographSnake;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

<<<<<<< HEAD
import java.io.IOException;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.math3.fraction.BigFraction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.exceptions.NotAvailableFromExchangeException;
import org.knowm.xchange.exceptions.NotYetImplementedForExchangeException;
import org.knowm.xchange.hitbtc.HitbtcExchange;
import org.knowm.xchange.hitbtc.v2.dto.HitbtcBalance;
import org.knowm.xchange.hitbtc.v2.service.HitbtcAccountService;
import org.knowm.xchange.service.marketdata.MarketDataService;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class DbApi<CodecRegistry> {
	private MongoClient mongo;
	private MongoDatabase db;
	private MongoCollection<Ticker> table;
	private org.bson.codecs.configuration.CodecRegistry pojoCodecRegistry;
	private  final Logger log = LogManager.getLogger(DbApi.class);
    private Exchange hitbtc;
	private MarketDataService marketDataService;
	private List<CurrencyPair> symbols;
	private HitbtcAccountService accountService;
	
	public DbApi(String key, String secret) {
		ExchangeSpecification exSpec = new HitbtcExchange().getDefaultExchangeSpecification();
		exSpec.setApiKey(key);
		exSpec.setSecretKey(secret);
		this.hitbtc = ExchangeFactory.INSTANCE.createExchange(exSpec);
		this.marketDataService = this.hitbtc.getMarketDataService();
		this.symbols = this.hitbtc.getExchangeSymbols();
		this.accountService = (HitbtcAccountService) hitbtc.getAccountService();
		
		this.pojoCodecRegistry = fromRegistries(
                fromProviders(PojoCodecProvider.builder().register(Ticker.class, CurrencyPair.class).build()),
                MongoClient.getDefaultCodecRegistry());
		this.mongo = new MongoClient("localhost", MongoClientOptions.builder().codecRegistry(pojoCodecRegistry).build());
		this.db = this.mongo.getDatabase("trade");
		this.table = this.db.getCollection("trade",Ticker.class);
		try {
			this.table.drop();
		}catch(Exception e) { System.out.println(e.getMessage());}
		this.table = this.db.getCollection("trade",Ticker.class);
	}
	
	public MongoCollection<Ticker> getTable() {
		return this.table;
	}
	
	public ArrayList<HashMap<String, Object>> getStart() throws InvalidKeyException, NoSuchAlgorithmException, IOException {	
		List<HitbtcBalance> walletResponse = this.accountService.getMainBalance();
		ArrayList<HashMap<String,Object>> wv = new ArrayList<HashMap<String,Object>>();
		for(HitbtcBalance balance : walletResponse ) {		
			if (balance.getAvailable().doubleValue() > 0.0) {
				HashMap<String,Object> map = new HashMap<String, Object>();
				map.put("currency", balance.getCurrency());
				map.put("value", balance.getAvailable().doubleValue() );
				
				Reference r = new Reference(this.table);
    			r.setReference(Currency.BTC);
    			r.setReferenceOf( new Currency((String)map.get("currency")) );
    			r.setVolume(new BigFraction(((BigDecimal)map.get("value")).doubleValue()));				
				
				try {
					map.put("value_conv", r.getConvertedValue() );
				} catch (Exception e) {
					log.warn("getStart: "+ e.getMessage());
					break;
				}
				wv.add(map);
			}
		}
		
		wv.sort((HashMap<String,Object> z1, HashMap<String,Object> z2) -> {
			if (((BigFraction)z1.get("value_conv")).doubleValue()  > ((BigFraction)z2.get("value_conv")).doubleValue()) {
				return 1;
			}if (((BigFraction)z1.get("value_conv")).doubleValue() < ((BigFraction)z2.get("value_conv")).doubleValue()) {
				return -1;
			}
			return 0;
		});
		return wv;
	}
	
	public void getTickerInformation() throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException, ExchangeException, IOException {
		
		for (CurrencyPair symbol: symbols) {
			Ticker ticker = this.marketDataService.getTicker(symbol);
			table.insertOne(ticker);
=======
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.fraction.BigFraction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.knowm.xchange.kraken.KrakenExchange;
import org.knowm.xchange.service.account.AccountService;
import org.knowm.xchange.service.marketdata.MarketDataService;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class DbApi<CodecRegistry> {
	private MongoClient mongo;
	private MongoDatabase db;
	private MongoCollection<Ticker> table;
	private org.bson.codecs.configuration.CodecRegistry pojoCodecRegistry;
	private File apikey;
	private  final Logger log = LogManager.getLogger(DbApi.class);
	private  Exchange exchange = ExchangeFactory.INSTANCE.createExchange(KrakenExchange.class.getName());
	private  MarketDataService marketDataService;
	private  List<CurrencyPair> symbols;
	private  AccountService accountService;

	public DbApi(File apikey) throws IOException {
		this.apikey = apikey;

		this.pojoCodecRegistry = fromRegistries(
                fromProviders(PojoCodecProvider.builder().register(Ticker.class, TradePair.class).build()),
                MongoClient.getDefaultCodecRegistry());
		this.mongo = new MongoClient("localhost", MongoClientOptions.builder().codecRegistry(pojoCodecRegistry).build());
		this.db = this.mongo.getDatabase("trade");
		this.table = this.db.getCollection("trade",Ticker.class);
		try {
			this.table.drop();
		}catch(Exception e) { System.out.println(e.getMessage());}
		this.table = this.db.getCollection("trade",Ticker.class);	
		
		FileReader fileReader = new FileReader(this.apikey);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		StringBuffer stringBuffer = new StringBuffer();
		String line;
		while ((line = bufferedReader.readLine()) != null) {
			stringBuffer.append(line);
			stringBuffer.append("\n");
		}
		fileReader.close();
		String[] key = stringBuffer.toString().split("\n");	
		
		ExchangeSpecification exchangeSpecification = new ExchangeSpecification(KrakenExchange.class.getName());
		exchangeSpecification.setApiKey(key[0]);
		exchangeSpecification.setSecretKey(key[1]);
		exchange.applySpecification(exchangeSpecification);
		marketDataService = exchange.getMarketDataService();
		symbols = exchange.getExchangeSymbols();
		accountService = exchange.getAccountService();
	}
	
	public MongoCollection<Ticker> getTable() {
		return this.table;
	}
	
	public ArrayList<HashMap<String, Object>> getStart() throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException, ExchangeException, IOException {		
		Map<Currency, Balance> wallet = accountService.getAccountInfo().getWallet().getBalances();
				
		Iterator<Currency> keyIt = wallet.keySet().iterator();
		ArrayList<HashMap<String,Object>> wv = new ArrayList<HashMap<String,Object>>();
		
		while(keyIt.hasNext()) {
			Currency key = (Currency ) keyIt.next();
		
			if (wallet.get(key).getAvailable().doubleValue() > 0.0) {
				HashMap<String,Object> map = new HashMap<String, Object>();
				map.put("currency", key);
				map.put("value", wallet.get(key).getAvailable() );
				
				Reference r = new Reference(this.table);
    			r.setReference("BTC");
    			r.setReferenceOf(((Currency)map.get("currency")).getCurrencyCode());
    			r.setVolume(new BigFraction(((BigDecimal)map.get("value")).doubleValue()));				
				
				map.put("value_conv", r.getConvertedValue() );
				wv.add(map);
			}
		}
		
		wv.sort((HashMap<String,Object> z1, HashMap<String,Object> z2) -> {
			if (((BigFraction)z1.get("value_conv")).doubleValue()  > ((BigFraction)z2.get("value_conv")).doubleValue()) {
				return 1;
			}if (((BigFraction)z1.get("value_conv")).doubleValue() < ((BigFraction)z2.get("value_conv")).doubleValue()) {
				return -1;
			}
			return 0;
		});
		Collections.reverse(wv);	
		return wv;
	}
	
	public void getTickerInformation() throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException, ExchangeException, IOException  {
		for (CurrencyPair symbol: symbols) {			
			Ticker ticker = new Ticker();
			try {
				org.knowm.xchange.dto.marketdata.Ticker tk = marketDataService.getTicker(symbol);
//				ticker.setFeesRaw(assetPairs.getJSONObject(pairs.get(i)).getJSONArray("fees")  );
				ticker.setTickerAsk( tk.getAsk().doubleValue() );
				ticker.setTickerBid( tk.getBid().doubleValue() );
				ticker.setTradePair(new TradePair( symbol.base.getCurrencyCode(), symbol.counter.getCurrencyCode().split("\\.")[0] ));
				table.insertOne(ticker);
			}catch (Exception ex) {
				log.warn(ex.getMessage());
			}
		}
	}
}
