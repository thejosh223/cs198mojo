package peersim.graph;

import java.awt.Label;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.xml.crypto.dsig.keyinfo.RetrievalMethod;


import peersim.simildis.LeveldNode;
import peersim.graph.*;
public class NeighbourListGraph implements Graph, java.io.Serializable {

/**
	 * 
	 */
private static final long serialVersionUID = 1L;

private final Set<Integer> nodes;

private final boolean directed;

private static final int PRIME1 = 12473;
private static final int PRIME2 = 16729;

private final HashMap<Integer, Float> weights;

private final HashMap<Integer, Set<Integer>> neighbors;
private final HashMap<Integer, Set<Integer>> reverseNeighbors;


public NeighbourListGraph( boolean directed ) {

	nodes = new HashSet<Integer>();	
	weights = new HashMap<Integer, Float>();
	
	this.directed = directed;
	neighbors = new HashMap<Integer, Set<Integer>>();
	reverseNeighbors = new HashMap<Integer, Set<Integer>>();
	
}

public Set<Integer> getNodes(){
	return this.nodes;
}

public Integer[][] removeAllEdges(int nodeId){

	Integer out[] = this.removeOutEdges(nodeId);
	Integer in[] = this.removeInEdges(nodeId);
	
	Integer [][] res = new Integer[2][];
	//HashMap<Integer, Integer[]> res = new HashMap<Integer, Integer[]>();
	res[0] = out;
	res[1] = in;
	//res.put(0, out);
	//res.put(1, in);
	return res;
}

public Integer[] removeOutEdges(int nodeId){
	for (Iterator iterator = this.neighbors.get(nodeId).iterator(); iterator.hasNext();) {
		Integer n = (Integer) iterator.next();
		this.reverseNeighbors.get(n).remove(nodeId);
		this.weights.remove(nodeId*PRIME1+n*PRIME2);
		
	}
	Integer[] result = new Integer[this.neighbors.get(nodeId).size()];
	result = this.neighbors.get(nodeId).toArray(result);
	this.neighbors.get(nodeId).clear();
	return result;
}

public Integer[] removeInEdges(int nodeId){
	Integer[] result = new Integer[this.reverseNeighbors.get(nodeId).size()];
	int i = 0;
	for (Iterator iterator = this.reverseNeighbors.get(nodeId).iterator(); iterator.hasNext();) {
		Integer n = (Integer) iterator.next();
		this.neighbors.get(n).remove(nodeId);
		this.weights.remove(n*PRIME1+nodeId*PRIME2);
		result[i] = n;
		i+=1;
		
	}
	this.reverseNeighbors.get(nodeId).clear();
	
	return result;
}



public NeighbourListGraph( int size, boolean directed ) {

	nodes = null;
	this.directed = directed;
	weights = new HashMap<Integer, Float>();
	neighbors = new HashMap<Integer, Set<Integer>>();
	reverseNeighbors = new HashMap<Integer, Set<Integer>>();
}

public int addNode( int o ) {
	if (!nodes.contains(o)) {
		neighbors.put(o,  new HashSet<Integer>());
		reverseNeighbors.put(o, new HashSet<Integer>());
		nodes.add(o);
		}
	return o;
	}


public void removeNode(int node){
	
	Iterator<Integer> it = neighbors.keySet().iterator();
	while (it.hasNext()){
		int key = it.next();
		neighbors.get(key).remove(node);
		weights.remove(node*PRIME1+key*PRIME2);
		weights.remove(key*PRIME1+node*PRIME2);
	}
	

	Iterator<Integer> it2 = reverseNeighbors.keySet().iterator();
	while (it2.hasNext()){
		int key = it2.next();
		reverseNeighbors.get(key).remove(node);
	}
	
	nodes.remove(node);
	neighbors.remove(node);
	reverseNeighbors.remove(node);
		
}


public List<LeveldNode> bfsTraverse(int startPoint){
	//BFS uses Queue data structure
	List<LeveldNode> result = new ArrayList<LeveldNode>();
	
	Queue<LeveldNode> q=new LinkedList<LeveldNode>();
	Set<Integer> visited = new HashSet<Integer>();
	LeveldNode n = new LeveldNode(startPoint,(byte)0);
	q.add(n);
	
	//int level = -1;
	result.add(n);

	visited.add(startPoint);

	while(!q.isEmpty())
	{
		n=q.remove();

		for (Iterator iterator = getNeighbours(n.nodeId).iterator(); iterator.hasNext();) {
			int child = (Integer) iterator.next();
		    if (!visited.contains(child))
		    {	visited.add(child);

				result.add(new LeveldNode(child, (byte)(n.level+1)));
				q.add(new LeveldNode(child, (byte)(n.level+1)));

		    }
		}
		
		
	}
	return result;
}

/////////////

public HashMap<Integer, Set<Integer>> bfsTraverseUndirected(int startPoint){
	// unlike the directed version, this method does not consider the edges' direction. 
	/**
	 * @return: a hasmap that key is the level id and value is the list of the nodes in that level.
	 */
	HashMap<Integer, Set<Integer>> result = new HashMap<Integer, Set<Integer>>();
	Queue<LeveldNode> q=new LinkedList<LeveldNode>();
	Set<Integer> visited = new HashSet<Integer>();
	LeveldNode n;
	n = new LeveldNode(startPoint,(byte)0);
	
	q.add(n);
	
	if (!result.keySet().contains(0))  {
		result.put(0, new HashSet<Integer>());
	} 
	result.get(0).add(startPoint);
	
	visited.add(startPoint);
	LeveldNode m;
	while(!q.isEmpty())
	{
		n=q.remove();

		for (Iterator iterator = neighbors.get(n.nodeId).iterator(); iterator.hasNext();) {
			int child = (Integer) iterator.next();
		    if (!visited.contains(child))
		    {
		    	
		    	visited.add(child);
		    	m = new LeveldNode(child, (byte)(n.level+1));
		    	if (!result.keySet().contains(n.level+1))  {
		    		result.put(n.level+1, new HashSet<Integer>());
		    	} 
		    	result.get(n.level+1).add(child);
				q.add(m);
				
		    }   
		}
		
		for (Iterator iterator = reverseNeighbors.get(n.nodeId).iterator(); iterator.hasNext();) {
			int child = (Integer) iterator.next();
		    if (!visited.contains(child))
		    {	
		    	
		    	visited.add(child);
		    	m = new LeveldNode(child, (byte)(n.level+1));
		    	if (!result.keySet().contains(n.level+1))  {
		    		result.put(n.level+1, new HashSet<Integer>());
		    	} 
		    	result.get(n.level+1).add(child);
				q.add(m);
		    }   
		}
	}
	return result;
}

public NeighbourListGraph getComponent(int startPoint, List<Integer> sameLevelNodes){
	// unlike the directed version, this method does not consider the edges' direction. 
	/**
	 * @return: a hasmap that key is the level id and value is the list of the nodes in that level.
	 */
	
	NeighbourListGraph comp = new NeighbourListGraph(true);
	
	comp.addNode(startPoint);
	Queue<Integer> q=new LinkedList<Integer>();
	Set<Integer> visited = new HashSet<Integer>();
	visited.add(startPoint);
	q.add(startPoint);
	while(!q.isEmpty())
	{
		int i= q.remove();
		for (Integer j : getNeighbours(i)) {
			if (sameLevelNodes.contains(j)) {
				comp.setEdge(i, j, getBiDiEdgeWeights(i, j));
				
				if (!visited.contains(j) ){
					 q.add(j);
					 visited.add(j);
				 }
			}
			
		}
		for (Integer j : getReverseNeighbours(i)) {
			if(sameLevelNodes.contains(j)){
				comp.setEdge(j, i,getBiDiEdgeWeights(i, j));
				
				if (!visited.contains(j)){
						 q.add(j);
						 visited.add(j);
					}
			}

		}
	}
	return comp;
}





public float totalCapacity(int i){
	float capacity = 0;
	for (Iterator<Integer> iterator = getNeighbours(i).iterator(); iterator.hasNext();) {
		int j = iterator.next();
		capacity += getBiDiEdgeWeights(i, j);
		
	}
	return capacity;
}

public float totalOutCapacity(int i){
	float capacity = 0;
	for (Iterator<Integer> iterator = getNeighbours(i).iterator(); iterator.hasNext();) {
		int j = iterator.next();
		capacity += getEdgeWeight(i, j);
		
	}
	return capacity;
}

public float totalInCapacity(int j){
	float capacity = 0;
	for (Iterator<Integer> iterator = reverseNeighbors.get(j).iterator(); iterator.hasNext();) {
		int i = iterator.next();
		capacity += getEdgeWeight(i, j);
		
	}
	return capacity;
	
}

// =================== graph implementations ======================
// ================================================================


public void setEdge( int i, int j, float w ) {
	
	if (i==0 || j ==0 || i==j)
		return;
	if (!nodes.contains(i))
		addNode(i);
	
	if (!nodes.contains(j))
		addNode(j);
	
	neighbors.get(i).add(j);
	reverseNeighbors.get(j).add(i);

	
	weights.put(i*PRIME1+j*PRIME2, w);
	if( !directed ) 
		{
			weights.put(j*PRIME1+i*PRIME2, w);
			neighbors.get(j).add(i);
		}
	}

//----------------------------------------------------------------

public boolean updateWeight( int i, int j, float newWeight ) {
	/**
	 * If the edge already exists, it simple updates the weight of the edge, otherwise add a new edge.
	 * 
	 * */
	
	
	if (!this.hasEdge(i, j)) { 
			this.setEdge(i, j, newWeight);
			return true;
		} 
	else{   
			if (weights.get(i*PRIME1+j*PRIME2)==newWeight)
				return false;
			else{
			weights.put(i*PRIME1+j*PRIME2, newWeight);
			if( !directed )	{weights.put(j*PRIME1+i*PRIME2, newWeight);}
			}
		}
	return false;
}

// ---------------------------------------------------------------

public boolean clearEdge( int i, int j ) {
	
	if (neighbors.keySet().contains(i)){
		neighbors.get(i).remove(j);
		weights.remove(i*PRIME1+j*PRIME2);
		
		reverseNeighbors.get(j).remove(i);
		
		if( !directed ) {
			neighbors.get(j).remove(i);
			weights.remove(j*PRIME1+i*PRIME2);
			}
	}
		
	
	
	return true;
	
}

// ---------------------------------------------------------------

public boolean isEdge(int i, int j) {
	
	if (nodes.contains(i) && nodes.contains(j) )
		return neighbors.get(i).contains(j);
	else
		return false;
}

public boolean hasUnEdge(int i, int j){
	if (!nodes.contains(i))
		return false;
	
	if (!nodes.contains(j))
		return false;
	return ( neighbors.get(i).contains(j) || neighbors.get(j).contains(i) );
}
// ---------------------------------------------------------------

public Collection<Integer> getReverseNeighbours(int i) {
	
	return Collections.unmodifiableCollection(reverseNeighbors.get(i));
}
//----------------------------------------------------------------

public Collection<Integer> getNeighbours(int i) {
	
	return Collections.unmodifiableCollection(neighbors.get(i));
}

//public Collection<Integer> getNeighbours(int i) {
	
//	return null;//Collections.unmodifiableCollection(neighbors.get(i));
//}

// ---------------------------------------------------------------

/** If the graph was gradually grown using {@link #addNode}, returns the
* object associated with the node, otherwise null */
public Integer getNode(int i) { return (nodes==null?null:i); }
	
// ---------------------------------------------------------------

/**
* Returns null always. 
*/
public Object getEdge(int i, int j) { return null; }

// ---------------------------------------------------------------

public int size() { return nodes.size(); }

// --------------------------------------------------------------------
	
public boolean directed() { return directed; }

// --------------------------------------------------------------------

public int degree(int i) { return neighbors.get(i).size(); }


public float getEdgeWeight(int i, int j){
	try{
		return weights.get(i*PRIME1+j*PRIME2);
	}catch(Exception e){
		return 0f;
	} 
}

////////
public float getBiDiEdgeWeights(int i, int j){
	
	try{
		return getEdgeWeight(i, j)+getEdgeWeight(j, i);
	}catch(Exception e){
		return 0f;
	}
	
	//return (getEdgeWeight(i, j)+getEdgeWeight(j,i));
}

////////
public boolean hasNode(int i){
	return (nodes.contains(i));
}

/////////
public boolean hasEdge(int i, int j){
	if (!nodes.contains(i))
		return false;
	
	if (!nodes.contains(j))
		return false;
	return (neighbors.get(i).contains(j));
}

public void destroyGraph(){
	
}

public int getNumberOfNodes(){
	return nodes.size();
}

public int getNumberOfEdges(){

	return weights.keySet().size();
}


public void logDegreeDist(String fileName ){
	FileOutputStream myFile;
	try {
		myFile = new FileOutputStream(fileName);
		PrintStream out = new PrintStream(myFile);
		out.println("#index|degree");
		int indexer = 1;
		for(Integer i: this.nodes){
			
			int outDegree = this.neighbors.get(i).size();
			int inDegree = this.reverseNeighbors.get(i).size();
			
			out.println(indexer+"|"+ (outDegree+inDegree));
			indexer++;
			
		}
		
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
		
	
}

public void visualize(String fileName){
	FileOutputStream myFile;
	try {
		myFile = new FileOutputStream(fileName);

		PrintStream out = new PrintStream(myFile);
		out.println((this.directed()?"digraph":"graph")+" {");
		out.println("edge  [fontsize=6];");
		String normalNodes = " node [shape=circle,width=0.2,height=0.2,fontsize=8]; ";
		String dummyNodes = " node [shape=hexagon,color = red, style=filled ,width=0.1,height=0.1,fontsize=8]; ";
		
		int l = dummyNodes.length();
		for (Iterator iterator = this.nodes.iterator(); iterator.hasNext();) {
			Integer i = (Integer) iterator.next();
			if (i<0)
				dummyNodes +=" "+i+";";
			else
				normalNodes +=" "+i+";";
			
		}
		if (dummyNodes.length() == l)
			dummyNodes ="";
		out.println(normalNodes);
		out.println(dummyNodes);
		
		NumberFormat formatter = new DecimalFormat("#0.00000000");
		for (Iterator<Integer> iterator = this.neighbors.keySet().iterator(); iterator.hasNext();) {
			int i =  iterator.next();
			Iterator<Integer> it=this.getNeighbours(i).iterator();
			while(it.hasNext())
			{
				final int j = it.next();
				if(this.directed())
					
					if (this.getEdgeWeight(i, j) !=0)
						out.println(i+" -> "+j+" [ label= \""+formatter.format(this.getEdgeWeight(i, j))+"\" ];");
					else
						out.println(i+" -> "+j+" [color= green, label= \""+formatter.format(this.getEdgeWeight(i, j))+"\" ];");
				
				else if( i<=j )
					out.println(i+" -- "+j+" [ label = \""+formatter.format(this.getBiDiEdgeWeights(i, j))+"\" ];");
			}
		}
		
		out.println("}");
		
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	
}


public String toString(){
	
	String s= "Nodes:";

	s+=this.nodes.toString()+"\n";
	s+="Edges:";
	
	for (Iterator<Integer> iterator = nodes.iterator(); iterator.hasNext();) {
		int i = iterator.next();
		
		if (neighbors.get(i).size()>0 ) 
		{
				Iterator<Integer> it = neighbors.get(i).iterator();
				while(it.hasNext())
				{
					int tail = it.next();
					int head =  getNode(i);
					s+="("+head+","+tail+","+ weights.get(head*PRIME1+tail*PRIME2)+") ";
				}
		}
	}

	return s;
	
}


public float calcReputation(int evaluator , int evaluatee, int maxHops){
	
	float outgoingFlow = maxflow(evaluator, evaluatee, maxHops);
	float incomingFlow = maxflow(evaluatee, evaluator, maxHops);
	
	float W_ij = (float) (Math.atan(outgoingFlow)/(Math.PI/2));
	float W_ji = (float) (Math.atan(incomingFlow)/(Math.PI/2));
	
	return W_ji * (1- W_ij);
	
}


public float maxflow(int start_point, int end_point, int maxHops){
	
	if (!(nodes.contains(start_point) && nodes.contains(end_point)))
		return 0;
	
	HashMap<Integer, Set<Integer>> arcscopy = this.neighbors;
	HashMap<Integer, Set<Integer>> backarcscopy = this.reverseNeighbors;
	
	HashMap<Integer, Float> flows = new HashMap<Integer, Float>();
	
	HashMap<Integer, Float> weightsCopy = this.weights; 
	
	HashMap<Integer, Integer> unscanned = new HashMap<Integer, Integer>();
	HashSet<Integer> scanned = new HashSet<Integer>();
	HashMap<Integer, FordLabel> lables = new HashMap<Integer, FordLabel>();
	
	HashMap<Integer, Integer> tempUnscannedAdd = new HashMap<Integer, Integer>();
	HashSet<Integer> tempUnscannedRemove = new HashSet<Integer>();
	
	while (true){
		lables.clear();
		lables.put(start_point, new FordLabel(0,false,-1));
		
		unscanned.clear();
		unscanned.put(start_point, 0);
		
		scanned.clear();
		
		boolean doRoutineB = false;
		while (true){ // routine A
			tempUnscannedAdd.clear();
			tempUnscannedRemove.clear();
			
			for (Integer node : unscanned.keySet()) {
				
				for (Integer outnode : arcscopy.get(node)) {
					
					if (scanned.contains(outnode) || unscanned.containsKey(outnode) || tempUnscannedAdd.containsKey(outnode)) continue;
					
					Integer s = node*PRIME1+outnode*PRIME2;
					if (!flows.containsKey(s))
						flows.put(s, (float)0);
					if (weightsCopy.get(s) <= flows.get(s) || unscanned.get(node)+1 > maxHops ) continue;
					
					lables.put(outnode	, new FordLabel(node, true, myMin(lables.get(node).cap	, weightsCopy.get(s) - flows.get(s))) );
					int temp = unscanned.get(node);
					tempUnscannedAdd.put(outnode, temp+1);
				}
				
				for (Integer innode : backarcscopy.get(node)) {
					
					if (scanned.contains(innode) || unscanned.containsKey(innode) || tempUnscannedAdd.containsKey(innode)) continue;
					Integer s = innode*PRIME1+node*PRIME2;
					if (!flows.containsKey(s))
						flows.put(s, (float)0);
					if (flows.get(s)==0 || unscanned.get(node)+1 > maxHops ) continue;
					
					lables.put(innode , new FordLabel(node, false, myMin(lables.get(node).cap	, flows.get(s))) );
					int temp = unscanned.get(node);
					tempUnscannedAdd.put(innode, temp - 1);
				}
			
			tempUnscannedRemove.add(node);
			scanned.add(node);
			if (scanned.contains(end_point)) 	
				{
					doRoutineB = true;
					break;
				}
			}
			
			if (doRoutineB) break;
			
			for(Integer i: tempUnscannedAdd.keySet()){
				unscanned.put(i, tempUnscannedAdd.get(i));
			}
			
			for(Integer i: tempUnscannedRemove){
				unscanned.remove(i);
			}
			
			if (unscanned.isEmpty()) {
				break;
			}
			
		} //end Routine A
			
			if (doRoutineB){
				doRoutineB = false;
				int s = end_point;
				FordLabel fl = lables.get(s);
				float et = fl.cap;
				while (true){
					if (s==start_point)
						break;
					fl = lables.get(s);
					if (fl.direction==true) {
						float f = flows.get(fl.node*PRIME1+s*PRIME2);
						flows.put(fl.node*PRIME1+s*PRIME2 , f + et);
					}
					else{
						float f = flows.get(s*PRIME1+fl.node*PRIME2);
						flows.put(s*PRIME1+fl.node*PRIME2 , f - et);
					}
					
					s = fl.node;
				}
				
			}
			else {
				float sum = 0;
				for (Integer innode :backarcscopy.get(end_point)) {
					int s= innode*PRIME1+end_point*PRIME2;
					if (flows.containsKey(s))
						sum += flows.get(innode*PRIME1+end_point*PRIME2);
				}
				return sum;
			}
		//} //inner while
	} // outer while 
}

public static float myMin(float a, float b){
	if (a==-1) return b;
	if (b==-1) return a;
	return Math.min(a, b);
}

class FordLabel{
	int node;
	boolean direction; 
	float cap;
	public FordLabel(int node, boolean direction, float cap) {
		super();
		this.node = node;
		this.direction = direction;
		this.cap = cap;
	}
	
	public String toString(){
		return "("+node+","+direction+","+cap+")";
	}
	
}




public static void main(String args[]){
	NeighbourListGraph g = new NeighbourListGraph(true);
	float delta = (float) -0.25;
	int currentLevel = 1;
	float thetta = (float)0.8;
	float f = (float) (0.0 + Math.pow(thetta, (double)(currentLevel+1)) * delta);
	
	
	g.setEdge(1, 2, 1);
	g.setEdge(2, 3, 1);
	g.setEdge(4, 2, 100);
	g.setEdge(3, 5, 1);
	g.setEdge(5, 4, 1);
	
	g.setEdge(6, 7, 5);
	g.setEdge(6, 3,12);
	g.setEdge(1, 8, 32);
	g.setEdge(5, 2, 52);
	g.setEdge(5, 3, 52);
	g.setEdge(2, 1, 11);
	g.setEdge(1, 6, 52);	
	g.setEdge(2, 8, 6);
	g.setEdge(6, 3, 6);
	g.setEdge(3, 8, 16);
	
	System.out.println(g.getEdgeWeight(2,8));
	
	/*System.out.println("neighbors: "+g.neighbors);
	System.out.println("reverse neighbors: "+g.reverseNeighbors);
	System.out.println("weights: "+g.weights);
	
	System.out.println("nodes: "+g.nodes);
	

	Integer[][] res = g.removeAllEdges(3);
	
	for (int i = 0; i < res[1].length; i++) {
		System.out.println(res[1][i]);	
	}
	

	System.out.println("After removal");
	System.out.println("neighbors: "+g.neighbors);
	System.out.println("reverse neighbors: "+g.reverseNeighbors);
	System.out.println("weights: "+g.weights);
	System.out.println("nodes: "+g.nodes);*/
	
	//System.out.println("Maxflow="+g.maxflow(4, 8, 50));
	System.out.println("Reputation: "+g.calcReputation(4, 6, 3));
	g.visualize("src/peersim/simildis/result/graphViz.txt");
}

@Override
public boolean setEdge(int i, int j) {
	return false;
}


}