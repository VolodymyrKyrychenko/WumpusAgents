import jade.core.Agent;

import java.util.HashMap;

import aima.core.environment.wumpusworld.*;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class Speleologist extends Agent {
	protected EfficientHybridWumpusAgent wumpusAgent;
	protected AID environmentAid;
	protected AID navigatorAid;
	protected WumpusPercept percept;
	protected WumpusAction offeredAction;
	
	final int CAVE_X_DIMENSION = 4;
	final int CAVE_Y_DIMENSION = 4;
	final AgentPosition START_POS = new AgentPosition(1, 1, AgentPosition.Orientation.FACING_NORTH);
	
	final HashMap<String, String[]> dict = new HashMap<String, String[]>() {{
		put("stench", new String[] {"I feel stench here", "There is a stench", "It is a wumpus stench here"});
		put("breeze", new String[] {"I feel breeze here", "There is a breeze", "It is a cool breeze here"});
		put("glitter", new String[] {"I feel glitter here", "There is a glitter", "It is a gold glitter here"});
		put("bump", new String[] {"I feel bump here", "There is a bump", "It is a wall here"});
		put("scream", new String[] {"I feel scream here", "There is a scream", "It is a wumpus scream here"});
	}};
	
	protected void setup() {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		environmentAid = findAgent("environment");
		navigatorAid = findAgent("navigator");

		wumpusAgent = new EfficientHybridWumpusAgent(CAVE_X_DIMENSION, CAVE_Y_DIMENSION, START_POS);
		addBehaviour(new SpeleologistBehaviour(this));
	}
	
	private AID findAgent(String agentType) {
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType(agentType);
		template.addServices(sd);
		AID agentAid = null;
		try {
			DFAgentDescription[] result = DFService.search(this, template); 
			if (result.length == 0) {
				System.out.println("There are no " + agentType + " found!");
			} else {
				System.out.println("Found the following " + agentType + " agent:");
				agentAid = result[0].getName();
				System.out.println(agentAid.getName());
				return agentAid;
			}
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		return agentAid;
	}
	
	protected void takeDown() {
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		System.out.println("Speleologist-agent " + getAID().getName() + " terminating.");
	}
}
