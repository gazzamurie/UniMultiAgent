package ontology;

import jade.content.onto.BeanOntology;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;

public class ECommerceOntology extends BeanOntology{
	private static Ontology theInstance = new ECommerceOntology("my_ontology");
	
	public static Ontology getInstace()
	{
		return theInstance;
	}
	
	//Singleton Pattern
	private ECommerceOntology(String name)
	{
		super(name);
		try
		{
			add("ontology.elements");
		}
		catch(BeanOntologyException e)
		{
			e.printStackTrace();
		}
	}
}
