package CryptographSnake;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jenetics.AnyGene;
import org.jenetics.Chromosome;
import org.jenetics.Genotype;
import org.jenetics.Phenotype;
import org.json.JSONObject;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.kraken.KrakenExchange;
import org.knowm.xchange.service.account.AccountService;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.knowm.xchange.service.trade.TradeService;

import com.mongodb.client.MongoCollection;

import edu.self.kraken.api.KrakenApi;
import edu.self.kraken.api.KrakenApi.Method;

public class OrderExecutor {
	private MongoCollection<Ticker> table;
	private HashMap<String,Object> start;
	private Phenotype<AnyGene<Ticker>, Double> order;
	private KrakenApi api = new KrakenApi();
	private final Logger log = LogManager.getLogger(OrderExecutor.class);
	private  Exchange kraken = ExchangeFactory.INSTANCE.createExchange(KrakenExchange.class.getName());
	private  MarketDataService marketDataService;
	private TradeService tradeService;
	private  AccountService accountService;

	
	public OrderExecutor(MongoCollection<Ticker> table, HashMap<String,Object> start, String k) throws IOException {
		this.start = start;
		this.setTable(table);
		
		FileReader fileReader = new FileReader(k);
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
		kraken.applySpecification(exchangeSpecification);
		tradeService = kraken.getTradeService();
		marketDataService = kraken.getMarketDataService();
		accountService = kraken.getAccountService();		
	}

	public void setOrder(Phenotype<AnyGene<Ticker>, Double> result) {
		this.order = result;
	}
	
	public void ExecuteOrder() throws InvalidKeyException, NoSuchAlgorithmException, IOException {
		Genotype<AnyGene<Ticker>> gt = this.order.getGenotype();
		Chromosome<AnyGene<Ticker>> chrom = gt.getChromosome();
		Iterator<AnyGene<Ticker>> it = chrom.iterator();
		String inval =  ((Currency)this.start.get("currency")).getCurrencyCode();
		while(it.hasNext()) {
			Ticker val = it.next().getAllele();
			MarketOrder order = null;
			if (inval.equals(val.getTradePair().getQuote())) {
				
				log.debug(accountService.getAccountInfo().getWallet().getBalance(
						new Currency(inval)).getTotal());
				
				order = new MarketOrder(OrderType.BID,accountService.getAccountInfo().getWallet().getBalance(
						new Currency(inval)).getTotal(), 
						new CurrencyPair( val.getTradePair().getBase(), val.getTradePair().getQuote()));
				inval = val.getTradePair().getBase();
			} else if (inval.equals(val.getTradePair().getBase())) {
				order = new MarketOrder(OrderType.ASK,accountService.getAccountInfo().getWallet().getBalance(
						new Currency(inval)).getTotal(), 
						new CurrencyPair( val.getTradePair().getBase(), val.getTradePair().getQuote()));
				inval = val.getTradePair().getQuote();
			} 
			try {
				this.tradeService.placeMarketOrder(order);
			} catch(Exception ex) {
				log.warn(ex.getMessage());
				break;
			}
			
			log.debug("\n");
			double tmp = 0;
			int i = 0;
			while(tmp <= 0.0) {
				i++;
				if (inval.equals(val.getTradePair().getBase())) {
					tmp =(new JSONObject(this.api.queryPrivate(Method.BALANCE)).getJSONObject("result").getDouble(val.getTradePair().getBase()));
				}else {
					tmp =(new JSONObject(this.api.queryPrivate(Method.BALANCE)).getJSONObject("result").getDouble(val.getTradePair().getQuote()));
				}
				try {
					Thread.sleep(80);
				} catch (InterruptedException e) {
					log.warn(e.getMessage());;
				}
				if (i >= 5) {
					break;
				}

			}
		}
		
	}

	public MongoCollection<Ticker> getTable() {
		return table;
	}

	public void setTable(MongoCollection<Ticker> table) {
		this.table = table;
	}
}
