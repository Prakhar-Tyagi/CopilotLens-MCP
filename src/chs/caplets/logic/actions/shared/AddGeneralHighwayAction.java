package chs.caplets.logic.actions.shared;

import chs.caf.ActionContainer;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.CreateGeneralHighwayAction;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IGeneralHighway;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.common.IUIDObject;
import chs.services.dynamicgfx.ISmartPoint;
import chs.system.FactoryMgr;
import chs.utility.PortHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.util.List;

public class AddGeneralHighwayAction extends CreateGeneralHighwayAction
{

	protected IGeneralHighway highway;

	public AddGeneralHighwayAction(ICapletController controller) {
		super(controller);
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		highway = getOperand();
		if (highway == null) {
			return IActionEnum.eCanceled;
		}
		if (!LogicObjectLockFinder.tryEdit(highway)) {
			return IActionEnum.eCanceled;
		}
		if(highway.isShared() && AddSharedHelper.isSharedObjectPermissionDenied())
		{
			return IActionEnum.eCanceled;
		}
		return super.onActivate(e);
	}

	public boolean onTerminate(boolean successful)
	{
		boolean ok = super.onTerminate(successful);
		highway = null;
		return ok;
	}

	// TODO jacobt FEAT13040 : generify CreateConductorAction.constructDisplayObject
	protected IGfxObject constructDisplayObject(List<ISmartPoint> point_list)
	{
		// Set the connectivity to be this port - we know it already exists in this design to exist at all
		getCommand().setCableHighway(highway);

		IHighwaySchematic schemHighway = null;
		ILogicDesign design = highway.getLogicDesign();
		if (design != null) {
			// port graphics should only be added for multiple representations of a highway
			// count usages *before* this action was performed
			IDesignWideUsageMgr dwum = design.getDesignWideUsageMgr();
			int usageCount = dwum.getDesignSharedUsageCount(highway);

			schemHighway = (IHighwaySchematic) super.constructDisplayObject(point_list);
			if (schemHighway == null) {
				return null;
			}

			boolean home = true; // the first instance added is initially home
			if (usageCount > 0) {
				int gridSpacing = getLogicModel().getDiagram().getGrid().getGridSpacing();
				PortHelper.addPortGraphics(schemHighway, design, usageCount, gridSpacing,
						dwum.getRepresentations(highway));
				home = false; // all instances after the first are initially non-home
			}
			schemHighway.setHome(home); // similar rules for XRefs as shared
		}

		return schemHighway;
	}

	public boolean isEnabled()
	{
		if (!super.isEnabled()) {
			return false;
		}

		// if we are in a transaction boundary, we MUST wait
		if (FactoryMgr.getSystemFactory().getCAFUtils().isWithinTransactionBoundary()) {
			return false; // wonder why this isn't in the super?
		}
		IGeneralHighway highway = getOperand();
		// for when we different UI strings for different highway types:
//		((IUpdateableAction) getActionUI()).updateUI();

		return highway != null;
	}

	protected Model getLogicModel()
	{
		return (Model) getModel();
	}

	/**
	 * @return If only a single connectivity wire is selected, return it otherwise return null
	 */
	@Nullable
	private IGeneralHighway getOperand()
	{
		IGeneralHighway highway = null;
		SelectSet selections = getController().getSelectMgr().getPreSelections();
		for (SelectedUIDObjectIterator it = selections.getSelectedUIDObjects(); it.hasNext(); ) {
			IUIDObject obj = it.getNext();

			if (obj instanceof IGeneralHighway) {
				if (highway == null) {
					highway = (IGeneralHighway) obj;
				}
				else {
					highway = null; // multiple conductors selected ==> not enabled
					break;
				}
			}
		}
		return highway;
	}

	public String getActionUIClass()
	{
		return AddGeneralHighwayActionUI.class.getName();
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}
}
