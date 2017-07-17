package CryptographSnake;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jenetics.AnyGene;
import org.jenetics.Phenotype;
import org.knowm.xchange.currency.Currency;


/**
 * 
 *
 */
public class App {	
	
    public static void main( String[] args ) {
    	String APIKEY = args[0];
        final Logger log = LogManager.getLogger(App.class);
    	
    	DbApi api = null;
		try {
			api = new DbApi(new File(APIKEY));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
    	log.debug("\n"
    			+ "\n"
    			+ "			Welcome to the CryptocurrencyGraphSnake\n"
    			+ "			Developed by Shamanou van Leeuwen\n"
    			+ "\n"
    			+ "\n");
    	while(true) {
        	try {
				api.getTickerInformation();
			} catch (org.knowm.xchange.exceptions.ExchangeException | org.knowm.xchange.exceptions.NotYetImplementedForExchangeException 
					| IOException e1) {
				e1.printStackTrace();
			}
    		
            log.debug("\n			+-----------------------+\n"
            		+ "			GRABBING TRADE START VALUE\n"
            		+ "			+-----------------------+\n"
            		+ "\n");
        	int i = 0;
            while(i >= 0) {
            	try {
            		ArrayList<HashMap<String, Object>> wallet = api.getStart();
            		
					HashMap<String, Object> start = wallet.get(i);
					log.debug("\n			+-----------------------+\n"
							+ "			EVOLVING TRADE TRAJECTORY nr. " + String.valueOf(i) + " - "+ ((Currency)start.get("currency")).getCurrencyCode() +"\n"
							+ "			+-----------------------+\n\n");
					i++;
					Evolve e = new Evolve(start, api.getTable());
					Phenotype<AnyGene<Ticker>, Double>  result = e.run();
					log.debug("Results:\n"+ result + "\n");
					OrderExecutor orderExecutor = new OrderExecutor(api.getTable(), start,APIKEY);
					if (result.getFitness() > 0.0) {
						orderExecutor.setOrder(result);
						orderExecutor.ExecuteOrder();
					}
            	} catch (IndexOutOfBoundsException | IOException | InvalidKeyException | NoSuchAlgorithmException ex ) {
            		log.warn(ex.getMessage());
            		i= -1;
				} 
            }
    	}
    }
}
