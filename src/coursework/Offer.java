package coursework;

import jade.core.AID;

public class Offer {
	private AID seller;
	private int price;
	private int profit;
	
	public Offer(AID seller, int price)
	{
		super();
		this.seller = seller;
		this.price = price;
	}
	
	public AID getSeller()
	{
		return seller;
	}
	
	public int getPrice()
	{
		return price;
	}
}
