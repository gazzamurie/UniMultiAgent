package coursework;

import java.util.ArrayList;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription; 
import jade.domain.FIPAAgentManagement.ServiceDescription; 
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class DayTicker extends Agent {

	public static final int NUM_DAYS = 90;
	
	@Override
	protected void setup()
	{
		//Add This Agent To The Yellow Pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("ticker-agent");
		sd.setName(getLocalName() + "-ticker-agent");
		dfd.addServices(sd);
		try
		{
			DFService.register(this, dfd);
		}
		catch(FIPAException e)
		{
			e.printStackTrace();
		}
		//Wait For The Agents To Start
		doWait(2);
		addBehaviour(new SynchAgentsBehaviour(this));
	}

	//DeRegister From The Yellow Pages
	@Override
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
	public class SynchAgentsBehaviour extends Behaviour
	{
		private int step = 0;
		private int day = 0;
		private ArrayList<AID> simulationAgents = new ArrayList<>();
		private int numFinReceived = 0;
		
		public SynchAgentsBehaviour(Agent a)
		{
			super(a);
		}
		@Override
		public void action()
		{
			switch(step)
			{
			case 0:
				//Find all agents using directory service 
				DFAgentDescription template1 = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("customer");
				template1.addServices(sd);
				DFAgentDescription template2 = new DFAgentDescription();
				ServiceDescription sd2 = new ServiceDescription();
				sd2.setType("manufacturer");
				template2.addServices(sd2);
				try
				{
					DFAgentDescription[] agentsType1 = DFService.search(myAgent, template1);
					for(int i=0; i<agentsType1.length; i++)
					{
						simulationAgents.add(agentsType1[i].getName());
					}
					DFAgentDescription[] agentsType2 = DFService.search(myAgent, template2);
					for(int i=0; i<agentsType2.length; i++)
					{
						simulationAgents.add(agentsType2[i].getName());
					}
				}
				catch(FIPAException e)
				{
					e.printStackTrace();
				}
				//Send New Day Message To Each Agent
				ACLMessage tick = new ACLMessage(ACLMessage.INFORM);
				tick.setContent("new day");
				for(AID id : simulationAgents)
				{
					tick.addReceiver(id);
				}
				myAgent.send(tick);
				step++;
				System.out.println("DAY: " + day);
				day++;
				
				break;
			case 1:
				//Wait To Receive A "done" Message From All Agents
				MessageTemplate mt = MessageTemplate.MatchContent("done");
				ACLMessage msg = myAgent.receive(mt);
				if(msg != null)
				{
					numFinReceived++;
					if(numFinReceived >= simulationAgents.size())
					{
						step++;
					}
				}
				else
				{
					block();
				}
			}
		}
		@Override
		public boolean done()
		{
			return step == 2;
		}
		//Reset Once Day Has Ended
		@Override
		public void reset()
		{
			super.reset();
			step = 0;
			simulationAgents.clear();
			numFinReceived = 0;
		}
		@Override
		public int onEnd()
		{
			
			if(day == NUM_DAYS)
			{
				
				//Send Termination Message To Each Agent
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				msg.setContent("terminate");
				System.out.println("TERMINATE");
				for(AID agent : simulationAgents)
				{
					msg.addReceiver(agent);
				}
				myAgent.send(msg);
				myAgent.doDelete();
			}
			else
			{
				reset();
				myAgent.addBehaviour(this);
			}
			return 0;
		}
	}
}
