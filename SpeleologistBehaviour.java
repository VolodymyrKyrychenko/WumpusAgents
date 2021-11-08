import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aima.core.environment.wumpusworld.WumpusAction;
import aima.core.environment.wumpusworld.WumpusPercept;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class SpeleologistBehaviour extends Behaviour{
	private int step = 0;
	private MessageTemplate mt;
	private Speleologist speleologist;
	
	public SpeleologistBehaviour(Speleologist agent) {
        super(agent);
        speleologist = agent;
	}
	
	@Override
	public void action() {
		switch (step) {
		case 0:
			ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
			request.addReceiver(speleologist.environmentAid); 
            request.setConversationId("environment-current-state");
            request.setReplyWith("environment-request" + System.currentTimeMillis());
            myAgent.send(request);
            mt = MessageTemplate.and(MessageTemplate.MatchConversationId("environment-current-state"),
                    MessageTemplate.MatchInReplyTo(request.getReplyWith()));
            step = 1;
			break;
		case 1:
			ACLMessage envRequestReply = myAgent.receive(mt);
            if (envRequestReply != null) {
            	if (envRequestReply.getPerformative() == ACLMessage.INFORM) {                	
                	// stench, breeze, glitter, bump, scream
                	String[] params = envRequestReply.getContent().split(" ");
                	speleologist.percept = new WumpusPercept();
                	if (Objects.equals(params[0], "true"))
                		speleologist.percept.setStench();
                	if (Objects.equals(params[1], "true"))
                		speleologist.percept.setBreeze();
                	if (Objects.equals(params[2], "true"))
                		speleologist.percept.setGlitter();
                	if (Objects.equals(params[3], "true"))
                		speleologist.percept.setBump();
                	if (Objects.equals(params[4], "true"))
                		speleologist.percept.setScream();
                    step = 2;
					System.out.println("Speleologist get response from environment: " + String.join(" ", params));
                }
            }
            else {
                block();
            }
			break;
		case 2:
			String messageForNavigator = "";
			if (speleologist.percept.isStench())
				messageForNavigator += speleologist.dict.get("stench")[(int) ( Math.random() * 3 )] + ". ";
			if (speleologist.percept.isBreeze())
				messageForNavigator += speleologist.dict.get("breeze")[(int) ( Math.random() * 3 )] + ". ";
			if (speleologist.percept.isGlitter())
				messageForNavigator += speleologist.dict.get("glitter")[(int) ( Math.random() * 3 )] + ". ";
			if (speleologist.percept.isBump())
				messageForNavigator += speleologist.dict.get("bump")[(int) ( Math.random() * 3 )] + ". ";
			if (speleologist.percept.isScream())
				messageForNavigator += speleologist.dict.get("scream")[(int) ( Math.random() * 3 )] + ". ";
			if (messageForNavigator.length() > 1) {
				messageForNavigator = messageForNavigator.substring(0, messageForNavigator.length() - 2);
		    }
			
			ACLMessage navMessage = new ACLMessage(ACLMessage.REQUEST);
			navMessage.addReceiver(speleologist.navigatorAid);
			navMessage.setContent(messageForNavigator);
			navMessage.setConversationId("navigator-decision");
			navMessage.setReplyWith("navigator-request" + System.currentTimeMillis());
            myAgent.send(navMessage);
            mt = MessageTemplate.and(MessageTemplate.MatchConversationId("navigator-decision"),
                    MessageTemplate.MatchInReplyTo(navMessage.getReplyWith()));
            step = 3;
			break;
		case 3:
			ACLMessage navigatorReply = myAgent.receive(mt);
            if (navigatorReply != null) {
                if (navigatorReply.getPerformative() == ACLMessage.INFORM) {
                    Pattern pattern = Pattern.compile("forward|turn(.*)left|turn(.*)right|grab|shoot|climb", Pattern.CASE_INSENSITIVE);
        	        Matcher matcher = pattern.matcher(navigatorReply.getContent());
        	        
        	        while(matcher.find()) {
        	            String found = matcher.group().toLowerCase();
        	            if (found.equals("forward"))
        	            	speleologist.offeredAction = WumpusAction.FORWARD;
        	            else if (found.contains("left"))
        	            	speleologist.offeredAction = WumpusAction.TURN_LEFT;
        	            else if (found.contains("right"))
        	            	speleologist.offeredAction = WumpusAction.TURN_RIGHT;
        	            else if (found.equals("grab"))
        	            	speleologist.offeredAction = WumpusAction.GRAB;
        	            else if (found.equals("shoot"))
        	            	speleologist.offeredAction = WumpusAction.SHOOT;
        	            else if (found.equals("climb"))
        	            	speleologist.offeredAction = WumpusAction.CLIMB;
        	        }
        	        step = 4;
					System.out.println("Speleologist get response from navigator: " + String.valueOf(speleologist.offeredAction));
                }
            }
            else {
                block();
            }
			break;
		case 4:
			ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            cfp.addReceiver(speleologist.environmentAid); 
            cfp.setContent(String.valueOf(speleologist.offeredAction));
            cfp.setConversationId("environment-change-state");
            cfp.setReplyWith("environment-cfp" + System.currentTimeMillis());
            myAgent.send(cfp);
            mt = MessageTemplate.and(MessageTemplate.MatchConversationId("environment-change-state"),
                    MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
            step = 5;
			break;
		case 5:
			ACLMessage envCfpReply = myAgent.receive(mt);
            if (envCfpReply != null) {
                step = 0;
                if (envCfpReply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    if (Objects.equals(envCfpReply.getContent(), "OK")) {
                        if (speleologist.offeredAction == WumpusAction.CLIMB) {
                        	step = 6;
                        }
                    }
                }
            }
            else {
                block();
            }
			break;
		default:
			break;
		}
	}

	@Override
	public boolean done() {
		return (step == 6);
	}
}
