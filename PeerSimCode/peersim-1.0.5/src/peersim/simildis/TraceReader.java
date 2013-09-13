package peersim.simildis;

import java.io.FileNotFoundException;

import peersim.config.*;
import peersim.core.*;
import java.io.IOException;  
import java.io.RandomAccessFile;

public class TraceReader implements Control{

	
    private static final String PAR_PROT = "protocol";
    private static final String PAR_TRACE_FILE ="file_name";
    private static final String LINES_PER_CYCLE ="line_per_cycle";
    private static final String TOTAL_LENGTH ="total_lines";
    private static final String HEADER_LENGTH ="header_length";
    private static final String LINE_LENGTH ="line_length";
    

    private final String name;

    private final int pid;
    private final String fileName;
    private final int linesPerCycle;
    private final int totalLength;
    private final int headerLength; // in byte
    private final int lineLength; // in byte
    
    private RandomAccessFile fileHandler ;
    
    private int passed_lines;
    

    public TraceReader(String name) {
        this.name = name;
        
        pid = Configuration.getPid(name + "." + PAR_PROT);
        fileName = Configuration.getString(name + "." + PAR_TRACE_FILE);
        linesPerCycle = Configuration.getInt(name + "." + LINES_PER_CYCLE);
        totalLength = Configuration.getInt(name + "." + TOTAL_LENGTH);
        headerLength = Configuration.getInt(name + "." + HEADER_LENGTH);
        lineLength = Configuration.getInt(name + "." + LINE_LENGTH);
        
        //traceReader = new ReadWithScanner(fileName);
        try {
			fileHandler = new RandomAccessFile(fileName,"r");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        this.passed_lines = 0;
    }
	
	
	@Override
	public boolean execute(int exp) {
		// TODO Auto-generated method stub
		//fileHandler = new RandomAccessFile(fileName,"r");
		try {
			fileHandler = new RandomAccessFile(fileName,"r");
			
			try {
				this.fileHandler.seek(this.headerLength + this.passed_lines * this.lineLength);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			int[][] records = processLineByLine(linesPerCycle, lineLength, fileHandler);
			for (int i = 0; i < records.length; i++) {
				int peer_id_from = records[i][0];
				int peer_id_to = records[i][1];
				int transfer = records[i][2];
				

				{
					SimilDis protocol = (SimilDis) Network.get(peer_id_from).getProtocol(pid);
				//if (protocol.subjectiveGraph.updateWeight(peer_id_from, peer_id_to, transfer))
				//	{
						// TEST **************************** TEST
						//protocol.newlyReceivedInfo.add(new InfoEdge(peer_id_from,peer_id_to,transfer));
				//	}
				}
				

				{
				SimilDis protocol1 = (SimilDis) Network.get(peer_id_to).getProtocol(pid);
				//if(protocol1.subjectiveGraph.updateWeight(peer_id_from, peer_id_to, transfer))
				//	{
						// TEST **************************** TEST
						//protocol1.newlyReceivedInfo.add(new InfoEdge(peer_id_from,peer_id_to,transfer));
				//	}
				}
				this.passed_lines +=1;
				
				// TEST	 **************************** TEST 
				int testid = 1;
				//if (peer_id_from!= testid && peer_id_to!=testid){
				SimilDis protocol2 = (SimilDis) Network.get(testid).getProtocol(pid);
				protocol2.newlyReceivedInfo.add(new InfoEdge(peer_id_from,peer_id_to,transfer));
				//}
				
				}
			long time = peersim.core.CommonState.getTime();
			//log("----------------------------------------------------------------------------------------------------------------------------");
			//log("Cycle: "+ time+" ---> Reading is done." );
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			fileHandler.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	///////////////////////
	public static final int[][] processLineByLine(int numberOfLines, int lineLen,  RandomAccessFile fh) throws FileNotFoundException {
		int[][] result = new int[numberOfLines][3] ;
		
		int lineCounter = 0;

		//long current_cycle = peersim.core.CommonState.getTime();
		//log("Recently read records: ==================== TIME:  "+ current_cycle+ "  >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		
		try {
			long len= fh.length();			
			while ( lineCounter < numberOfLines && fh.getFilePointer() < len){
				byte[] b = new byte[lineLen];
				fh.read(b);  
				String line = new String(b);
				//log(fh.getFilePointer()+" "+ len);
				//log(line);
				int[] record = processLine(line);
			  if (record != null ){
				  result[lineCounter][0]=record[0];
				  result[lineCounter][1]=record[1];
				  result[lineCounter][2]=record[2];
				  lineCounter +=1;
				  
			  }
			
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	    return result;
	  }
	
	///////////////////////
	private static void log(Object aObject){
		    System.out.println(String.valueOf(aObject));
		  }
	
	protected static int[] processLine(String aLine){
	    //use a second Scanner to parse the content of each line
		int[] r = new int[3];
	    if (aLine.charAt(0)=='#')
	    	return null;
	    String[] lineStr = aLine.split("\\|");
	    r[0] = Integer.parseInt(lineStr[0].replaceAll(" ", ""));
	    r[1] = Integer.parseInt(lineStr[1].replaceAll(" ", ""));
	    r[2] = Integer.parseInt(lineStr[2].replaceAll(" ", ""));
	    
	    
	    return r;
	  }


	@Override
	public boolean execute() {
		// TODO Auto-generated method stub
		return false;
	}

}
