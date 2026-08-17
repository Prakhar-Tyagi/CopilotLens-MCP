package chs.caplets.logic.actions.icdbrowser;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.KeyStroke;
import java.awt.Event;
import java.awt.event.KeyEvent;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner})
public class AddDeviceFromICDActionUI extends ActionUI
{

	public AddDeviceFromICDActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{

		putValue(MNEMONIC_KEY, (int) 'D');
		putValue(NAME,
				ResourceMgr.getStringForMenu(AddDeviceFromICDActionUI.class, "AddDeviceFromICDActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(AddDeviceFromICDActionUI.class, "AddDeviceFromICDActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(AddDeviceFromICDActionUI.class, "AddDeviceFromICDActionUI.longDesc.decl"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_device_active.gif"));
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_I, Event.CTRL_MASK));
	}

	public String getActionClass()
	{
		return AddDeviceFromICDAction.class.getName();
	}
}
