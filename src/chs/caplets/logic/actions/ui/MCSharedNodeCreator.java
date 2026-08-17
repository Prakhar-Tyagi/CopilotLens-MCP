package chs.caplets.logic.actions.ui;

import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedMulticoreIterator;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class MCSharedNodeCreator
{
	@NotNull private final ISharedMulticore sharedMulticore;

	public MCSharedNodeCreator(@NotNull ISharedMulticore multicore)
	{
		sharedMulticore = multicore;
	}

	@NotNull public MCSharedNode execute()
	{
		MCSharedNode root = new MCSharedNode("root", null);
		MCSharedNode mcSharedNode = createMCSharedNode(sharedMulticore);
		root.add(mcSharedNode);
		// Root is not actually the root multicore, just a dummy so return the child that is associated with the mc
		return mcSharedNode;
	}

	@NotNull private MCSharedNode createMCSharedNode(@NotNull ISharedMulticore mc)
	{
		MCSharedNode mcSharedNode = new MCSharedNode(mc);
		// Get the Conductors
		Set<ISharedConductor> sharedConductors = mc.getConductorsIncludingShields();
		for (ISharedConductor conductor : sharedConductors) {
			MCSharedNode node = new MCSharedNode(conductor);
			mcSharedNode.add(node);
		}
		// Check to see if it has Multicore
		ISharedMulticoreIterator mcIterator = mc.getMulticores();
		while (mcIterator.hasNext()) {
			ISharedMulticore mCore = mcIterator.getNext();
			mcSharedNode.add(createMCSharedNode(mCore));
		}
		return mcSharedNode;
	}
}
