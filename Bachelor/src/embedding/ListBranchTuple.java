package embedding;

import org.apache.bcel.generic.*;

class ListBranchTuple {
	
	public final InstructionList il;
	public final BranchHandle bh;

	public ListBranchTuple(InstructionList instructionList, BranchHandle branchHandle) {
		this.il = instructionList;
		this.bh = branchHandle;
	}

}
