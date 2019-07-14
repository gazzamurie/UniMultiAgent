package coursework;

import java.util.ArrayList;
import java.util.HashMap;

import ontology.ECommerceOntology;
import ontology.elements.*;
import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class OnSellerAgent extends Agent {
	private Codec codec = new SLCodec();
	private Ontology ontology = ECommerceOntology.getInstace();
	// Stock list, with serial number as key
	private AID DayTicker;
	private ArrayList<AID> buyers = new ArrayList<>();
	private ArrayList<String> parts = new ArrayList<>();
	private HashMap<Integer,Item> itemsForSale = new HashMap<>(); 
	private HashMap<String, Integer> WareHouse = new HashMap<String, Integer>();
	private HashMap<String, Integer> price = new HashMap<String, Integer>();
	private int profit = 0;
	
	protected void setup() {
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);
		
		Type type = new Type();
		Memory mem = new Memory();
		type.setName("Desktop");
		type.setSerialNumber(123);
		mem.setName("8GB");
		
		WareHouse.put("Desktop",10);
		WareHouse.put("Laptop",10);
		WareHouse.put("8GB",3);
		WareHouse.put("16GB",4);
		WareHouse.put("1TB",5);
		WareHouse.put("2TB",6);
		WareHouse.put("lapMotherBoard",7);
		WareHouse.put("deskMotherBoard",8);
		
		price.put("Desktop", 300);
		price.put("Laptop", 300);
		
		

		itemsForSale.put(type.getSerialNumber(),type);
		
		
		// Add Agent To Yellow Pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("manufacturer");
		sd.setName(getLocalName() + "-manseller-agent");
		dfd.addServices(sd);
		
		try {
			DFService.register(this, dfd);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
		
		addBehaviour(new TickerWaiter(this));
		
	}
	protected void takeDown()
	{
		try
		{
			DFService.deregister(this);
		}
		catch(FIPAException e)
		{
			e.printStackTrace();
		}
	}

	public class TickerWaiter extends CyclicBehaviour {

		public TickerWaiter(Agent a) {
			super(a);
		}

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.or(
					MessageTemplate.MatchContent("new day"),
					MessageTemplate.MatchContent("terminate"));
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				if (DayTicker == null) {
					DayTicker = msg.getSender();
				}
				if (msg.getContent().equals("new day")) {
					myAgent.addBehaviour(new findBuyer(myAgent));
					myAgent.addBehaviour(new ReceiverBehaviour(myAgent));
					myAgent.addBehaviour(new QueryBehaviour());
					myAgent.addBehaviour(new SellBehaviour());
					myAgent.addBehaviour(new EndDayListener(myAgent));
				} else {
					// Termination Message To End
					myAgent.doDelete();
				}
			} else {
				block();
			}
			
		}
		
		public class findBuyer extends OneShotBehaviour {
			public findBuyer(Agent a) {
				super(a);
			}

			@Override
			public void action() {
				DFAgentDescription buyerTemplate = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				
				sd.setType("customer");
				buyerTemplate.addServices(sd);
				try {
					buyers.clear();
					DFAgentDescription[] agentsType1 = DFService.search(
							myAgent, buyerTemplate);
					for (int i = 0; i < agentsType1.length; i++) {
						buyers.add(agentsType1[i].getName());
					}
				} catch (FIPAException e) {
					e.printStackTrace();
				}
			}

//			private char[] count(String string) {
//				return null;
//			}
		}
	
	

	public class ReceiverBehaviour extends CyclicBehaviour
	{
	public ReceiverBehaviour(Agent a)
	{
		super(a);
		parts.clear();
	}
	
	@Override
	public void action()
	{
		//Try to receive message
		ACLMessage msg = myAgent.receive();
		if(msg != null)
		{
			parts.add(msg.getContent());
		}
		else
		{
			//Put behaviour to sleep until message arrives
			block();
		}
	}
	}
	}
	private class QueryBehaviour extends CyclicBehaviour{
		@Override
		public void action() {
			//This behaviour should only respond to QUERY_IF messages
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF); 
			ACLMessage msg = receive(mt);
			if(msg != null){
				try {
					ContentElement ce = null;
//					System.out.println(msg.getContent()); //print out the message content in SL

					// Let JADE convert from String to Java objects
					// Output will be a ContentElement
					ce = getContentManager().extractContent(msg);
					if (ce instanceof Owns) {
						Owns owns = (Owns) ce;
						Item it = owns.getItem();
						Type type = (Type)it;
//						System.out.println("The Device " + type.getName());
						
//						System.out.println(WareHouse);
//						System.out.println(WareHouse.get("8GB"));
//						if(WareHouse.get("Desktop")==0)
						
//						check if seller has it in stock
						if(WareHouse.containsKey(type.getName()) && WareHouse.get(type.getName())>0) {
							System.out.println("I have the PC in stock!");
						}
						else {
							System.out.println("PC is out of stock");
						}
					}
				}

				catch (CodecException ce) {
					ce.printStackTrace();
				}
				catch (OntologyException oe) {
					oe.printStackTrace();
				}

			}
			else{
				block();
				
			}
			myAgent.removeBehaviour(this);
		}
	}
	
	
	private class SellBehaviour extends CyclicBehaviour{
		@Override
		public void action() {
			//This behaviour should only respond to REQUEST messages
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST); 
			ACLMessage msg = receive(mt);
			if(msg != null){
				try {
					ContentElement ce = null;
//					System.out.println(msg.getContent()); //print out the message content in SL

					// Let JADE convert from String to Java objects
					// Output will be a ContentElement
					ce = getContentManager().extractContent(msg);
					if(ce instanceof Action) {
						Concept action = ((Action)ce).getAction();
						if (action instanceof Sell) {
							Sell order = (Sell)action;
							Item it = order.getItem();
							// Extract the CD name and print it to demonstrate use of the ontology
							if(it instanceof Type){
								Type type = (Type)it;
								//check if seller has it in stock
								
							
//								System.out.println("THIS IS WHAT? " + type.getName());
							if(WareHouse.containsKey(type.getName()) && WareHouse.get(type.getName())>0 && type.getPrice()>=price.get(type.getName())) {	
//									System.out.println("Selling Product " + type.getName());
//									System.out.println(price.get(type.getName()));
									profit = profit + type.getPrice();
									System.out.println(" PROFIT :" + profit);
							
									System.out.println(WareHouse); // print the List.
									for (int i = 0; i < WareHouse.get(type.getName()); i++) {
									  Integer v = WareHouse.get(type.getName()); // get the element.
									  v = v - 1; // Update the value.
									  WareHouse.replace(type.getName(), v); // Update the List.
									}
									
									
									WareHouse.get(type.getName());
									System.out.println("[WAREHOUSE PARTS] " + WareHouse);
										
								}
								else {
									System.out.println("The product is either out of stock or the money propoesed does not cover costs.");
									
								}

							}
							else{
								System.out.println("WHOOPS");
							}
						}
						else{
							System.out.println("WHOOPS");
						}

					}
				}

				catch (CodecException ce) {
					ce.printStackTrace();
				}
				catch (OntologyException oe) {
					oe.printStackTrace();
				}

			}
			else{
				block();
			}
			myAgent.removeBehaviour(this);
		}
	
	}

	public class EndDayListener extends CyclicBehaviour {
		
		public EndDayListener(Agent a) {
			super(a);
		}

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchContent("done");
			ACLMessage msg = myAgent.receive(mt);

			ACLMessage tick = new ACLMessage(ACLMessage.INFORM);
			tick.addReceiver(DayTicker);
			tick.setContent("done");
			System.out.println("DONE MESSAGE SELLER");
			myAgent.send(tick);

			myAgent.removeBehaviour(this);	
		}
	}
}

