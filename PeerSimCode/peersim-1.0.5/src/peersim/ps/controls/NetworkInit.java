package peersim.ps.controls;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;

import peersim.core.*;
import peersim.config.Configuration;
import peersim.ps.newscast.Newscast;
import peersim.ps.pubsub.PubSub;
import peersim.ps.tman.TMan;

public class NetworkInit implements Control {

	private static final String PAR_NEWSCAST_PROT="newscast";
	private static final String PAR_TMAN_PROT="tman";
	private static final String PAR_PUBSUB_PROT="pubsub";
	private static final String PAR_IDLENGTH = "idLength";
	private static final String PAR_K = "k";
	
	private final int newscastPID;
	private final int tmanPID;
	private final int pubsubPID;
	private final int idLength;
	private final int k;
	
//------------------------------------------------------------------------	
	public NetworkInit(String prefix) {
		this.newscastPID = Configuration.getPid(prefix + "." + PAR_NEWSCAST_PROT);
		this.tmanPID = Configuration.getPid(prefix + "." + PAR_TMAN_PROT);
		this.pubsubPID = Configuration.getPid(prefix + "." + PAR_PUBSUB_PROT);
		this.idLength = Configuration.getInt(prefix + "." + PAR_IDLENGTH);
		this.k = Configuration.getInt(prefix + "." + PAR_K);
	}
	
//------------------------------------------------------------------------	
	public boolean execute() {
		int count = 0;
		BigInteger id;
		ArrayList<BigInteger> ids = new ArrayList<BigInteger>();

		while (count < Network.size()) {
			id = new BigInteger(this.idLength, CommonState.r);
			if (!ids.contains(id)) {
				ids.add(id);
				count++;
				System.out.println("create id " + count);
			}
		}

		this.newscastInit();
		this.tmanInit(ids);
		this.pubsubInit(ids);
		this.start();
		
		return true;
	}
	
//------------------------------------------------------------------------	
	private void newscastInit() {
		Node node;
		Newscast newscast;
		ArrayList<Node> nodes = new ArrayList<Node>();
		ArrayList<Node> neighbors = new ArrayList<Node>();

		System.out.println("newscast init ...");

		for (int i = 0; i < Network.size(); i++)
			nodes.add(Network.get(i));
		
		for (int i = 0; i < Network.size(); i++) {
			node = Network.get(i);
			newscast = (Newscast)node.getProtocol(this.newscastPID);
			
			if (nodes.size() > this.k) {
				Collections.shuffle(nodes);
				neighbors.addAll(nodes.subList(0, this.k));
			} else
				neighbors = nodes;
			
			neighbors.remove(node);
			
			for (Node n : neighbors)
				newscast.addNeighbor(n);
			
			neighbors.clear();
		}
	}
	
//------------------------------------------------------------------------	
	private void tmanInit(ArrayList<BigInteger> ids) {
		Node node;
		TMan tman;

		System.out.println("tman init ...");

		for(int i = 0; i < Network.size(); i++) {			
			node = Network.get(i);
			tman = (TMan)node.getProtocol(this.tmanPID);
			tman.init(node, ids.get(i), this.idLength);
		}
	}

//------------------------------------------------------------------------	
	private void pubsubInit(ArrayList<BigInteger> ids) {
		Node node;
		PubSub pubsub;

		for(int i = 0; i < Network.size(); i++) {			
			node = Network.get(i);
			pubsub = (PubSub)node.getProtocol(this.pubsubPID);
			pubsub.init(node, ids.get(i));
			System.out.println("pubsub init node " + ids.get(i));
		}
	}

//------------------------------------------------------------------------	
	private void start() {
//		Node publisher = Network.get(CommonState.r.nextInt(Network.size()));
//		PubSub pubsub = (PubSub)publisher.getProtocol(this.pubsubPID);
//		PeerAddress publisherAddress = pubsub.getNodeProfile().getAddress(); 
//		PublishEvent publishEvent = new PublishEvent(publisherAddress.getId(), BigInteger.ZERO);
//		PubSubEvent event = new PubSubEvent(PubSubEventType.PUBLISH, publisherAddress, publishEvent);
//		EDSimulator.add(1000000, event, publisher, this.pubsubPID);
	}

	@Override
	public boolean execute(int exp) {
		// TODO Auto-generated method stub
		return false;
	}
}
