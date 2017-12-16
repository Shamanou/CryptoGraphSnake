package CryptographSnake;

public class TradePair {
	private String base;
	private String quote;
	
	public TradePair() {}
	
	public TradePair(String base, String quote) {
		this.base = base;
		this.quote = quote;
	}
	
	public String getBase() {
		return base;
	}
	public void setBase(String base) {
		this.base = base;
	}
	public String getQuote() {
		return quote;
	}
	public void setQuote(String quote) {
		this.quote = quote;
	}
}
