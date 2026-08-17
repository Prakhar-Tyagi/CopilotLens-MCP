package chs.caplets.logic.actions.shared;

import chs.caf.ActionEntry;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.caf.caplet.action.IActionEnum;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.CreateFunctionBaseConductorAction;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.shared.ISharedConductor;
import chs.common.IUID;
import chs.services.dynamicgfx.ISmartPoint;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.List;

public abstract class AddSharedBaseFunctionConductorAction extends CreateFunctionBaseConductorAction
{

	protected ISpecialSelectMgr m_specialSelectMgr;
	@Nullable protected IUID m_sharedConductorUID;

	protected AddSharedBaseFunctionConductorAction(ICapletController controller, ISpecialSelectMgr libSelectMgr)
	{
		super(controller);

		m_specialSelectMgr = libSelectMgr;
		if (getActionUI() != null) {
			m_specialSelectMgr.contextMenuAddAction(
					new ActionEntry(getActionUI(), (String) getActionUI().getValue(Action.SHORT_DESCRIPTION))
					{
						public boolean shouldDisplay()
						{
							return getOperand() != null && super.shouldDisplay();
						}
					});
		}
	}

	protected Model getLogicModel()
	{
		return (Model) getModel();
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		m_sharedConductorUID = getOperand();
		if (m_sharedConductorUID == null) {
			return IActionEnum.eCanceled;
		}

		final ISharedConductor sharedConductor = getSharedBaseFunctionConductor();
		if (sharedConductor == null) {
			return IActionEnum.eCanceled;
		}

		final ILogicDesign logicDesign = getLogicModel().getDesign();

		if (mCreateCondInstanceHelper.isSharedConductorUnusable(sharedConductor, logicDesign, this::refresh)) {
			return IActionEnum.eCanceled;
		}

		return super.onActivate(e);
	}

	@Nullable protected ISharedConductor getSharedBaseFunctionConductor()
	{
		return UIDMgr.getObjectOfType(m_sharedConductorUID, ISharedConductor.class);
	}

	public boolean onTerminate(boolean successful)
	{
		boolean ok = successful;
		ISharedConductor sharedCond = getSharedBaseFunctionConductor();
		try {
			if (ok && !refresh(sharedCond, getLogicModel().getDesign().getProject())) {
				ok = false;
			}

//			if (ok && !m_sharedWire.lockForExclusiveRead()) {
//				LogicActionMessageHelper.warnLocked(m_sharedWire);
//				ok = false;
//			}

			ok = super.onTerminate(ok);
		}
		finally {
			assert sharedCond != null;
			sharedCond.unlock();
		}

		m_sharedConductorUID = null;
		return ok;
	}

	@Override protected IGfxObject constructDisplayObject(List<ISmartPoint> smartPoints)
	{
		// If this isn't the first time this wire has been placed in this design, use the existing connectivity.
		// design&connectivity won't be null here
		//noinspection ConstantConditions
		ISharedConductor sharedConductor = getSharedBaseFunctionConductor();
		ConductorDisplayObjectConstructionHelper displayObjectConstructionHelper = new ConductorDisplayObjectConstructionHelper(getLogicModel(), getCommand(), this::constructSchemConductor);
		return displayObjectConstructionHelper.constructDisplayObject(smartPoints, sharedConductor);
	}

	private chs.cof.logical.schem.IConductor constructSchemConductor(List<ISmartPoint> smartPoints){
		chs.cof.logical.schem.IConductor schemCond =
				(chs.cof.logical.schem.IConductor) super.constructDisplayObject(smartPoints);
		return schemCond;
	}

	public boolean isEnabled()
	{
		//
		// If we are in a transaction boundary, we MUST wait
		//
		if (FactoryMgr.getSystemFactory().getCAFUtils().isWithinTransactionBoundary()) {
			return false;
		}
		return super.isEnabled() && getOperand() != null;
	}

	@Nullable
	protected abstract IUID getOperand();
}