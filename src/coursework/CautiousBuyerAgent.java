package coursework;


import java.util.ArrayList;

import java.util.Random;

import jade.content.onto.basic.Action;
import ontology.ECommerceOntology;
import ontology.elements.*;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class CautiousBuyerAgent extends Agent {

	private Codec codec = new SLCodec();
	private Ontology ontology = ECommerceOntology.getInstace();
	private AID sellerAID;
	private AID DayTicker;
	private ArrayList<String> pcBuilder = new ArrayList<>();
	private ArrayList<String> sendlist = new ArrayList<>();
	private ArrayList<AID> sellers = new ArrayList<>();

	protected void setup() {
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);

		sellerAID = new AID("seller",AID.ISLOCALNAME);

		//Add This Agent To The Yellow Pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("customer");
		sd.setName(getLocalName() + "-customer-agent");
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
					// Spawn New Sequential Behaviour For Days Activated
					SequentialBehaviour dailyActivity = new SequentialBehaviour();
					// SubBehaviour Will Execute In the Order They Are Added
					dailyActivity.addSubBehaviour(new findSeller(myAgent));
					dailyActivity.addSubBehaviour(new QueryBuyerBehaviour(myAgent));
					dailyActivity.addSubBehaviour(new endDay(myAgent));
					myAgent.addBehaviour(dailyActivity);
				} else {
					myAgent.doDelete();
				}
			} else {
				block();
			}
		}
	

		public class findSeller extends OneShotBehaviour {
			public findSeller(Agent a) {
				super(a);
			}

			@Override
			public void action() {
				DFAgentDescription sellerTemplate = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("manufacturer");
				sellerTemplate.addServices(sd);
				try {
					sellers.clear();
					DFAgentDescription[] agentsType1 = DFService.search(
							myAgent, sellerTemplate);
					for (int i = 0; i < agentsType1.length; i++) {
						sellers.add(agentsType1[i].getName());
					}
				} catch (FIPAException e) {
					e.printStackTrace();
				}
			}
		}

		private class QueryBuyerBehaviour extends OneShotBehaviour {
			public QueryBuyerBehaviour(Agent a) 
			{
				super(a);
			}

			@Override
			public void action() {

				// Prepare Query If Message
				ACLMessage msg = new ACLMessage(ACLMessage.QUERY_IF);
				msg.addReceiver(sellerAID);
				msg.setLanguage(codec.getName());
				msg.setOntology(ontology.getName());
				// Prepare the content
				sendlist.clear();

				float typeRand = new Random().nextFloat();
				float memRand = new Random().nextFloat();
				float hdRand = new Random().nextFloat();
				Random generator= new Random();
				int num = generator.nextInt(5);
				
				Type type = new Type();
				Memory mem = new Memory();
				HardDrive hd = new HardDrive();
				MotherBoard mb = new MotherBoard();
				Screen screen = new Screen();
				if (typeRand < 0.5) {
					type.setName("Desktop");
					type.setSerialNumber(123);
					type.setPrice(320);
					mb.setName("desktopMother");
					screen.setName("false");

				} else {
					type.setName("Laptop");
					type.setSerialNumber(345);
					type.setPrice(320);
					mb.setName("laptopMother");
					screen.setName("true");
				}
				if (memRand < 0.5) {
					mem.setName("8GB");
				} else {
					mem.setName("16GB");
				}
				if (hdRand < 0.5) {
					hd.setName("1TB");
				} else {
					hd.setName("2TB");
				}
				pcBuilder.clear();
				pcBuilder.add(type.getName());
				pcBuilder.add(mem.getName());
				pcBuilder.add(hd.getName());
				pcBuilder.add(mb.getName());
				pcBuilder.add(screen.getName());

				Owns owns = new Owns();
				owns.setOwner(sellerAID);
				owns.setItem(type);

				try {
					// Let JADE convert from java objects to string
					getContentManager().fillContent(msg, owns);
					send(msg);			

				} catch (CodecException ce) {
					ce.printStackTrace();
				} catch (OntologyException oe) {
					oe.printStackTrace();
				}
				for(AID seller : sellers)
				{
					msg.addReceiver(seller);
				}
				myAgent.send(msg);


				ACLMessage msg1 = new ACLMessage(ACLMessage.REQUEST);
				msg1.addReceiver(sellerAID); // sellerAID is the AID of the Seller agent
				msg1.setLanguage(codec.getName());
				msg1.setOntology(ontology.getName()); 

				Sell order = new Sell();
				order.setBuyer(myAgent.getAID());
				order.setItem(type);

				Action request = new Action();
				request.setAction(order);
				request.setActor(sellerAID); // the agent that you request to perform the action
				try {
					// Let JADE convert from Java objects to string
					getContentManager().fillContent(msg1, request); //send the wrapper object
					send(msg1);
				}
				catch (CodecException ce) {
					ce.printStackTrace();
				}
				catch (OntologyException oe) {
					oe.printStackTrace();
				} 
			}
		}
	}


public class endDay extends OneShotBehaviour {
	public endDay(Agent a) {
		super(a);
	}

	@Override
	public void action() {
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.addReceiver(DayTicker);
		msg.setContent("done");
		System.out.println("DONE MESSAGE BUYER");
		myAgent.send(msg);
		// Send A Message To Each Seller We Have Finished
		ACLMessage sellerDone = new ACLMessage(ACLMessage.INFORM);
		sellerDone.setContent("done");
		for (AID seller : sellers) {
			sellerDone.addReceiver(seller);
		}
		myAgent.send(sellerDone);
	}
}
}
