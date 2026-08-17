package chs.caplets.logic.actions.serviceDocumentation.smartflows;

import chs.caf.ActionContainer;
import chs.caf.cafmain.actions.ApplyStyleOnDiagramObjectAction;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectionFilter;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.NetConductor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ChangeFlowDirectionAction extends ApplyStyleOnDiagramObjectAction
{

	public ChangeFlowDirectionAction(
			@NotNull ICapletController controller)
	{
		super(controller);
	}

	@Override protected boolean onTerminate(boolean successful)

	{
		if (successful) {
			List<NetConductor> selectedNets = getAllSelectedNets();
			changeDirectionForSelectedFlows(selectedNets);
			applyStyleOnSelection();
		}
		return true;
	}

	public void changeDirectionForSelectedFlows(List<NetConductor> flows)
	{
		flows.stream().forEach(flow -> changeDirectionFor(flow));
	}

	public void changeDirectionFor(NetConductor net)
	{
		IProcessFlow processFlow = createFlowObject(net);
		if (processFlow != null) {
			processFlow.toggleDirection();
		}
	}

	@Nullable public IProcessFlow createFlowObject(NetConductor net)
	{
		return new ProcessFlow(net);
	}

	@Override public String getActionUIClass()
	{
		return ChangeFlowDirectionActionUI.class.getName();
	}

	@Override public boolean isEnabled()
	{
		List<NetConductor> allnets = getAllSelectedNets();
		return !allnets.isEmpty();
	}

	@NotNull protected List<NetConductor> getAllSelectedNets()
	{
		SelectSet selectedObjects = getSelectedObjects();
		return getNetsFromSelection(selectedObjects);
	}

	@NotNull private List<NetConductor> getNetsFromSelection(SelectSet nets)
	{
		List<NetConductor> allnets = new ArrayList<>();
		if (nets.getSelectCount() == 1) {
			chs.cof.logical.schem.IConductor uidObj =
					(chs.cof.logical.schem.IConductor) nets.getSelectedUIDObjects().getNext();
			collectNets(allnets, uidObj);
		}
		return allnets;
	}

	protected void collectNets(List<NetConductor> allnets, chs.cof.logical.schem.IConductor uidObj)
	{
		if (uidObj != null) {
			IConductor conductor = uidObj.getConnectivity();
			if (conductor instanceof NetConductor) {
				allnets.add((NetConductor) conductor);
			}
		}
	}

	@NotNull private SelectSet getSelectedObjects()
	{
		SelectSet selections = getSelectedSet();
		SelectSet nets = new SelectSet();
		SelectionFilter filter = new SelectionFilter(chs.cof.logical.schem.IConductor.class);
		nets.setSelectionFilter(filter);
		nets.setSelections(selections);
		return nets;
	}

	protected SelectSet getSelectedSet()
	{
		return getController().getSelectMgr().getPreSelections();
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
	}
}
