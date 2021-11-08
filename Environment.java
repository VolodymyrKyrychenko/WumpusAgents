import aima.core.agent.Agent;
import aima.core.environment.wumpusworld.AgentPosition;
import aima.core.environment.wumpusworld.WumpusAction;
import aima.core.environment.wumpusworld.WumpusCave;
import aima.core.environment.wumpusworld.WumpusPercept;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.HashMap;

public class Environment extends jade.core.Agent {
	private WumpusEnvironment wumpusEnvironment;
	private HashMap<AID, Boolean> registeredAgents = new HashMap<AID, Boolean>();
	private ACLMessage msgRequest;
	private ACLMessage msgCfp;
	
	final int CAVE_X_DIMENSION = 4;
	final int CAVE_Y_DIMENSION = 4;
	
	protected void setup() {
		String configString = "      P W G P           S   P   ";
		WumpusCave cave = new WumpusCave(CAVE_X_DIMENSION, CAVE_Y_DIMENSION, configString);		
		wumpusEnvironment = new WumpusEnvironment(cave);
		
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("environment");
		sd.setName("environment");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		addBehaviour(new RequestHandleServer());
	}
	
	protected void takeDown() {
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		System.out.println("Environment-agent " + getAID().getName() + " terminating.");
	}

	private class RequestHandleServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mtRequest = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			MessageTemplate mtCfp = MessageTemplate.MatchPerformative(ACLMessage.CFP);

			msgRequest = myAgent.receive(mtRequest);
			msgCfp = myAgent.receive(mtCfp);

			if (msgRequest != null) {
				myAgent.addBehaviour(new RequestCurrentEnvironmentStateServer(msgRequest));
			}
			if (msgCfp != null) {
				myAgent.addBehaviour(new ExecuteActionServer(msgCfp));
			}
			if (msgRequest == null && msgCfp == null) {
				block();
			}
		}
	}

	private class RequestCurrentEnvironmentStateServer extends OneShotBehaviour {
		private ACLMessage msg;

		public RequestCurrentEnvironmentStateServer(ACLMessage message)
		{
			super();
			this.msg = message;
		}

		public void action() {
			if (msg != null) {
				AID agentAid = msg.getSender();
				if (!registeredAgents.containsKey(agentAid)) {
					wumpusEnvironment.addAgent(agentAid);
					registeredAgents.put(agentAid, true);
				}

				WumpusPercept percept = wumpusEnvironment.getPerceptSeenBy(agentAid);
				ACLMessage reply = msg.createReply();

				if (percept != null) {
					reply.setPerformative(ACLMessage.INFORM);
					String answer = String.valueOf(percept.isStench()) + " "
							+ String.valueOf(percept.isBreeze()) + " "
							+ String.valueOf(percept.isGlitter()) + " "
							+ String.valueOf(percept.isBump()) + " "
							+ String.valueOf(percept.isScream()) + " "
							+ String.valueOf(wumpusEnvironment.getTime());
					reply.setContent(answer);
				}
				else {
					reply.setPerformative(ACLMessage.FAILURE);
					reply.setContent("no agent was found");
				}
				myAgent.send(reply);
			}
		}
	}
	
	private class ExecuteActionServer extends OneShotBehaviour {
		private ACLMessage msg;

		public ExecuteActionServer(ACLMessage message)
		{
			super();
			this.msg = message;
		}

		public void action() {
			if (msg != null) {
				WumpusAction action = WumpusAction.valueOf(msg.getContent());								
				ACLMessage reply = msg.createReply();
				
				if (action != null) {
					wumpusEnvironment.execute(msg.getSender(), action);

					AgentPosition newPos = wumpusEnvironment.getAgentPosition(msg.getSender());
					System.out.println(newPos);

					reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
					reply.setContent("OK");
				}
				else {
					reply.setPerformative(ACLMessage.FAILURE);
					reply.setContent("invalid action");
				}
				myAgent.send(reply);
				
				if (action == WumpusAction.CLIMB) {
					myAgent.doDelete();
				}
			}
		}
	}
}
