package CryptographSnake;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.math3.fraction.BigFraction;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.json.JSONObject;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import edu.self.kraken.api.KrakenApi;
import edu.self.kraken.api.KrakenApi.Method;


public class KrakenDbApi<CodecRegistry> {
	private final String USER_AGENT = "Mozilla/5.0";
	private MongoClient mongo;
	private MongoDatabase db;
	private MongoCollection<Ticker> table;
	private org.bson.codecs.configuration.CodecRegistry pojoCodecRegistry;
	private File apikey;
	
	
	public KrakenDbApi(File apikey) {
		this.apikey = apikey;
		this.pojoCodecRegistry = fromRegistries(
                fromProviders(PojoCodecProvider.builder().register(Ticker.class, TradePair.class).build()),
                MongoClient.getDefaultCodecRegistry());
		this.mongo = new MongoClient("localhost", MongoClientOptions.builder().codecRegistry(pojoCodecRegistry).build());
		this.db = this.mongo.getDatabase("trade");
		this.table = this.db.getCollection("trade",Ticker.class);
		try {
			this.table.drop();
		}catch(Exception e) {}
		this.table = this.db.getCollection("trade",Ticker.class);		
	}
	
	public MongoCollection<Ticker> getTable() {
		return this.table;
	}
	
	private JSONObject request(String url, String method) throws IOException {
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		con.setRequestMethod(method);
			
		con.setRequestProperty("User-Agent", USER_AGENT);

		int responseCode = con.getResponseCode();
		System.out.println("\nSending '"+ method +"' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		return new JSONObject(response.toString()).getJSONObject("result");
	}
	
	private JSONObject request(String url, String method, String data) throws IOException {
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		con.setRequestMethod(method);
		con.setRequestProperty("User-Agent", USER_AGENT);

		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(data);
		wr.flush();
		wr.close();
		
		int responseCode = con.getResponseCode();
		System.out.println("\nSending '"+ method +"' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		return new JSONObject(response.toString()).getJSONObject("result");
	}
	
	private List<String> getCurrencyPairs() throws IOException {
		String url = "https://api.kraken.com/0/public/AssetPairs";
		return IteratorUtils.toList(this.request(url,"GET").keys());
	}
	
	public ArrayList<HashMap<String, Object>> getStart(String REFERENCE) throws IOException, java.security.InvalidKeyException, NoSuchAlgorithmException {
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
		
		
        KrakenApi api = new KrakenApi();
        api.setKey(key[0]);
		api.setSecret(key[1]);
       
        String response = api.queryPrivate(Method.BALANCE);
		
		JSONObject wallet = new JSONObject(response).getJSONObject("result");
		Iterator<String> walletKeys = wallet.keys();
		ArrayList<HashMap<String, Object>> walletValues = new ArrayList<HashMap<String, Object>>(); 
		while(walletKeys.hasNext()) {
			String walletKey = walletKeys.next();
			double walletValue = wallet.getDouble(walletKey);
			if (walletValue > 0.0) {
				HashMap<String,Object> map = new HashMap<String, Object>();
				map.put("currency", walletKey);
				map.put("volume", new BigFraction(walletValue));
				walletValues.add(map);
			}
		}

		walletValues.sort((HashMap<String,Object> z1, HashMap<String,Object> z2) -> {
			if (((BigFraction)z1.get("volume")).doubleValue() > ((BigFraction)z2.get("volume")).doubleValue())
				return 1;
			if (((BigFraction)z1.get("volume")).doubleValue() < ((BigFraction)z2.get("volume")).doubleValue())
				return -1;
			return 0;
		});
//		System.out.println("before - " + walletValues);
		Collections.reverse(walletValues);
//		System.out.println("after - " + walletValues);
		return walletValues;
	}
	
	public void getTickerInformation() throws IOException{
		String pairsPostData = "";
		List<String> pairs = this.getCurrencyPairs();
		for(int i = 0; i < pairs.size() ;i++) {
			pairsPostData += pairs.get(i) + ",";
		}
		pairsPostData = pairsPostData.substring(0, pairsPostData.length() - 1);
		JSONObject tickers = this.request("https://api.kraken.com/0/public/Ticker", "POST", "pair="+pairsPostData);
		JSONObject assetPairs = this.request("https://api.kraken.com/0/public/AssetPairs", "POST", pairsPostData);
		for(int i = 0; i < pairs.size() ;i++) {
			if (!pairs.get(i).contains(".")) {
				Ticker ticker = new Ticker();
				ticker.setFeesRaw(assetPairs.getJSONObject(pairs.get(i)).getJSONArray("fees")  );
				ticker.setTickerAsk( tickers.getJSONObject(pairs.get(i)).getJSONArray("a").getDouble(0) );
				ticker.setTickerBid( tickers.getJSONObject(pairs.get(i)).getJSONArray("b").getDouble(0) );
				ticker.setFeesVolumeCurrency( assetPairs.getJSONObject(pairs.get(i)).getString("fee_volume_currency")  );
					if (pairs.get(i).substring(0,1).equals("X")) {
						ticker.setTradePair(new TradePair(pairs.get(i).substring(0,4), pairs.get(i).substring(4,8)));	
					} else if(pairs.get(i).substring(0,4).equals("USDT")){
						ticker.setTradePair(new TradePair(pairs.get(i).substring(0,4), pairs.get(i).substring(4,8)));
					}else if (pairs.get(i).substring(0,4).equals("DASH")){
						ticker.setTradePair(new TradePair(pairs.get(i).substring(0,4), pairs.get(i).substring(4,7)));
					} else {
						ticker.setTradePair(new TradePair(pairs.get(i).substring(0,3), pairs.get(i).substring(3,6)));
					}
				table.insertOne(ticker);
			}
		}
	}
}
