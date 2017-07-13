package CryptographSnake;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import org.jenetics.AnyGene;
import org.jenetics.Phenotype;


/**
 * 
 *
 */
public class App {	
	
    public static void main( String[] args ) {
    	String APIKEY = args[0];
    	
    	
    	KrakenDbApi api = new KrakenDbApi(new File(APIKEY));
    	System.out.println("\n"
    			+ "\n"
    			+ "			Welcome to the CryptocurrencyGraphSnake\n"
    			+ "			Developed by Shamanou van Leeuwen\n"
    			+ "\n"
    			+ "\n");
    	while(true) {
        	try {
    			api.getTickerInformation();
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    		
            System.out.println("			+-----------------------+\n"
            		+ "			GRABBING TRADE START VALUE\n"
            		+ "			+-----------------------+\n"
            		+ "\n");
        	int i = 0;
            while(true) {
            	try {
					ArrayList<HashMap<String, Object>> wallet = api.getStart("XXBT");
					HashMap<String, Object> start = wallet.get(i);
					System.out.println("			+-----------------------+\n"
							+ "			EVOLVING TRADE TRAJECTORY nr. " + String.valueOf(i) + " - "+ (String)start.get("currency") +"\n"
							+ "			+-----------------------+\n\n");
					i++;
					Evolve e = new Evolve(start, api.getTable());
					Phenotype<AnyGene<Ticker>, Double>  result = e.run();
					System.out.println("Results:\n"+ result + "\n");
					OrderExecutor orderExecutor = new OrderExecutor(api.getTable(), start,APIKEY);
					if (result.getFitness() > 0.0) {
						orderExecutor.setOrder(result);
						orderExecutor.ExecuteOrder();
					}
            	} catch (IOException | java.security.InvalidKeyException | NoSuchAlgorithmException | IndexOutOfBoundsException e ) {
            		System.out.println(e.getMessage());
            		i= 0;
				} 
            }
    	}
    }
}
