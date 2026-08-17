package chs.caplets.logic.actions.shared;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.caplets.logic.Model;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IFunctionConductor;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedFunctionConductor;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.images.CHSImages;
import chs.system.UIDMgr;
import chs.utility.logic.ILogicModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by nagamani on 23-02-2015.
 */
public class AddSharedSignalAction extends AddSharedBaseFunctionConductorAction
{

	public AddSharedSignalAction(ICapletController controller, ISpecialSelectMgr libSelectMgr)
	{
		super(controller, libSelectMgr);
	}

	protected Model getLogicModel()
	{
		return (Model) getModel();
	}

	@Nullable protected ISharedConductor getSharedBaseFunctionConductor()
	{
		return UIDMgr.getObjectOfType(m_sharedConductorUID, ISharedConductor.class);
	}

	@Override protected Class<? extends IConductor> getConductorType()
	{
		return IFunctionConductor.class;
	}

	public String getActionUIClass()
	{
		return AddSharedSignalActionUI.class.getName();
	}

	@Nullable
	protected IUID getOperand()
	{
		if (m_specialSelectMgr.getSelectedObjects().getSize() == 1) {
			IUIDObject uidObj = m_specialSelectMgr.getSelectedObjects().getNext();
			if (uidObj instanceof ISharedConductor) {
				ISharedConductor shareConductor = (ISharedConductor) uidObj;
				final ILogicDesign design = ((ILogicModel) getModel()).getDesign();
				if (shareConductor instanceof ISharedFunctionConductor) {
					return shareConductor.getUID();
				}
			}
		}
		return null;
	}

	@NotNull protected String getCursorImage()
	{
		return CHSImages.FUNCTIONCODUCTOR_ADD_CURSOR;
	}
}