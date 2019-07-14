package ontology.elements;

import java.util.List;

import jade.content.onto.annotations.*;

public class Type extends Item {
	private String name;
	private int price;
	
	@Slot(mandatory = true)
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	@Slot(mandatory = true)
	public int getPrice()
	{
		return price;
	}
	
	public void setPrice(int price)
	{
		this.price = price;
	}
}