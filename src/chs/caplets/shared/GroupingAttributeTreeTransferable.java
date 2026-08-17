package chs.caplets.shared;

import chs.caf.action.dragdrop.BasicTreeDragDropActionInvocationControl;
import chs.caf.action.dragdrop.IDragDropActionInvocationControl;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.datatransfer.DataFlavor;
import java.util.ArrayList;
import java.util.List;

public class GroupingAttributeTreeTransferable extends BasicTreeDragDropActionInvocationControl
{

	private List<DefaultMutableTreeNode> selectedObjects;

	public GroupingAttributeTreeTransferable(List<DefaultMutableTreeNode> selectedObjects,
			@NotNull IDragDropActionInvocationControl dNdActionInvocationControl)
	{
		super(dNdActionInvocationControl);
		this.selectedObjects = new ArrayList<>(selectedObjects);
	}

	@NotNull @Override protected Object getAlternateTransferData(DataFlavor flavor)
	{
		return selectedObjects;
	}
}
