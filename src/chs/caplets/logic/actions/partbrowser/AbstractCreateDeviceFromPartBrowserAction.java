package chs.caplets.logic.actions.partbrowser;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.cafmain.actions.partbrowser.PartBrowserAction;
import chs.cof.parts.ILibraryDevice;
import chs.cof.parts.ILibraryObject;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign, Application.CapitalCapture,
				Application.CapitalArchitect, Application.SEElectricalDesign}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
public abstract class AbstractCreateDeviceFromPartBrowserAction extends PartBrowserAction
{

	protected AbstractCreateDeviceFromPartBrowserAction()
	{
		super(ResourceMgr.getString(CreateDeviceFromPartBrowserAction.class,
				"CreateDeviceFromPartBrowserAction.name.decl"),
				ResourceMgr.getString(CreateDeviceFromPartBrowserAction.class,
						"CreateDeviceFromPartBrowserAction.shortDesc.decl"),
				ResourceMgr.getString(CreateDeviceFromPartBrowserAction.class,
						"CreateDeviceFromPartBrowserAction.longDesc.decl"),
				(int) ResourceMgr.getMnemonic(CreateDeviceFromPartBrowserAction.class,
						"CreateDeviceFromPartBrowserAction.mnemonic"),
				CHSImageLoader.loadImageIcon(CHSImages.DEVICE_ICON_ENABLED));
	}

	protected AbstractCreateDeviceFromPartBrowserAction(String name, String shortDesc, String longDesc, int mnemonic, Icon icon)
	{
		super(name, shortDesc, longDesc, mnemonic, icon);
	}

	public boolean isApplicable(ILibraryObject libObj)
	{
		return (libObj instanceof ILibraryDevice);
	}
}
