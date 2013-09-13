package peersim.simildis;

import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;

public class SimilarityListObserver implements Control{


    private static final String PAR_PROT = "protocol";
    private final String name;

    private final int pid;

    public SimilarityListObserver(String name) {
        this.name = name;
        pid = Configuration.getPid(name + "." + PAR_PROT);
    }

    public boolean execute(int exp) {
        long time = peersim.core.CommonState.getTime();

        //for (int i = 0; i < Network.size(); i++) {
            //SimilDis protocol = (SimilDis) Network.get(i).getProtocol(pid);
            
            //log("simi pidddddddddddddd:"+pid);
            //System.out.println("Similarity list, Node "+i+":");
            //System.out.println(protocol.similarityList.toString()+"\n");
            
        //}
        
        //TEST
        //SimilDis protocol = (SimilDis) Network.get(1).getProtocol(pid);
        //log(protocol.similarityList.toString()+"\n");
        //log("Cycle: "+ time+" ---> Similarity list update is done." );
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

