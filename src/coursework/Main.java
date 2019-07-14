package coursework;

import jade.core.*;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

public class Main {

	public static void main(String[] args)
	{
		Profile myProfile = new ProfileImpl();
		Runtime myRunTime = Runtime.instance();
		try
		{
			ContainerController myContainer = myRunTime.createMainContainer(myProfile);
			AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
			rma.start();
			
			AgentController sellerAgent = myContainer.createNewAgent("seller", OnSellerAgent.class.getCanonicalName(), null);
			sellerAgent.start();
			
			AgentController buyerAgent = myContainer.createNewAgent("buyer", CautiousBuyerAgent.class.getCanonicalName(), null);
			buyerAgent.start();
			
			AgentController dayTicker = myContainer.createNewAgent("ticker", DayTicker.class.getCanonicalName(), null);
			dayTicker.start();
			
//			AgentController RecklessbuyerAgent = myContainer.createNewAgent("Reckbuyer", RecklessBuyerAgent.class.getCanonicalName(), null);
//			RecklessbuyerAgent.start();
		}
		catch(Exception e)
		{
			System.out.println("Exception Stating Agent: " + e.toString());
		}
	}
}
