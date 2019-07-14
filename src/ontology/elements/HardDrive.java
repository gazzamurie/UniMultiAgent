package ontology.elements;

import java.util.List;

import jade.content.onto.annotations.*;

public class HardDrive extends Item {
	private String name;
	
	@Slot(mandatory = true)
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
}
