package chs.caplets.logic.actions.ui;

import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConductorIterator;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IMulticoreIterator;
import org.jetbrains.annotations.NotNull;

public class MCNodeCreator
{

	@NotNull private final IMulticore mMulticore;

	public MCNodeCreator(@NotNull IMulticore multicore)
	{

		mMulticore = multicore;
	}

	@NotNull public MCNode execute()
	{
		MCNode root = new MCNode("root", null);
		MCNode mcNode = createMCNode(mMulticore);
		root.add(mcNode);
		// Root is not actually the root multicore, just a dummy so return the child that is associated with the mc
		return mcNode;
	}

	@NotNull private MCNode createMCNode(@NotNull IMulticore mc)
	{
		MCNode mcNode = new MCNode(mc);

		// Get the Conductors
		IConductorIterator condIterator = mc.getConductorsIncludingShields();
		while (condIterator.hasNext()) {
			IConductor conductor = condIterator.getNext();
			MCNode node = new MCNode(conductor);
			mcNode.add(node);
		}
		// Check to see if it has Multicore
		IMulticoreIterator mcIterator = mc.getMulticores();
		while (mcIterator.hasNext()) {
			IMulticore mCore = mcIterator.getNext();
			mcNode.add(createMCNode(mCore));
		}
		return mcNode;
	}
}