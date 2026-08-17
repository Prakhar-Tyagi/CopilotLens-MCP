package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.selection.SelectSet;
import chs.cof.logical.schem.IPinList;
import chs.ctf.caf.ui.IPlacementOptionParams;
import chs.ctf.caf.ui.PinPlaceOptionsParams;

import java.awt.event.ActionEvent;

public class AddPinWNAccelAction extends AddPinAction
{

	public AddPinWNAccelAction(ICapletController controller)
	{
		super(controller);
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		if (!initAddPinModel()) {
			return IActionEnum.eCanceled;
		}
		IPinList pinList = m_addPinActionModel.getReference();
		assert pinList != null;
		IPlacementOptionParams params = new PinPlaceOptionsParams(pinList.getConnectivity());
		boolean retState = m_addPinActionPresenter.initializePresenterForWNAccel(m_addPinActionModel, params);
		return retState ? IActionEnum.eActivated : IActionEnum.eCanceled;
	}

	/**
	 * Return our matching ActionUI class
	 */
	public String getActionUIClass()
	{
		return AddPinWNAccelActionUI.class.getName();
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
	}

//	@Override public void populateActiveCtxMenu(ActionContainer container)
//	{
//	}
}
