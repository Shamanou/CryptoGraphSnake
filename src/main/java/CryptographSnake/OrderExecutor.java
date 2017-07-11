package CryptographSnake;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;

import org.jenetics.AnyGene;
import org.jenetics.Chromosome;
import org.jenetics.Genotype;
import org.jenetics.Phenotype;
import org.json.JSONObject;

import com.mongodb.client.MongoCollection;

import edu.self.kraken.api.KrakenApi;
import edu.self.kraken.api.KrakenApi.Method;

public class OrderExecutor {
	private MongoCollection<Ticker> table;
	private HashMap<String,Object> start;
	private Phenotype<AnyGene<Ticker>, Double> order;
	private KrakenApi api = new KrakenApi();
	
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
				
        api.setKey(key[0]);
		api.setSecret(key[1]);
		
	}

	public void setOrder(Phenotype<AnyGene<Ticker>, Double> result) {
		this.order = result;
	}
	
	public void ExecuteOrder() throws InvalidKeyException, NoSuchAlgorithmException, IOException {
		Genotype<AnyGene<Ticker>> gt = this.order.getGenotype();
		Chromosome<AnyGene<Ticker>> chrom = gt.getChromosome();
		Iterator<AnyGene<Ticker>> it = chrom.iterator();
		String inval = (String) this.start.get("currency");
		while(it.hasNext()) {
			Ticker val = it.next().getAllele();
			System.out.print(val.getTradePair().getBase() + val.getTradePair().getQuote() +"	");
//			System.out.println(this.api.queryPrivate(Method.BALANCE));
			if (inval.equals(val.getTradePair().getQuote())) {
				HashMap<String, String> parameters = new HashMap<String,String>();
				parameters.put("type", "buy");
				parameters.put("ordertype", "market");
				parameters.put("pair", val.getTradePair().getBase() + val.getTradePair().getQuote());
				parameters.put("volume", String.valueOf((new JSONObject(this.api.queryPrivate(Method.BALANCE)).getJSONObject("result").getDouble(val.getTradePair().getQuote()))) );
				parameters.put("oflags", "viqc");
				System.out.print(this.api.queryPrivate(Method.ADD_ORDER, parameters));
				inval = val.getTradePair().getBase();
			} else if (inval.equals(val.getTradePair().getBase())) {
				HashMap<String, String> parameters = new HashMap<String,String>();
				parameters.put("type", "sell");
				parameters.put("ordertype", "market");
				parameters.put("pair", val.getTradePair().getBase() + val.getTradePair().getQuote());
				parameters.put("volume", String.valueOf((new JSONObject(this.api.queryPrivate(Method.BALANCE)).getJSONObject("result").getDouble(val.getTradePair().getBase()))) );
				System.out.print(this.api.queryPrivate(Method.ADD_ORDER, parameters));
				inval = val.getTradePair().getQuote();
			} 
			System.out.print("\n");
			double tmp = 0;
			while(tmp <= 0.0) {
				if (inval.equals(val.getTradePair().getBase())) {
					tmp =(new JSONObject(this.api.queryPrivate(Method.BALANCE)).getJSONObject("result").getDouble(val.getTradePair().getQuote()));
				}else {
					tmp =(new JSONObject(this.api.queryPrivate(Method.BALANCE)).getJSONObject("result").getDouble(val.getTradePair().getBase()));
				}
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
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
