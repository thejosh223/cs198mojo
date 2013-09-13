package peersim.simildis;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import java.util.Queue;
import java.util.Set;

import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.graph.NeighbourListGraph;
import peersim.simildis.NodeLabels;
import peersim.simildis.utils.SimpleEdge;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SimUpdatControl implements Control{

    private static final String PAR_PROT = "protocol";
    private static final String PAR_THETTA = "thetta";
    private static final String PAR_STRATEGY = "strategy";
    private static final String PAR_DUMMY_SHARE = "dummy_share";
    private static final String PAR_EXPS = "experiments";
    private static final String PAR_RUNTIME_FILE ="runtime_file";
    
    
    
    private final String name;

    private final int pid;
    private final double thetta;
    private final String strategy;
    private final double dummy_share;
    private final String runtimeFileName ;
    
    FileOutputStream confFileHandler;// = new FileOutputStream(confFile);
	PrintStream outFile; // = new PrintStream(confFileHandler);
    
    public SimUpdatControl(String name) {
        this.name = name;
        pid = Configuration.getPid(name + "." + PAR_PROT);
        thetta = Configuration.getDouble(name + "." +PAR_THETTA);
        strategy = Configuration.getString(name + "." +PAR_STRATEGY);
        dummy_share = Configuration.getDouble(name + "." +PAR_DUMMY_SHARE);
        runtimeFileName = Configuration.getString(name+"."+PAR_RUNTIME_FILE);
        
        try {
        	outFile = new PrintStream(new FileOutputStream(runtimeFileName, true));
        	outFile.println("#experiment|cycle|runtime");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
    }
	
	@Override
	public boolean execute(int exp) {
		// TODO Auto-generated method stub
		
		//log("--> Running update similarity ");
		
		 /*ExecutorService execSvc = Executors.newFixedThreadPool( 2 );
		 
		 for (int i = 1; i < Network.size(); i++) {
			 execSvc.execute(new ThreadUpdateSimGraph(i, (float)thetta, (float)dummy_share, pid, strategy));
			}
		 execSvc.shutdown();*/
		 
		long time = peersim.core.CommonState.getTime();

		//log("Cycle: "+ time+" ---> Similarity graph update is done." );
		
		//for (int i = 1; i < Network.size(); i++) {
			double runtime = updateSimilarityGraph(1, thetta);
			//if (i==1)
			//	outFile.println((exp+1)+"|"+(time+1)+"|"+runtime);
		//}
		
		
		return false;
	}
	
////////////////////////////////////////////////////////////////
	public void traverseAndUpdate_3_modified(int startPoint, SimilarityDAG sDAG, NeighbourListGraph sg, SimilarityList sl, float sameLevelChangeThreshold){
		/**
		 * This method only updates the similarity graph and similarity values. It assumes that the graph structure already is 
		 * in order, and the similarity of the startPoint has already been calculated and is fixed. 
		 */
		
		Queue<Integer> q=new LinkedList<Integer>();
		q.add(startPoint);
		
		ArrayList<Integer> childrenList = new ArrayList<Integer>();
		while(!q.isEmpty()){ 
			int i=q.remove();
			childrenList.clear();
				//Queue<Integer> remQueue = new LinkedList<Integer>();
				//remQueue.add(i);
				//HashMap<Integer, Float> traversed = new HashMap<Integer, Float>();
				//Set<SimpleEdge> edgesIntheSameLevel = new HashSet<SimpleEdge>();
				//Set<Integer> foundChildren = new HashSet<Integer>();
				
				Collection<Integer> children = sDAG.DAG.getNeighbours(i);
				Set<Integer> dummyChildren = new HashSet<Integer>();
				for (Integer child : children) {
					if(child < 0){
						dummyChildren.add(child);
						//int parent2 = sDAG.getTheOtherParent(child, i);
						//edgesIntheSameLevel.add(new SimpleEdge(i,parent2, child));
						
					}
				}
				
/*				while (!remQueue.isEmpty()){  // traverse and find the edges in the same level that via the dummy nodes are reachable via i
					int parent1 = remQueue.remove();
					traversed.put(parent1, (float)0);
					
					Collection<Integer> children = sDAG.DAG.getNeighbours(parent1);
					for (Integer child : children) {
						if(child < 0 && !foundChildren.contains(child)){
							int parent2 = sDAG.getTheOtherParent(child, parent1);
							edgesIntheSameLevel.add(new SimpleEdge(parent1,parent2, child));
							
							traversed.put(parent1, traversed.get(parent1)+sg.getBiDiEdgeWeights(parent1, parent2)/2);
							//if (!traversed.keySet().contains(parent2)){
							//	remQueue.add(parent2);
							//}
							foundChildren.add(child);
						}
						else{
							
							traversed.put(parent1, traversed.get(parent1)+sg.getBiDiEdgeWeights(parent1, child));
						}
					}

				}*/
				
				//Set<Integer> balancedNodes =  balanceSimilaritySameLevel(edgesIntheSameLevel, sDAG,sg,traversed, sDAG.labels.get(i).level, sameLevelChangeThreshold,  sl);
				Set<Integer> balancedNodes;
				if (sDAG.labels.get(i).level<=2 && false){
					balancedNodes =  balanceSimilaritySameLevel(dummyChildren, sDAG,sg, sDAG.labels.get(i).level, sameLevelChangeThreshold,  sl);
				}
				else{
					balancedNodes = new HashSet<Integer>();
				}
				
				
				//Set<Integer> balancedNodes =  balanceSimilaritySameLevel(dummyChildren, sDAG,sg, sDAG.labels.get(i).level, sameLevelChangeThreshold,  sl);
				
				//Set<Integer> balancedNodes = new HashSet<Integer>();
				balancedNodes.add(i);
				HashSet<Integer> allChildren = new HashSet<Integer>();
				for (Integer m : balancedNodes) {
					float sumWeights = 0;
					childrenList.clear();
					for (Integer child : sDAG.DAG.getNeighbours(m)) {
						if ( child > 0){
							//sumWeights += sg.getBiDiEdgeWeights(m, child);
							childrenList.add(child);
							allChildren.add(child);
							if (!q.contains(child))
					    		q.add(child);
						}
						sumWeights += sg.getBiDiEdgeWeights(m, child);
						

					}
					
					if (childrenList.size()>0){
						float currentHeadSimilarity = sDAG.labels.get(m).similarity;
						byte currentLevel = sDAG.labels.get(m).level;
						
						for (int k = 0; k < childrenList.size(); k++) {
							
							int j = childrenList.get(k);
							float currentWight = sg.getBiDiEdgeWeights(m, j);
							
							float newDAGWeight;
							if (sumWeights==0)
								newDAGWeight = 0; 
							else 
								newDAGWeight = currentHeadSimilarity * currentWight /sumWeights;
							
							//float currentDAGWeight = sDAG.DAG.getEdgeWeight(m, j);
							sDAG.DAG.updateWeight(m, j, newDAGWeight);
							
							/*float currentSimilarity = sDAG.labels.get(j).similarity;
							
							float delta = newDAGWeight - currentDAGWeight;
							float f = (float) (currentSimilarity + Math.pow(sDAG.thetta, (double)(currentLevel+1)) * delta);
							sDAG.labels.get(j).similarity = f; 
							sDAG.receivedSimilarity.put(j, f);
							sl.updateSimilarity(j, f);
							
							sDAG.labels.get(j).level = (byte)(currentLevel +1);*/
							
						}
						
						
					}
					
				}  
				for (Integer child :allChildren){
					
					updateSimilarity(child,sDAG, sg, sl);
				}
				
			///}
			
		}
	}
	
	
	
	
	////////////////////////
	public void  updateSimilarity(int node, SimilarityDAG sDAG, NeighbourListGraph sg, SimilarityList sl){
		float f = sDAG.DAG.totalInCapacity(node);
		byte level = sDAG.labels.get(node).level;
		sDAG.labels.get(node).similarity = (float)(Math.pow(this.thetta,level ) * f);
		sDAG.receivedSimilarity.put(node, sDAG.labels.get(node).similarity);
		sl.updateSimilarity(node, sDAG.labels.get(node).similarity);
		
	}
	
///////////////////////////////////////////////////////////////////////////////////
	public Set<Integer> balanceSimilaritySameLevel(Set<Integer> dummyChildren, SimilarityDAG sDAG, NeighbourListGraph sGraph, int myLevel,float sameLevelChangeThreshold,SimilarityList sl){
		
		Set<Integer> balancedNodes = new HashSet<Integer>();
		Set<Integer> tempSet = new HashSet<Integer>();
		HashMap<Integer, List<Float>> similarities = new HashMap<Integer, List<Float>>();

		for(Integer c: dummyChildren){
//			int node1, node2;
			
			Collection<Integer> parents = sDAG.DAG.getReverseNeighbours(c);
			Iterator iterator = parents.iterator();
			
			//for (Iterator iterator = parents.iterator(); iterator.hasNext();) {
			int node1 = (Integer) iterator.next();
			//iterator.next();
			int node2 = (Integer) iterator.next();
			//	break;
			//}
			float commonEdgeWeight = sGraph.getBiDiEdgeWeights(node1, node2);		
			
			////////////////////////
			float capToChildren1 =0;
			for (Integer child: sDAG.DAG.getNeighbours(node1)) {
				if (child >0){
					commonEdgeWeight += sGraph.getBiDiEdgeWeights(node1, child);
				
				}
				
			}
			
			float capToChildren2 =0;
			for (Integer child: sDAG.DAG.getNeighbours(node2)) {
				if (child >0){
					commonEdgeWeight += sGraph.getBiDiEdgeWeights(node2, child);
				
				}
				
			}
			//float capToChildren1 = capToChildren.get(node1);
			tempSet.clear();
			Collection<Integer> parents1 = sDAG.DAG.getReverseNeighbours(node1);
			float flowFromParents1 = 0;

			for (Integer parent : parents1) {
				flowFromParents1 += sGraph.getBiDiEdgeWeights(parent, node1);
			}
			
			//float capToChildren2 = capToChildren.get(node2);
			Collection<Integer> parents2 = sDAG.DAG.getReverseNeighbours(node2);
			float flowFromParents2 = 0;
			
			for (Integer parent : parents2) {
				flowFromParents2 += sGraph.getBiDiEdgeWeights(parent, node2);
			}
			
			
			float share1 = 1- (flowFromParents1/(flowFromParents1+flowFromParents2) );
			float share2 = 1- (flowFromParents2/(flowFromParents1+flowFromParents2) );
			
			//share1= (float) 0.5;
			//share2= (float) 0.5;
			
			float w = commonEdgeWeight/2;
			/*float receivedSimFromHigherLevel_1 = 0;
			float receivedSimFromHigherLevel_2 = 0;
			
			
			if (node1 < node2)
				{
				receivedSimFromHigherLevel_1 = sDAG.labels.get(node1).similarity - sDAG.labels.get(commonChild).similarity;
				receivedSimFromHigherLevel_2 = sDAG.labels.get(node2).similarity + sDAG.labels.get(commonChild).similarity;
				
				}
			else{
				receivedSimFromHigherLevel_1 = sDAG.labels.get(node1).similarity + sDAG.labels.get(commonChild).similarity;
				receivedSimFromHigherLevel_2 = sDAG.labels.get(node2).similarity - sDAG.labels.get(commonChild).similarity;
				
			}*/

			float shareNode1 = (float) Math.pow(this.thetta, myLevel+1) * share1 * sDAG.receivedSimilarity.get(node1) * (w/(capToChildren1));
			float shareNode2 = (float) Math.pow(this.thetta, myLevel+1) * share2 * sDAG.receivedSimilarity.get(node2) * (w/(capToChildren2));
			
			float delta1 = (- shareNode1 + shareNode2)/2;
			float delta2 = - delta1;
			
			if (!similarities.containsKey(node1))
				similarities.put(node1, new ArrayList<Float>());
			
			if (!similarities.containsKey(node2))
				similarities.put(node2, new ArrayList<Float>());
			
			
			similarities.get(node1).add(delta1);
			similarities.get(node2).add(delta2);
			
			/*if (node1 < node2)
				sDAG.labels.put(commonChild, new NodeLabels(delta1, (byte)(myLevel+1))); // delta1 is added to the smaller node
			else
				sDAG.labels.put(commonChild, new NodeLabels(-delta1, (byte)(myLevel+1)));*/
			
		}
		
		for(Integer node: similarities.keySet()){
			float sum=0;
			float currentSim = sDAG.receivedSimilarity.get(node); //sDAG.labels.get(node).similarity;
			for(float delta: similarities.get(node)){
				sum+= delta;
				currentSim += delta;
			}
			
			//sDAG.labels.remove(node);
			//if (Math.abs(sDAG.labels.get(node).similarity - currentSim) > sameLevelChangeThreshold){
			if (Math.abs(sDAG.labels.get(node).similarity - currentSim) > 0.2+sameLevelChangeThreshold){
				sDAG.labels.remove(node);
				sDAG.labels.put(node, new NodeLabels(currentSim, (byte)(myLevel)));
				//sl.updateSimilarity(node, currentSim);
				balancedNodes.add(node);
			}
			
			
		}
		
		return balancedNodes;
		
	}
	
////////////////////////////////////////////////////////////////
	public void traverseAndUpdate_1_modified(int startPoint, SimilarityDAG sDAG, NeighbourListGraph sg, SimilarityList sl, float sameLevelChangeThreshold){
		/**
		 * This method only updates the similarity graph and similarity values. It assumes that the graph structure already is 
		 * in order, and the similarity of the startPoint already is calculated and fixed. 
		 */
		
		Queue<Integer> q=new LinkedList<Integer>();
		q.add(startPoint);
		
		//Set<Integer> balancedSameLevelNodes = new HashSet<Integer>();
		ArrayList<Integer> tempList = new ArrayList<Integer>();
		while(!q.isEmpty()){ 
			int i=q.remove();
			//tempList.clear();
			//Queue<Integer> remQueue = new LinkedList<Integer>();
			//remQueue.add(i);
			//HashMap<Integer, Float> traversed = new HashMap<Integer, Float>();
			//Set<SimpleEdge> edgesIntheSameLevel = new HashSet<SimpleEdge>();
			//Set<Integer> foundChildren = new HashSet<Integer>();
			
			Collection<Integer> children = sDAG.DAG.getNeighbours(i);
			Set<Integer> dummyChildren = new HashSet<Integer>();
			for (Integer child : children) {
				if(child < 0){
					dummyChildren.add(child);
					//int parent2 = sDAG.getTheOtherParent(child, i);
					//edgesIntheSameLevel.add(new SimpleEdge(i,parent2, child));
					
				}
			}
			
			
			
			
/*			while (!remQueue.isEmpty()){  // traverse and find the edges in the same level that via the dummy nodes are reachable via i
				int parent1 = remQueue.remove();
				traversed.put(parent1, (float)0);
				Collection<Integer> children = sDAG.DAG.getNeighbours(parent1);
				for (Integer child : children) {
					if(child < 0 && !foundChildren.contains(child)){
						int parent2 = sDAG.getTheOtherParent(child, parent1);
						edgesIntheSameLevel.add(new SimpleEdge(parent1,parent2, child));
						traversed.put(parent1, traversed.get(parent1)+sg.getBiDiEdgeWeights(parent1, parent2)/2);
						if (!traversed.keySet().contains(parent2)){
							remQueue.add(parent2);
						}
						foundChildren.add(child);
							
					}
					else{
							traversed.put(parent1, traversed.get(parent1)+sg.getBiDiEdgeWeights(parent1, child));
						}
					}

				}*/
			Set<Integer> balancedNodes;
				if (sDAG.labels.get(i).level<=2 && false){
					balancedNodes =  balanceSimilaritySameLevel(dummyChildren, sDAG,sg, sDAG.labels.get(i).level, sameLevelChangeThreshold,  sl);
				}
				else{
					balancedNodes = new HashSet<Integer>();
				}
				
				
				//Set<Integer> balancedNodes = new HashSet<Integer>();
				balancedNodes.add(i);
				for (Integer m : balancedNodes) {
					float sumWeights = 0;
					tempList.clear();
					for (Integer child : sDAG.DAG.getNeighbours(m)) {
						if ( child > 0){
							sumWeights += sg.getBiDiEdgeWeights(m, child);
							tempList.add(child);
							if (!q.contains(child))
					    		q.add(child);
						}
						
						//tempList.add(child);
						//if (!q.contains(child))
				    	//	q.add(child);
					}
					
					if (tempList.size()>0){
						float currentHeadSimilarity = sDAG.labels.get(m).similarity;
						byte currentLevel = sDAG.labels.get(m).level;
						
						for (int k = 0; k < tempList.size(); k++) {
							
							int j = tempList.get(k);
							float currentWight = 0;
							currentWight = sg.getBiDiEdgeWeights(m, j);
							
							float newDAGWeight;
							if (sumWeights==0)
								newDAGWeight = 0; 
							else 
								newDAGWeight = currentHeadSimilarity * currentWight /sumWeights;
							
							float currentDAGWeight = sDAG.DAG.getEdgeWeight(m, j);
							sDAG.DAG.updateWeight(m, j, newDAGWeight);
							
							//float currentSimilarity = sDAG.labels.get(j).similarity;
							float currentReceivedSimilarity=0;
							if (sDAG.receivedSimilarity.containsKey(j))
								currentReceivedSimilarity = sDAG.receivedSimilarity.get(j);
							
							float delta = newDAGWeight - currentDAGWeight;
							float f = (float) (currentReceivedSimilarity + Math.pow(sDAG.thetta, (double)(currentLevel+1)) * delta);
							sDAG.labels.get(j).similarity = f; 
							sDAG.receivedSimilarity.put(j, f);
							//sl.updateSimilarity(j, f);
							
							sDAG.labels.get(j).level = (byte)(currentLevel +1);
							
						}
					}
					
				}  
				
			///}
			
		}
	}
	
////////////////////////////////////////////////////////////////
	public void traverseAndUpdate_2(int A, int B,  SimilarityDAG sDAG, NeighbourListGraph sg, SimilarityList sl, float thresholdValued){
		/**
		 * A in the main CC but B is not. 
		 */
		float removedEdgeWeight = (float)0;
		if (sg.hasEdge(A, B)){
			removedEdgeWeight = sg.getEdgeWeight(A, B);
			sg.clearEdge(A, B);
			joinSubgraph(A,B,sDAG,sg);
			
			sg.setEdge(A, B, removedEdgeWeight);
			//traverseAndUpdate_1(A, sDAG, sg, sl);
			traverseAndUpdate_1_modified(A, sDAG, sg, sl,thresholdValued);
			
		}
		else if (sg.hasEdge(B, A)){
			removedEdgeWeight = sg.getEdgeWeight(B, A);
			sg.clearEdge(B, A);
			joinSubgraph(A,B,sDAG,sg);
			
			sg.setEdge(B, A, removedEdgeWeight);
			//traverseAndUpdate_1(A, sDAG, sg, sl);
			traverseAndUpdate_1_modified(A, sDAG, sg, sl,thresholdValued);
		}
	}
	
////////////////////////////////////////////////////////////////
	public void joinSubgraph(int headNode, int tailNode,  SimilarityDAG sDAG, NeighbourListGraph sg){
		/**
		 * This method process a new subgraph and joins it to the similarity graph. The assigned similarity values for the newly joined nodes are 0.
		 */
		HashMap<Integer, Set<Integer>> newNodesSet = sg.bfsTraverseUndirected(tailNode);
		
		int numberOfLevels = newNodesSet.keySet().size();
		
		byte startingLevel = (byte)(sDAG.labels.get(headNode).level +1);
		sDAG.labels.put(tailNode, new NodeLabels((float)0.0,(byte)startingLevel));
		
		for (int i = 0; i < numberOfLevels; i++) {
			Integer[] headNodesArray = new Integer[newNodesSet.get(i).size()];
			headNodesArray = newNodesSet.get(i).toArray(headNodesArray);

			for (int j = 0; j < headNodesArray.length; j++) {
				int head = headNodesArray[j];
				if (!sDAG.labels.keySet().contains(head)){
					sDAG.labels.put(head, new NodeLabels((float)0,(byte)(startingLevel+i)));
				}
				if (i < numberOfLevels -1 ){
						for (Iterator<Integer> itJ = newNodesSet.get(i+1).iterator(); itJ.hasNext();) {
							int tail = itJ.next();
							if (sg.hasUnEdge(head, tail)) {
								sDAG.DAG.setEdge(head, tail, (float)0 );
								
								if (!sDAG.labels.keySet().contains(tail)){
									sDAG.labels.put(tail, new NodeLabels((float)0,(byte)(startingLevel+i+1)));
								}
								
							}
						}
				}
				for (int k = j+1; k < headNodesArray.length; k++){
					int tail = headNodesArray[k];
					if (head!=tail){
						if (sg.hasUnEdge(head, tail)) { //edge at the same level
							sDAG.DAG.setEdge(head, sDAG.dummyNodeIndex, (float)0 );
							sDAG.DAG.setEdge(tail, sDAG.dummyNodeIndex, (float)0 );
							sDAG.labels.put(sDAG.dummyNodeIndex, new NodeLabels((float)0,(byte)(startingLevel+i+1)));
							sDAG.dummyNodeIndex -=1;
							
						}
					}
				}
			}
		}
		
		sDAG.DAG.setEdge(headNode, tailNode, (float)0);
		if (!sDAG.labels.keySet().contains(tailNode))
			sDAG.labels.put(tailNode, new NodeLabels((float)0,(byte)(startingLevel)));
		
	}
	
	
////////////////////////////////////////////////////////////////	
	public double  updateSimilarityGraph(int nodeIndex, double thetta){
		// updates the similarity list according to the new records. 
		
		SimilDis p = (SimilDis) Network.get(nodeIndex).getProtocol(pid);
		
		NeighbourListGraph gDAG = p.similarityGraph.DAG;
		NeighbourListGraph sg = p.subjectiveGraph;
		
		if (p.newlyReceivedInfo.size()==0){
			if (p.similarityGraph.root == -1){ //This is the first time that the similarity graph is touched.  
				p.similarityGraph.root = nodeIndex;
				p.similarityGraph.DAG.addNode(nodeIndex);
				p.similarityGraph.labels.put(nodeIndex, new NodeLabels((float)1.0, (byte)0) );
				p.similarityGraph.thetta = (float)thetta;
				p.similarityGraph.dummyNodeIndex = -1;
				p.subjectiveGraph.addNode(nodeIndex);
			}
			return 0;
		}
		
		if (p.similarityGraph.root == -1){ //This is the first time that the similarity graph is touched.  
			p.similarityGraph.root = nodeIndex;
			p.similarityGraph.DAG.addNode(nodeIndex);
			p.similarityGraph.labels.put(nodeIndex, new NodeLabels((float)1.0, (byte)0) );
			p.similarityGraph.thetta = (float)thetta;
			p.similarityGraph.dummyNodeIndex = -1;
			
			
		}
		
		Iterator<InfoEdge> it = p.newlyReceivedInfo.iterator(); // the list of new records read from the file or received from other peers.
		
		double updateStartTime = System.currentTimeMillis();
		
		while (it.hasNext())
		{
			InfoEdge e = it.next();
			//log(e.toString());
			int A = e.peer_id_from;
			int B = e.peer_id_to;
			float w = e.transfer;
			
			
			
			if (sg.getEdgeWeight(A, B) >= w || A==0 || B==0) continue;
			
			sg.setEdge(A, B,w);  // add the record to the subjective graph.
			
			p.buffer.addNewRecord(A, B, (byte)0);
			
			if (gDAG.hasNode(A)) {
				NodeLabels labelA = p.similarityGraph.labels.get(A);
				NodeLabels labelB; //= p.similarityGraph.labels.get(B);
				if (gDAG.hasNode(B)) { 	// both at DAG
					 labelB = p.similarityGraph.labels.get(B);
					 if (gDAG.hasUnEdge(A, B)){ // There is an edge between A and B.
						 
						 if (labelA.level < labelB.level) {  // the edge is A-->B
							 //traverseAndUpdate_1(A, p.similarityGraph, sg , p.similarityList);
							 traverseAndUpdate_1_modified(A, p.similarityGraph, sg , p.similarityList, 0);
						 }
						 
						 else if (labelB.level < labelA.level) { // the edge is B-->A 
							 //traverseAndUpdate_1(B, p.similarityGraph, sg, p.similarityList);
							 traverseAndUpdate_1_modified(B, p.similarityGraph, sg, p.similarityList,0);
						 }	 
						 else {  // A and B are at the same level 
							 //traverseAndUpdate_1(A, p.similarityGraph, sg, p.similarityList);
							 //traverseAndUpdate_1(B, p.similarityGraph, sg, p.similarityList);
							 traverseAndUpdate_1_modified(A, p.similarityGraph, sg, p.similarityList,0);
						 }
					 }
					 else{  // The edge between A and B is new. 
						 if (labelA.level < labelB.level) {
							 if (labelA.level+1 == labelB.level){ // the level does not change. 
								 p.similarityGraph.DAG.setEdge(A, B,(float)0);
								 //traverseAndUpdate_1(A, p.similarityGraph, sg, p.similarityList);
								 traverseAndUpdate_1_modified(A, p.similarityGraph, sg, p.similarityList,0);
							 }
							 else  
							 {	 
								 Queue<Integer> unprocessedNodes = new LinkedList<Integer>();
								 unprocessedNodes.add(B);
								 HashMap<Integer, Byte> oldLeves = new HashMap<Integer, Byte>();
								 
								 oldLeves.put(B, labelB.level);
								 p.similarityGraph.labels.get(B).level= (byte) (labelA.level+1);
								 rewireDAGBetter( oldLeves, p.similarityGraph, sg, unprocessedNodes);
								 //gDAG.visualize("src/peersim/simildis/result/SG_debug_1.dot");
								 p.similarityGraph.DAG.setEdge(A, B,(float)0);
								 //traverseAndUpdate_3(A, p.similarityGraph, p.subjectiveGraph, p.similarityList);
								 traverseAndUpdate_3_modified(A, p.similarityGraph, p.subjectiveGraph, p.similarityList,0);
						 
							 }
						 }
						 else if(labelA.level > labelB.level) {
							 if (labelB.level+1 == labelA.level){ // the level does not change as well. 
								 p.similarityGraph.DAG.setEdge(B, A,(float)0);
								 //traverseAndUpdate_1(B, p.similarityGraph, sg, p.similarityList);
								 traverseAndUpdate_1_modified(B, p.similarityGraph, sg, p.similarityList,0);
							 }
							 else{
								 Queue<Integer> unprocessedNodes = new LinkedList<Integer>();
								 unprocessedNodes.add(A);
								 HashMap<Integer, Byte> oldLeves = new HashMap<Integer, Byte>();
								 
								 oldLeves.put(A, labelA.level);
								 p.similarityGraph.labels.get(A).level= (byte) (labelB.level+1);
								 //if (A==626 && B==7)
								//	 p.similarityGraph.DAG.visualize("src/peersim/simildis/resultNoDummy/DAGSim_dynamic_626_7_before_rewire.dot");
								 rewireDAGBetter( oldLeves, p.similarityGraph, sg, unprocessedNodes);
								 //if (A==626 && B==7){
								//	 log("After rewire:"+p.similarityGraph.hasCycle()+"");
								//	 p.similarityGraph.DAG.visualize("src/peersim/simildis/resultNoDummy/DAGSim_dynamic_afterupdate.dot");
									 
								 //}
								 
								 p.similarityGraph.DAG.setEdge(B, A,(float)0);
								 ///log("After adding the edge :"+p.similarityGraph.hasCycle()+"");
								// p.similarityGraph.DAG.visualize("src/peersim/simildis/resultNoDummy/DAGSim_dynamic_afteaddedge.dot");
								//traverseAndUpdate_3(B, p.similarityGraph, p.subjectiveGraph, p.similarityList);
								traverseAndUpdate_3_modified(B, p.similarityGraph, p.subjectiveGraph, p.similarityList,0);
								//log("After modifiy weights:"+p.similarityGraph.hasCycle()+"");
							}
							 
						 }
						 
						 else { // A and B are at the same level. 
							 if (this.strategy.equals("dummy")){ // the strategy is to create a dummy node.
								 
								 Collection<Integer> n1 = p.similarityGraph.DAG.getNeighbours(A);
								 Collection<Integer> n2 = p.similarityGraph.DAG.getNeighbours(B);
								 if (!haveCommonDummy(n1,n2)){
								 		 int dumNode = p.similarityGraph.dummyNodeIndex;
										 p.similarityGraph.dummyNodeIndex -=1;
										 p.similarityGraph.DAG.setEdge(A, dumNode, (float)0);
										 p.similarityGraph.DAG.setEdge(B, dumNode, (float)0);
										 p.similarityGraph.labels.put(dumNode, new NodeLabels((float)0, (byte)0));
								 	}
								 //traverseAndUpdate_1(A, p.similarityGraph, sg, p.similarityList);
								 //traverseAndUpdate_1(B, p.similarityGraph, sg, p.similarityList);
								 traverseAndUpdate_1_modified(A, p.similarityGraph, sg, p.similarityList,0);
							 }
						 } 
					 }
					
				}
				else
				{
					traverseAndUpdate_2(A, B, p.similarityGraph, sg, p.similarityList,0);	// A at DAG B not at DAG.
				}
				
			}
			else {
				if (gDAG.hasNode(B)) {
					// A not in DAG , B in DAG
					traverseAndUpdate_2(B, A, p.similarityGraph, sg, p.similarityList,0);
				}
				else
				{
					// None at DAG 
					continue;
					//System.err.println("edge "+A+" ---> "+B+" is added to SG.");
				}
			}
			
			int cc = p.similarityGraph.hasCycle();
			if (cc!=-1){
				log("cc="+cc);
				log("edge:"+e.toString()  );
				System.exit(1);
				
			}
		}
		
		double updateEndTime = System.currentTimeMillis();
		//System.out.println("runtime:"+ (updateEndTime - updateStartTime));
		

		
		p.newlyReceivedInfo.clear();
		return (updateEndTime - updateStartTime);
	}

///////////////////////////////////////////////////////////
		
///////////////////////////////////////////////////////////
public void rewireDAGBetter(HashMap<Integer, Byte> oldLevelSet, SimilarityDAG sDAG, NeighbourListGraph sg, Queue<Integer> unTraversed){
	//sDAG.DAG.visualize("src/peersim/simildis/result/ProblemSerious_b2.dot");
	//log("Before Enter -------->>>>>> "+unTraversed.toString());
	
	HashSet<Integer> processed = new HashSet<Integer>();
	
	while (!unTraversed.isEmpty()){
		// TODO: the following 2 lines could be removed.
	//if (unTraversed.isEmpty())
	//	return;
	
	int m = unTraversed.remove(); // m with new level 
	byte startPointOldLevel;
	byte startPointNewLevel;
	byte levelDif;
	//skips all dummy nodes.
	while (m < 0){
		if (unTraversed.isEmpty())
			return;  
		m = unTraversed.remove();
		 
	}
	
	if (processed.contains(m)) continue;
	//sDAG.DAG.visualize("src/peersim/simildis/result/SG_debug_1.dot");
	//log(sDAG.labels.toString());
	startPointOldLevel = oldLevelSet.get(m);
	startPointNewLevel = sDAG.labels.get(m).level;
	levelDif = (byte) (startPointOldLevel - startPointNewLevel);
	
	Integer[] tempCol = new Integer[sDAG.DAG.getNeighbours(m).size()];
	tempCol = sDAG.DAG.getNeighbours(m).toArray(tempCol);
		
	for (int i = 0; i < tempCol.length; i++) {
		int child = tempCol[i];
	    if (!processed.contains(child))
	    {
	    	if (child < 0) {// a dummy child
	    		if (sDAG.labels.get(m).level != oldLevelSet.get(m)) {
	    			
		    		for (Iterator iterator2 = sDAG.DAG.getReverseNeighbours(child).iterator(); iterator2.hasNext();) {
						
		    			Integer node1 = (Integer) iterator2.next();
						Integer node2 = (Integer) iterator2.next();
						
						if (node1==m){
							if(sDAG.labels.get(m).level == sDAG.labels.get(node2).level) break;
							}
						
						else{
							if(sDAG.labels.get(m).level == sDAG.labels.get(node1).level) break;
							
						}
						
						if (node1==m) {
							sDAG.DAG.setEdge(m, node2,(float)0);
							//sDAG.DAG.visualize("src/peersim/simildis/resultNoDummy/DAGSim_dynamic_626_7_insidereqire.dot");
							oldLevelSet.put(node2, sDAG.labels.get(node2).level);
							sDAG.labels.get(node2).level = (byte) (sDAG.labels.get(m).level + 1);
							if (!processed.contains(node2));
								unTraversed.add(node2);
							sDAG.DAG.removeNode(child);
							//sDAG.DAG.visualize("src/peersim/simildis/resultNoDummy/DAGSim_dynamic_626_7_insidereqire.dot");
				    		sDAG.labels.remove(child);
						}
						else {
							sDAG.DAG.setEdge(m, node1,(float)0);
							//sDAG.DAG.visualize("src/peersim/simildis/resultNoDummy/DAGSim_dynamic_626_7_insidereqire.dot");
							oldLevelSet.put(node1, sDAG.labels.get(node1).level);
							sDAG.labels.get(node1).level = (byte) (sDAG.labels.get(m).level + 1);
							if (!processed.contains(node1));
								unTraversed.add(node1);
							sDAG.DAG.removeNode(child);
							//sDAG.DAG.visualize("src/peersim/simildis/resultNoDummy/DAGSim_dynamic_626_7_insidereqire.dot");
				    		sDAG.labels.remove(child);
						}
						break;
							
					}
		    		
	    		}
	    	}
	    	else {
	    		if (oldLevelSet.containsKey(child)){
	    			if (sDAG.labels.get(child).level==sDAG.labels.get(m).level){
	    				sDAG.DAG.clearEdge(m, child);
	    				sDAG.DAG.setEdge(m, sDAG.dummyNodeIndex,(float)0);
	    				sDAG.DAG.setEdge(child, sDAG.dummyNodeIndex,(float)0);
	    				//sDAG.DAG.visualize("src/peersim/simildis/resultNoDummy/DAGSim_dynamic_626_7_insidereqire.dot");
	    				sDAG.labels.put(sDAG.dummyNodeIndex, new NodeLabels((float)0, (byte)(sDAG.labels.get(child).level+1)));
	    				if (!processed.contains(sDAG.dummyNodeIndex));
	    					unTraversed.add(sDAG.dummyNodeIndex);
	    				sDAG.dummyNodeIndex -=1;
	    			}
	    		}
	    		else{
	    		
	    		oldLevelSet.put(child, sDAG.labels.get(child).level);
		    	sDAG.labels.get(child).level = (byte) (sDAG.labels.get(child).level - levelDif);
		    	sDAG.DAG.updateWeight(m, child, (float)0);
		    	//sDAG.DAG.visualize("src/peersim/simildis/resultNoDummy/DAGSim_dynamic_626_7_insidereqire.dot");
		    	if (!processed.contains(child));
		    		unTraversed.add(child);
	    		}
	    	}
	    }
		//sDAG.DAG.visualize("src/peersim/simildis/result/SG_debug_1.dot");
		//log(sDAG.labels.toString());
	}
	tempCol = new Integer[sDAG.DAG.getReverseNeighbours(m).size()];
	tempCol = sDAG.DAG.getReverseNeighbours(m).toArray(tempCol);
	for (int i = 0; i < tempCol.length; i++) {             ///TODOOOOOO check unprocessed
		int father = tempCol[i];
		if (!processed.contains(father)) { 
			byte nLevel = sDAG.labels.get(m).level;
			if (sDAG.labels.get(father).level  > (byte)(nLevel+1)) { // level should be changed.
				sDAG.DAG.clearEdge(father, m);
				sDAG.DAG.setEdge(m, father,(float)0);
				//sDAG.DAG.visualize("src/peersim/simildis/resultNoDummy/DAGSim_dynamic_626_7_insidereqire.dot");
				oldLevelSet.put(father, sDAG.labels.get(father).level);
				sDAG.labels.get(father).level = (byte)(nLevel +1);
				if (!processed.contains(father));
					unTraversed.add(father);
					
			}
			else if (sDAG.labels.get(father).level  == (byte)(nLevel+1)){
				sDAG.DAG.clearEdge(father, m);
				sDAG.DAG.setEdge(m, father,(float)0);
				//sDAG.DAG.visualize("src/peersim/simildis/resultNoDummy/DAGSim_dynamic_626_7_insidereqire.dot");
			}
			else if (sDAG.labels.get(father).level  == (byte)(nLevel)){
				sDAG.DAG.clearEdge(father, m);
				sDAG.DAG.setEdge(father, sDAG.dummyNodeIndex,(float)0);
				sDAG.DAG.setEdge(m, sDAG.dummyNodeIndex,(float)0);
				//sDAG.DAG.visualize("src/peersim/simildis/resultNoDummy/DAGSim_dynamic_626_7_insidereqire.dot");
				sDAG.labels.put(sDAG.dummyNodeIndex, new NodeLabels((float)0, (byte)(nLevel+1)));
				if (!processed.contains(sDAG.dummyNodeIndex));
					unTraversed.add(sDAG.dummyNodeIndex);
				sDAG.dummyNodeIndex -=1;
			}
		}
		//sDAG.DAG.visualize("src/peersim/simildis/result/SG_debug_1.dot");
		//log(sDAG.labels.toString());
	}
	//tempCol=null;
	processed.add(m);
	}
//rewireDAGBetter(oldLevelSet, sDAG, sg, unTraversed);
}


/////////////////////////////////////////////////
public void rewireDAGBetter_problem6267(HashMap<Integer, Byte> oldLevelSet, SimilarityDAG sDAG, NeighbourListGraph sg, Queue<Integer> unTraversed){
	//sDAG.DAG.visualize("src/peersim/simildis/result/ProblemSerious_b2.dot");
	//log("Before Enter -------->>>>>> "+unTraversed.toString());
	
	HashSet<Integer> processed = new HashSet<Integer>();
	
	while (!unTraversed.isEmpty()){
		// TODO: the following 2 lines could be removed.
	if (unTraversed.isEmpty())
		return;
	
	int m = unTraversed.remove(); // m with new level 
	byte startPointOldLevel;
	byte startPointNewLevel;
	byte levelDif;
	//skips all dummy nodes.
	while (m < 0){
		if (unTraversed.isEmpty())
			return;  
		m = unTraversed.remove();
		 
	}
	
	//sDAG.DAG.visualize("src/peersim/simildis/result/SG_debug_1.dot");
	//log(sDAG.labels.toString());
	startPointOldLevel = oldLevelSet.get(m);
	startPointNewLevel = sDAG.labels.get(m).level;
	levelDif = (byte) (startPointOldLevel - startPointNewLevel);
	
	Integer[] tempCol = new Integer[sDAG.DAG.getNeighbours(m).size()];
	tempCol = sDAG.DAG.getNeighbours(m).toArray(tempCol);
		
	for (int i = 0; i < tempCol.length; i++) {
		int child = tempCol[i];
	    if (!unTraversed.contains(child))
	    {
	    	if (child < 0) {// a dummy child
	    		if (sDAG.labels.get(m).level != oldLevelSet.get(m)) {
	    			
		    		for (Iterator iterator2 = sDAG.DAG.getReverseNeighbours(child).iterator(); iterator2.hasNext();) {
						
		    			Integer node1 = (Integer) iterator2.next();
						Integer node2 = (Integer) iterator2.next();
						
						if (node1==m){
							if(sDAG.labels.get(m).level == sDAG.labels.get(node2).level) break;
							}
						
						else{
							if(sDAG.labels.get(m).level == sDAG.labels.get(node1).level) break;
							
						}
						
						if (node1==m) {
							sDAG.DAG.setEdge(m, node2,(float)0);
							sDAG.DAG.visualize("src/peersim/simildis/resultNoDummy/DAGSim_dynamic_626_7_insidereqire.dot");
							oldLevelSet.put(node2, sDAG.labels.get(node2).level);
							sDAG.labels.get(node2).level = (byte) (sDAG.labels.get(m).level + 1);
							if (!processed.contains(node2));
								unTraversed.add(node2);
							sDAG.DAG.removeNode(child);
							sDAG.DAG.visualize("src/peersim/simildis/resultNoDummy/DAGSim_dynamic_626_7_insidereqire.dot");
				    		sDAG.labels.remove(child);
						}
						else {
							sDAG.DAG.setEdge(m, node1,(float)0);
							sDAG.DAG.visualize("src/peersim/simildis/resultNoDummy/DAGSim_dynamic_626_7_insidereqire.dot");
							oldLevelSet.put(node1, sDAG.labels.get(node1).level);
							sDAG.labels.get(node1).level = (byte) (sDAG.labels.get(m).level + 1);
							if (!processed.contains(node1));
								unTraversed.add(node1);
							sDAG.DAG.removeNode(child);
							sDAG.DAG.visualize("src/peersim/simildis/resultNoDummy/DAGSim_dynamic_626_7_insidereqire.dot");
				    		sDAG.labels.remove(child);
						}
						break;
							
					}
		    		
	    		}
	    	}
	    	else {
	    		if (oldLevelSet.containsKey(child)){
	    			if (sDAG.labels.get(child).level==sDAG.labels.get(m).level){
	    				sDAG.DAG.clearEdge(m, child);
	    				sDAG.DAG.setEdge(m, sDAG.dummyNodeIndex,(float)0);
	    				sDAG.DAG.setEdge(child, sDAG.dummyNodeIndex,(float)0);
	    				sDAG.DAG.visualize("src/peersim/simildis/resultNoDummy/DAGSim_dynamic_626_7_insidereqire.dot");
	    				sDAG.labels.put(sDAG.dummyNodeIndex, new NodeLabels((float)0, (byte)(sDAG.labels.get(child).level+1)));
	    				if (!processed.contains(sDAG.dummyNodeIndex));
	    					unTraversed.add(sDAG.dummyNodeIndex);
	    				sDAG.dummyNodeIndex -=1;
	    			}
	    		}
	    		else{
	    		
	    		oldLevelSet.put(child, sDAG.labels.get(child).level);
		    	sDAG.labels.get(child).level = (byte) (sDAG.labels.get(child).level - levelDif);
		    	sDAG.DAG.updateWeight(m, child, (float)0);
		    	sDAG.DAG.visualize("src/peersim/simildis/resultNoDummy/DAGSim_dynamic_626_7_insidereqire.dot");
		    	if (!processed.contains(child));
		    		unTraversed.add(child);
	    		}
	    	}
	    }
		//sDAG.DAG.visualize("src/peersim/simildis/result/SG_debug_1.dot");
		//log(sDAG.labels.toString());
	}
	tempCol = new Integer[sDAG.DAG.getReverseNeighbours(m).size()];
	tempCol = sDAG.DAG.getReverseNeighbours(m).toArray(tempCol);
	for (int i = 0; i < tempCol.length; i++) {             ///TODOOOOOO check unprocessed
		int father = tempCol[i];
		if (!unTraversed.contains(father)) { 
			byte nLevel = sDAG.labels.get(m).level;
			if (sDAG.labels.get(father).level  > (byte)(nLevel+1)) { // level should be changed.
				sDAG.DAG.clearEdge(father, m);
				sDAG.DAG.setEdge(m, father,(float)0);
				sDAG.DAG.visualize("src/peersim/simildis/resultNoDummy/DAGSim_dynamic_626_7_insidereqire.dot");
				oldLevelSet.put(father, sDAG.labels.get(father).level);
				sDAG.labels.get(father).level = (byte)(nLevel +1);
				if (!processed.contains(father));
					unTraversed.add(father);
					
			}
			else if (sDAG.labels.get(father).level  == (byte)(nLevel+1)){
				sDAG.DAG.clearEdge(father, m);
				sDAG.DAG.setEdge(m, father,(float)0);
				sDAG.DAG.visualize("src/peersim/simildis/resultNoDummy/DAGSim_dynamic_626_7_insidereqire.dot");
			}
			else if (sDAG.labels.get(father).level  == (byte)(nLevel)){
				sDAG.DAG.clearEdge(father, m);
				sDAG.DAG.setEdge(father, sDAG.dummyNodeIndex,(float)0);
				sDAG.DAG.setEdge(m, sDAG.dummyNodeIndex,(float)0);
				sDAG.DAG.visualize("src/peersim/simildis/resultNoDummy/DAGSim_dynamic_626_7_insidereqire.dot");
				sDAG.labels.put(sDAG.dummyNodeIndex, new NodeLabels((float)0, (byte)(nLevel+1)));
				if (!processed.contains(sDAG.dummyNodeIndex));
					unTraversed.add(sDAG.dummyNodeIndex);
				sDAG.dummyNodeIndex -=1;
			}
		}
		//sDAG.DAG.visualize("src/peersim/simildis/result/SG_debug_1.dot");
		//log(sDAG.labels.toString());
	}
	//tempCol=null;
	processed.add(m);
	}
//rewireDAGBetter(oldLevelSet, sDAG, sg, unTraversed);
}



/////////////////////////////////////////////////
public static void log(String s){
		System.out.println(s);
	}
	
	private boolean haveCommonDummy(Collection<Integer> n1, Collection<Integer> n2 ){
		
		for (Integer i : n1) {
			for (Integer j: n2) {
				if (i==j && i<0)
					return true;
				
			}
			
		}
		return false;
	}
	
class NodeLevel{
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + getOuterType().hashCode();
		result = prime * result + level;
		result = prime * result + nodeId;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NodeLevel other = (NodeLevel) obj;
		if (!getOuterType().equals(other.getOuterType()))
			return false;
		if (nodeId != other.nodeId)
			return false;
		return true;
	}
	public NodeLevel(int nodeId, byte level) {
		super();
		this.nodeId = nodeId;
		this.level = level;
	}
	public int nodeId;
	public byte level;
	private SimUpdatControl getOuterType() {
		return SimUpdatControl.this;
	}
}

class SimpleEdge implements Comparable<SimpleEdge>{
	private  final int i;
	public int getI() {
		return i;
	}

	public int getJ() {
		return j;
	}

	private final int j;
	private final int c;
	public int getC() {
		return c;
	}
	
	SimpleEdge(int i , int j, int c){
		this.i = i;
		this.j = j;
		this.c = c;
	}
	
	@Override
	public boolean equals(Object o2){
		
		return ( this.i==((SimpleEdge)o2).i &&  this.j==((SimpleEdge)o2).j ) || ( this.i==((SimpleEdge)o2).j &&  this.j==((SimpleEdge)o2).i ) ;
	}
	
	@Override
	public int compareTo(SimpleEdge o2) {
			return 0;
	}
	public String toString(){
		return "("+i+","+j+")";
	}
	
	@Override
	public int hashCode(){
		return (10711 * (10711 * getI()+getJ())) + (10711 * (10711 * getJ()+getI())); 
	}
}

@Override
public boolean execute() {
	// TODO Auto-generated method stub
	return false;
}
	
}
