package chs.caplets.logic.actions.shared;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IFunctionMessage;
import chs.cof.logical.cable.ILogicConnectivity;
import chs.cof.logical.shared.ISharedFunctionMessage;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.images.CHSImages;
import chs.services.dynamicgfx.ISmartPoint;
import chs.system.UIDMgr;
import chs.utility.helpers.SharedConductorHelper;
import chs.utility.helpers.SharedFunctionMessageHelper;
import chs.utility.helpers.SharedFunctionMessageState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AddSharedMessageAction extends AddSharedBaseFunctionConductorAction
{

	public AddSharedMessageAction(ICapletController controller, ISpecialSelectMgr libSelectMgr)
	{
		super(controller, libSelectMgr);
	}

	@Override protected Class<? extends IConductor> getConductorType()
	{
		return IFunctionMessage.class;
	}

	@Override protected IGfxObject constructDisplayObject(List<ISmartPoint> smartPoints)
	{
		ISharedFunctionMessage sharedFunctionMessage =
				UIDMgr.getObjectOfType(m_sharedConductorUID, ISharedFunctionMessage.class);
		IConnectivity connectivity = getLogicModel().getDesign().getConnectivity();
		if (connectivity == null) {
			return null;
		}
		IConductor cableCond = connectivity.findSharedConductor(sharedFunctionMessage);

		getCommand().setCableConductor(cableCond); // if null a new connectivity is created

		chs.cof.logical.schem.IConductor schemCond =
				(chs.cof.logical.schem.IConductor) super.constructDisplayObject(smartPoints);
		if (schemCond == null || sharedFunctionMessage == null) {
			return null;
		}
		SharedConductorHelper
				.assignToShared(schemCond, sharedFunctionMessage, getLogicModel().getDesign(),
						getLogicModel().getDiagram());

		IFunctionMessage functionMessage = (IFunctionMessage) schemCond.getConnectivity();
		functionMessage.setSharedConductor(sharedFunctionMessage);
		SharedFunctionMessageState updates = new SharedFunctionMessageState();
		SharedFunctionMessageHelper.fixUpFunctionMessageStructure(
				(ILogicConnectivity) connectivity, functionMessage, updates);
		return schemCond;
	}

	public String getActionUIClass()
	{
		return AddSharedMessageActionUI.class.getName();
	}

	@Nullable
	protected IUID getOperand()
	{
		if (m_specialSelectMgr.getSelectedObjects().getSize() == 1) {
			IUIDObject uidObj = m_specialSelectMgr.getSelectedObjects().getNext();
			if (uidObj instanceof ISharedFunctionMessage) {
				ISharedFunctionMessage shareConductor = (ISharedFunctionMessage) uidObj;
				return shareConductor.getUID();
			}
		}
		return null;
	}

	@NotNull protected String getCursorImage()
	{
		return CHSImages.FUNCTIONMESSAGE_ADD_CURSOR;
	}
}