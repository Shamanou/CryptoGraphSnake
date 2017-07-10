package CryptographSnake;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import org.jenetics.AnyGene;
import org.jenetics.Phenotype;


/**
 * 
 *
 */
public class App {	
	
    public static void main( String[] args ) {
    	KrakenDbApi<?> api = new KrakenDbApi(new File("src/main/java/CryptographSnake/apikey"));
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
            while(true) {
            for(int i = 0; i < 4; i++) {
            	try {
					HashMap<String, Object> start = api.getStart("ZUSD", i);
					
					System.out.println("			+-----------------------+\n"
							+ "			EVOLVING TRADE TRAJECTORY nr. " + String.valueOf(i) + "\n"
							+ "			+-----------------------+\n\n");
					Evolve e = new Evolve(start, api.getTable());
					Phenotype<AnyGene<Ticker>, Double>  result = e.run();
					System.out.println("Results:\n"+ result + "\n");
					OrderExecutor orderExecutor = new OrderExecutor(api.getTable(), start);
					orderExecutor.setOrder(result);
					orderExecutor.ExecuteOrder();
            	} catch (Exception e) {
					i = -1;
				}
            }
            }
    	}
    }
}
