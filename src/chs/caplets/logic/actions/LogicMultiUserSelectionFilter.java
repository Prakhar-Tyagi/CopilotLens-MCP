package chs.caplets.logic.actions;

import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectionIterator;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.schem.ISchemDiagram;
import chs.utilities.CommonUtils;
import chs.utility.DiagramHelper;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

public class LogicMultiUserSelectionFilter
{

	private LogicMultiUserSelectionFilter()
	{
	}

	public static Set<IDiagramObject> getValidDiagramObjectOperands(@Nullable SelectSet selections)
	{
		Set<IDiagramObject> validDiagramObjectsToProcess = new LinkedHashSet<>();

		if (selections != null) {
			SelectionIterator iter = selections.getSelected();
			while (iter.hasNext()) {
				IDiagramObject selectdObj = CommonUtils.cast(iter.getNext().getObject(), IDiagramObject.class);

				if (selectdObj != null) {
					ISchemDiagram diagram = DiagramHelper.getDiagram(selectdObj);
					if (diagram != null && diagram.isEditable()) {
						validDiagramObjectsToProcess.add(selectdObj);
					}
				}
			}
		}
		return validDiagramObjectsToProcess;
	}
}