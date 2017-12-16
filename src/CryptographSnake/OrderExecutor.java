package CryptographSnake;

import java.io.IOException;
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
import org.knowm.xchange.hitbtc.HitbtcExchange;
import org.knowm.xchange.service.account.AccountService;
import org.knowm.xchange.service.trade.TradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mongodb.client.MongoCollection;

public class OrderExecutor {
    private MongoCollection<Ticker> table;
    private HashMap<String, Object> start;
    private Phenotype<AnyGene<Ticker>, Double> order;
    private final Logger log = LoggerFactory.getLogger(OrderExecutor.class);
    private Exchange exchange = ExchangeFactory.INSTANCE.createExchange(HitbtcExchange.class.getName());
    private TradeService tradeService;
    private AccountService accountService;


    public OrderExecutor(MongoCollection<Ticker> table, HashMap<String, Object> start, String k, String s) throws IOException {
        this.start = start;
        this.setTable(table);

        ExchangeSpecification exchangeSpecification = new ExchangeSpecification(HitbtcExchange.class.getName());
        exchangeSpecification.setApiKey(k);
        exchangeSpecification.setSecretKey(s);
        exchange.applySpecification(exchangeSpecification);
        tradeService = exchange.getTradeService();
        accountService = exchange.getAccountService();
    }

    public void setOrder(Phenotype<AnyGene<Ticker>, Double> result) {
        this.order = result;
    }

    public void ExecuteOrder() throws InvalidKeyException, NoSuchAlgorithmException, IOException {
        Genotype<AnyGene<Ticker>> gt = this.order.getGenotype();
        Chromosome<AnyGene<Ticker>> chrom = gt.getChromosome();
        Iterator<AnyGene<Ticker>> it = chrom.iterator();
        String inval = ((Currency) this.start.get("currency")).getCurrencyCode();
        while (it.hasNext()) {
            Ticker val = it.next().getAllele();
            MarketOrder order = null;

            log.debug(accountService.getAccountInfo().getWallet().getBalance(
                    new Currency(inval)).getTotal().toPlainString());

            if (inval.equals(val.getTradePair().getQuote())) {
                order = new MarketOrder(OrderType.BID, accountService.getAccountInfo().getWallet().getBalance(
                        new Currency(inval)).getTotal(),
                        new CurrencyPair(val.getTradePair().getBase(), val.getTradePair().getQuote()));
                inval = val.getTradePair().getBase();
            } else if (inval.equals(val.getTradePair().getBase())) {
                order = new MarketOrder(OrderType.ASK, accountService.getAccountInfo().getWallet().getBalance(
                        new Currency(inval)).getTotal(),
                        new CurrencyPair(val.getTradePair().getBase(), val.getTradePair().getQuote()));
                inval = val.getTradePair().getQuote();
            }
            try {
                this.tradeService.placeMarketOrder(order);
                log.info(val.getTradePair().getBase() + " - " + val.getTradePair().getQuote() + " trade executed");
            } catch (Exception ex) {
                log.warn("Could not execute order (" + val.getTradePair().getBase() + " - " + val.getTradePair().getQuote() + "): " + ex.getMessage());
                break;
            }

            log.debug("\n");
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                log.warn(e.getMessage());
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
