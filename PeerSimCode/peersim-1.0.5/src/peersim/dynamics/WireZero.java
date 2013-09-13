package peersim.dynamics;


import peersim.graph.*;


public class WireZero extends WireGraph {

// ===================== initialization ==============================
// ===================================================================

public WireZero(String prefix) { super(prefix); }

// ===================== public methods ==============================
// ===================================================================


public void wire(Graph g) {
	
	GraphFactory.wireZero(g);
}

@Override
public boolean execute() {
	// TODO Auto-generated method stub
	return false;
}


}

