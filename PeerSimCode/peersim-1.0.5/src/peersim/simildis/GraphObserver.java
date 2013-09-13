package peersim.simildis;

import peersim.config.*;
import peersim.core.*;
import peersim.util.*;

/**
 */
public class GraphObserver implements Control {

    // ///////////////////////////////////////////////////////////////////////
    // Constants
    // ///////////////////////////////////////////////////////////////////////

    /**
     * The protocol to operate on.
     * 
     * @config
     */
    private static final String PAR_PROT = "protocol";

    // ///////////////////////////////////////////////////////////////////////
    // Fields
    // ///////////////////////////////////////////////////////////////////////

    /**
     * The name of this observer in the configuration file. Initialized by the
     * constructor parameter.
     */
    private final String name;

    /** Protocol identifier, obtained from config property {@link #PAR_PROT}. */
    private final int pid;

    // ///////////////////////////////////////////////////////////////////////
    // Constructor
    // ///////////////////////////////////////////////////////////////////////

    /**
     * Standard constructor that reads the configuration parameters. Invoked by
     * the simulation engine.
     * 
     * @param name
     *            the configuration prefix for this class.
     */
    public GraphObserver(String name) {
        this.name = name;
        pid = Configuration.getPid(name + "." + PAR_PROT);
    }

    // ///////////////////////////////////////////////////////////////////////
    // Methods
    // ///////////////////////////////////////////////////////////////////////

    // Comment inherited from interface
    public boolean execute(int exp) {
        long time = peersim.core.CommonState.getTime();

        //for (int i = 1; i < Network.size(); i++) {
        //int i=1;
            //SimilDis protocol = (SimilDis) Network.get(i).getProtocol(pid);
            //log("graph pidddddddddddddd:"+pid);
            //System.out.println("\n ..................................................................................................................");
            //System.out.println("Time:"+time+"\t Node "+i+" is:");
            //System.out.println("----------------------------- SG  --------------------------------");
            //System.out.println(protocol.subjectiveGraph.toString());
            //System.out.println("..................................................................................................................");
            //System.out.println("Time:"+time+"\t Node "+i+":");
            //System.out.println(protocol.similarityGraph.DAG.toString());
            //System.out.println(" ====================== Similarity List ==========================");
            //System.out.println(protocol.similarityGraph.labels);
    	    /*if (i==1 )

            {
            	protocol.similarityGraph.DAG.visualize("src/peersim/simildis/resultNoDummy/DAGSim_dynamic_"+i+".dot");
            	protocol.subjectiveGraph.visualize("src/peersim/simildis/resultNoDummy/SG_Dynamic_"+i+".dot");
            	log(protocol.similarityGraph.DAG.toString());
            }*/
        //}
        //log("Cycle: "+ time+" ---> Graph Observer is done." );
        //SimilDis protocol = (SimilDis) Network.get(1).getProtocol(pid);
        //if (time>140)
       	//protocol.similarityGraph.DAG.visualize("src/peersim/simildis/resultNoDummy/DAGSim_dynamic_.dot");
        return false;
    }
    
    public void log(String s){
    	System.out.println(s);
    }

	@Override
	public boolean execute() {
		// TODO Auto-generated method stub
		return false;
	}

}
