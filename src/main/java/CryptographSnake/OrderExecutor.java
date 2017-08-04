package CryptographSnake;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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
import org.knowm.xchange.dto.Order.IOrderFlags;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.kraken.KrakenExchange;
import org.knowm.xchange.kraken.dto.trade.KrakenOrderFlags;
import org.knowm.xchange.service.account.AccountService;
import org.knowm.xchange.service.trade.TradeService;

import com.mongodb.client.MongoCollection;

public class OrderExecutor {
	private MongoCollection<Ticker> table;
	private HashMap<String,Object> start;
	private Phenotype<AnyGene<Ticker>, Double> order;
	private final Logger log = LogManager.getLogger(OrderExecutor.class);
	private  Exchange kraken = ExchangeFactory.INSTANCE.createExchange(KrakenExchange.class.getName());
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
			Set<IOrderFlags> flags =  new HashSet<IOrderFlags>();
			flags.add(KrakenOrderFlags.VIQC);
			
			log.debug(accountService.getAccountInfo().getWallet().getBalance(
					new Currency(inval)).getTotal());
			
			if (inval.equals(val.getTradePair().getQuote())) {					
				order = new MarketOrder(OrderType.BID,accountService.getAccountInfo().getWallet().getBalance(
						new Currency(inval)).getTotal(), 
						new CurrencyPair( val.getTradePair().getBase(), val.getTradePair().getQuote()));
				order.setOrderFlags(flags);
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
				log.warn("Could not execute order ("+ val.getTradePair().getBase() + " - " + val.getTradePair().getQuote() +"): "+ ex.getMessage());
				break;
			}
			
			log.debug("\n");
			double tmp = 0;
			int i = 0;
			while(tmp <= 0.0) {
				i++;
				if (inval.equals(val.getTradePair().getBase())) {
					tmp = accountService.getAccountInfo().getWallet().getBalances().get(val.getTradePair().getBase()).getAvailable().doubleValue();
				}else {
					tmp = accountService.getAccountInfo().getWallet().getBalances().get(val.getTradePair().getQuote()).getAvailable().doubleValue();
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
