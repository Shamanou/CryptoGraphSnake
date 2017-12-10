package CryptographSnake;

import java.io.IOException;
import java.math.RoundingMode;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.knowm.xchange.dto.account.Wallet;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.hitbtc.HitbtcExchange;
import org.knowm.xchange.hitbtc.v2.service.HitbtcAccountService;
import org.knowm.xchange.hitbtc.v2.service.HitbtcTradeService;
import org.knowm.xchange.hitbtc.v2.service.HitbtcTradeService.HitbtcTradeHistoryParams;
import org.knowm.xchange.service.marketdata.MarketDataService;

import com.mongodb.client.MongoCollection;


public class OrderExecutor {
	private MongoCollection<Ticker> table;
	private HashMap<String,Object> start;
	private Phenotype<AnyGene<Ticker>, Double> order;
	private final Logger log = LogManager.getLogger(OrderExecutor.class);
    private Exchange hitbtc;
	private HitbtcAccountService accountService;
	private HitbtcTradeService tradeService;
	
	public OrderExecutor(MongoCollection<Ticker> table, HashMap<String,Object> start, String key , String secret) throws IOException {
		this.start = start;
		this.setTable(table);
		
		ExchangeSpecification exSpec = new HitbtcExchange().getDefaultExchangeSpecification();
		exSpec.setApiKey(key);
		exSpec.setSecretKey(secret);
		this.hitbtc = ExchangeFactory.INSTANCE.createExchange(exSpec);
		this.accountService = (HitbtcAccountService) hitbtc.getAccountService();
        
	}

	public void setOrder(Phenotype<AnyGene<Ticker>, Double> result) {
		this.order = result;
	}
	
	public void ExecuteOrder() throws InvalidKeyException, NoSuchAlgorithmException, IOException {
		Genotype<AnyGene<Ticker>> gt = this.order.getGenotype();
		Chromosome<AnyGene<Ticker>> chrom = gt.getChromosome();
		Iterator<AnyGene<Ticker>> it = chrom.iterator();
		Currency inval =  new Currency((String)this.start.get("currency"));
		Wallet wallets = accountService.getAccountInfo().getWallet();
		
		while(it.hasNext()) {
			Ticker val = it.next().getAllele();
			OrderType tradeType = null;
			DecimalFormat df = new DecimalFormat("#.#####");
			df.setRoundingMode(RoundingMode.CEILING);

			if (inval.equals(val.getCurrencyPair().counter)) {
				tradeType = OrderType.ASK;
				inval = val.getCurrencyPair().base;
			} else if (inval.equals(val.getCurrencyPair().base)) {
				inval = val.getCurrencyPair().counter;
				tradeType = OrderType.BID;
			} 
			HitbtcTradeHistoryParams params = (HitbtcTradeHistoryParams) this.tradeService.createOpenOrdersParams();
			params.setCurrencyPair(val.getCurrencyPair());
			MarketOrder marketOrder = new MarketOrder(tradeType, wallets.getBalance(inval).getAvailable(), val.getCurrencyPair());			
			String orderResult = this.tradeService.placeMarketOrder(marketOrder);
			log.debug(orderResult);
			double openOrders;
			try {
				openOrders = this.tradeService.getOpenOrders().getOpenOrders().size();
			}catch(Exception e) {
				openOrders = 0;
			}
			while(openOrders > 0) {
				try {
					openOrders = this.tradeService.getOpenOrders().getOpenOrders().size();
					Thread.sleep(80);
				} catch (InterruptedException e) {
					log.warn(e.getMessage());
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
