package CryptographSnake;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.jenetics.AnyGene;
import org.jenetics.Phenotype;
import org.json.JSONException;
import org.knowm.xchange.currency.Currency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class App {	
	
    public static void main( String[] args ) throws InvalidKeyException {
        final Logger log = LoggerFactory.getLogger(App.class);
    	DbApi api = null;
    	
    	String key = args[0];
    	String secret = args[1];    	
		try {
			api = new DbApi(key, secret);
		} catch (JSONException | IOException e) {
			e.printStackTrace();
		}

    	log.info("\n"
    			+ "\n"
    			+ "			Welcome to the CryptocurrencyGraphSnake\n"
    			+ "			Developed by Shamanou van Leeuwen\n"
    			+ "\n"
    			+ "\n");
    	while(true) {
				try {
					api.getTickerInformation();
				} catch (JSONException | IOException e1) {
					e1.printStackTrace();
				}
    		
            log.info("\n			+-----------------------+\n"
            		+ "			GRABBING TRADE START VALUE\n"
            		+ "			+-----------------------+\n"
            		+ "\n");
        	int i = 0;
            while(i >= 0) {
            	try {
            		ArrayList<HashMap<String, Object>> wallet = api.getStart();
            		
					HashMap<String, Object> start = wallet.get(i);
					OrderExecutor orderExecutor = new OrderExecutor(api.getTable(), start, key, secret);
					
					log.info("\n			+-----------------------+\n"
							+ "			EVOLVING TRADE TRAJECTORY nr. " + String.valueOf(i) + " - "+ ((Currency)start.get("currency")).getDisplayName() +"\n"
							+ "			+-----------------------+\n\n");
					i++;
					Evolve e = new Evolve(start, api.getTable());
					Phenotype<AnyGene<Ticker>, Double>  result = e.run();
					String resultString = "";
					Iterator<AnyGene<Ticker>> chromit = result.getGenotype().getChromosome().iterator();
					
					while(chromit.hasNext()) {
						Ticker val = chromit.next().getAllele();
						resultString += val.getTradePair().getBase()+val.getTradePair().getQuote() + "\n"; 
					}
					
					
					log.info("Results:\n"+ resultString + "\n");
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
