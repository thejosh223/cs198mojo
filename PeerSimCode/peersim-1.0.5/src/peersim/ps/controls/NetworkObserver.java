package peersim.ps.controls;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Vector;

import peersim.config.*;
import peersim.core.*;
import peersim.ps.pubsub.PubSub;
import peersim.ps.tman.TMan;
import peersim.ps.types.NodeInterests;
import peersim.ps.types.NodeProfile;
import peersim.ps.types.PeerAddress;
import peersim.ps.types.Topic;
import peersim.ps.utility.FileIO;

public class NetworkObserver implements Control {
    private static final String PAR_TMAN_PROT = "tman";
    private static final String PAR_PUBSUB_PROT = "pubsub";
    private static final String LogFile = "pubsub.log";
    private static int round;

    private final int tmanPID;
    private final int pubsubPID;

//------------------------------------------------------------------------    
    public NetworkObserver(String name) {
    	round = 0;
    	this.tmanPID = Configuration.getPid(name + "." + PAR_TMAN_PROT);
    	this.pubsubPID = Configuration.getPid(name + "." + PAR_PUBSUB_PROT);
    	FileIO.write("", LogFile);
    }

//------------------------------------------------------------------------    
    public boolean execute() {
    	round++;
    	
    	if (round % 20 == 0) {
        	String str = new String("-------------------------------------------\n");
        	str += "round: " + round + "\n";
        	str += "time: " + peersim.core.CommonState.getTime() + "\n";
        	FileIO.append(str, LogFile);

        	this.tmanObserver();
    		this.pubsubObserver();
    	}
    	
    	return false;
    }

//------------------------------------------------------------------------    
    public boolean tmanObserver() {
    	this.plotTMan();
    	this.verifyRing();
    	
        return false;
    }
    
//------------------------------------------------------------------------    
    public boolean pubsubObserver() {
    	this.verifyStructure();

    	return false;
    }
    
//------------------------------------------------------------------------    
	private void verifyRing() {
		Node node;
		TMan tman;
 		int wrongLinks = 0;
		PeerAddress pred, succ;
		int size = Network.size();
		Node[] sortedNodes = new Node[size];
		TreeMap<PeerAddress, Node> nodesCollection = new TreeMap<PeerAddress, Node>();
	
        for (int i = 0; i < Network.size(); i++) {
        	node = Network.get(i);
        	nodesCollection.put(((TMan)node.getProtocol(this.tmanPID)).getPeerAddress(), node);
        }

        nodesCollection.values().toArray(sortedNodes);
        
        for (int i = 0; i < size; i++) {
        	tman = (TMan)sortedNodes[i].getProtocol(this.tmanPID);
        	
			pred = tman.getPred();
			succ = tman.getSucc();

			if (!((TMan)sortedNodes[(size + i - 1) % size].getProtocol(this.tmanPID)).getPeerAddress().equals(pred))
				wrongLinks++;

			if (!((TMan)sortedNodes[(i + 1) % size].getProtocol(this.tmanPID)).getPeerAddress().equals(succ))
				wrongLinks++;
		}
        
		FileIO.append("Wrong ring links: " + wrongLinks + "\n", LogFile);
	}
	
//------------------------------------------------------------------------    
    private void plotTMan() {
    	TMan tman;
    	BigInteger id;
    	PeerAddress succ;
    	PeerAddress pred;
    	Vector<PeerAddress> cluster; 

        
        String ringStr = "graph g {\n";
        String clusterStr = "digraph g {\n";
        
        for (int i = 0; i < Network.size(); i++) {
        	tman = (TMan)Network.get(i).getProtocol(this.tmanPID);
        	id = tman.getPeerAddress().getId();
        	
        	// draw ring pointers
        	pred = tman.getPred();
        	succ = tman.getSucc();
        	if (pred != null && succ == null)
        		ringStr += id + "-- " + pred.getId() + ";\n";
        	else if (pred == null && succ != null)
        		ringStr += id + " -- " + succ.getId() + ";\n";
        	else if (pred != null && succ != null) {
        		ringStr += id + " -- " + succ.getId() + ";\n";
        		ringStr += id + "-- " + pred.getId() + ";\n";
        	}
        	
        	// draw cluster pointers
        	cluster = tman.getCluster();
        	if (cluster != null) {
        		for (PeerAddress neighbor : cluster)
        			clusterStr += id + "->" + neighbor.getId() + ";\n";
        	}
        }
        
        ringStr += "}";
        clusterStr += "}";
    
        FileIO.write(ringStr, "ring.viz");
        FileIO.write(clusterStr, "cluster.viz");        
    }
    
//-----------------------------------------------------------------------------------------------
 	public void verifyStructure() {
		NodeProfile nodeProfile;
		NodeInterests interestes;
		HashMap<Topic, ArrayList<NodeProfile>> topics = new HashMap<Topic, ArrayList<NodeProfile>>();
		
		for (int i = 0; i < Network.size(); i++) {
        	nodeProfile = ((PubSub)Network.get(i).getProtocol(this.pubsubPID)).getNodeProfile();
        	interestes = nodeProfile.getNodeInterests();
        	
        	for (Topic topic : interestes.getInterests().keySet()) {
        		if (!topics.containsKey(topic))
        			topics.put(topic, new ArrayList<NodeProfile>());
        		
        		topics.get(topic).add(nodeProfile);  // do we need to put it back in the TreeMap again?
        	}
        }
		
		verifySubgraphs(topics);
 	}
 	
//-----------------------------------------------------------------------------------------------
	public void verifySubgraphs(HashMap<Topic, ArrayList<NodeProfile>> topics) {
		String str = "";
		BigInteger topicID;
		ArrayList<NodeProfile> interestedNodes;
		
		for (Topic topic : topics.keySet()) {
			topicID = topic.getTopicId();
			interestedNodes = topics.get(topic);
			
			ArrayList<NodeProfile> pathList = findPathList(interestedNodes.get(0), topicID);

			Vector<NodeProfile> relayPeers = new Vector<NodeProfile>();
			relayPeers.addAll(pathList);
			relayPeers.removeAll(interestedNodes);
			
			Vector<NodeProfile> partitionedPeers = new Vector<NodeProfile>();
			partitionedPeers.addAll(interestedNodes);
			partitionedPeers.removeAll(pathList);			
		
			str += "topic: " + topicID + "\t\trelay nodes: " + relayPeers.size() + "\t\tpath size: " + pathList.size() + "\t\tpartitioned peers: " + partitionedPeers.size() + "\t\tpopulation: " + topics.get(topic).size() + "\n";
			//str += "population: " + topics.get(topic) + "\n\n";
			
//			PubSub pubsub;
//			if (partitionedPeers.size() > 0) {
//				str += "path list: \n";
//				for (NodeProfile profile : pathList) {
//					pubsub = (PubSub)profile.getAddress().getNode().getProtocol(this.pubsubPID);
//					str += profile.getAddress().getId() + "==> friends: " + pubsub.getFriends().getNodes() + ", fans: " + pubsub.getFans().getNodes() + "\n\n";					
//				}
//				
//				str += "-----------------------------\n\n";
//				str += "partitioned peers: \n";
//				for (NodeProfile profile : partitionedPeers) {
//					pubsub = (PubSub)profile.getAddress().getNode().getProtocol(this.pubsubPID);
//					str += profile.getAddress().getId() + "==> friends: " + pubsub.getFriends().getNodes() + ", fans: " + pubsub.getFans().getNodes() + "\n\n";					
//				}
//
//				str += "-----------------------------\n\n";
//				str += "population: " + topics.get(topic) + "\n\n\n\n";
//			}
		}
		
		////////////////////////////////////
//		str += "--------------------------\n";
//		for (Topic topic : topics.keySet())
//			str += "topic: " + topic + ", interesteds: " + topics.get(topic) + "\n";
//		
//		str += "--------------------------\n";
//		PubSub pubsub;
//		PeerAddress address;
//		for (int i = 0; i < Network.size(); i++) {
//			pubsub = (PubSub)Network.get(i).getProtocol(pubsubPID);
//			address = pubsub.getNodeProfile().getAddress();
//			str += "node: " + address.getId() + ", friends: " + pubsub.getFriends().getNodes() + "\n";
//			str += "node: " + address.getId() + ", fans: " + pubsub.getFans() + "\n";
//			for (Topic topic : topics.keySet())
//				str += "node: " + address.getId() + ", topic: " + topic + ", interested neighbors: " + pubsub.findInterestedPeers(address, topic.getTopicId()) + "\n";
//			
//			str += "++++++++\n";			
//		}
		////////////////////////////////////
		
		FileIO.append(str, LogFile);
	}
	
//-----------------------------------------------------------------------------------------------	
	private ArrayList<NodeProfile> findPathList(NodeProfile interestedNode, BigInteger topicID) {
		int i = 0;
		PubSub pubsub;
		NodeProfile node;
		Vector<NodeProfile> nodeInterestedNeighbors;
		ArrayList<NodeProfile> pathList = new ArrayList<NodeProfile>();
		
		pathList.add(interestedNode);
		
		while (i < pathList.size()) {
			node = pathList.get(i);
			pubsub = (PubSub)node.getAddress().getNode().getProtocol(this.pubsubPID);
			nodeInterestedNeighbors = pubsub.findInterestedPeers(pubsub.getNodeProfile().getAddress(), topicID);
			
			for (NodeProfile profile : nodeInterestedNeighbors) {
				if (!pathList.contains(profile))
					pathList.add(profile);
			}
			
			i++;
		}
			
		return pathList;
	}

	@Override
	public boolean execute(int exp) {
		// TODO Auto-generated method stub
		return false;
	} 
}
