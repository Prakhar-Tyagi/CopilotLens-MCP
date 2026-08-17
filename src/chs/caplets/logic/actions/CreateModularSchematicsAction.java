package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.IGfxModel;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.utilities.CommonUtils;
import chs.utility.helpers.ConnectorHelper;
import chs.utility.helpers.ModularSchemPinListInfo;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

public class CreateModularSchematicsAction extends ControllerActionRT implements ICtxMenuProvider
{

	public CreateModularSchematicsAction(
			@NotNull ICapletController controller)
	{
		super(controller);
	}

	@Override protected IActionEnum onActivate(ActionEvent e)
	{
		return IActionEnum.eCompleted;
	}

	@Override protected boolean onTerminate(boolean successful)
	{
		if (successful) {
			ISchemDiagram diagram = (ISchemDiagram) ((IGfxModel) getController().getCapletModel()).getSheet();
			SelectSet preSelections = getController().getSelectMgr().getPreSelections();
			for (IPinList selectedModularConnector : getSelectedModularConnectors(preSelections)) {
				ConnectorHelper.ensureModularSchematics(selectedModularConnector, diagram);
			}
			return true;
		}
		return false;
	}

	@NotNull private Set<IPinList> getSelectedModularConnectors(SelectSet inputSet)
	{
		Set<IPinList> modularCandidates = new HashSet<>();
		for (IPinList pinList : inputSet.getSelectedObjects(IPinList.class)) {
			IConnector connector = CommonUtils.cast(pinList.getConnectivity(), IConnector.class);
			if (connector != null) {
				int cableDepth = ConnectorHelper.getCableModularDepth(connector);
				int schemDepth = ConnectorHelper.getSchematicModularDepth(pinList);
				if (cableDepth > schemDepth) {
					modularCandidates.add(new ModularSchemPinListInfo(pinList).getAnchor());
				}
			}
		}
		return modularCandidates;
	}

	@Override public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		Action actionUI = getActionUI();
		if (actionUI != null && !getSelectedModularConnectors(selections).isEmpty()) {
			container.add(new ActionEntry(actionUI));
		}
	}

	@Override public void populateActiveCtxMenu(ActionContainer container)
	{

	}

	@Override public String getActionUIClass()
	{
		return CreateModularSchematicsActionUI.class.getName();
	}
}
