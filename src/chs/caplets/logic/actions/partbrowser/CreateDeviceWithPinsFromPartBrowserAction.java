package chs.caplets.logic.actions.partbrowser;

import chs.caf.CAFUtils;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.action.IAction;
import chs.caplets.logic.actions.AddDeviceFromLibraryWithPinsAction;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.Nullable;

@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign, Application.CapitalCapture,
				Application.CapitalArchitect, Application.SEElectricalDesign}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
class CreateDeviceWithPinsFromPartBrowserAction extends AbstractCreateDeviceFromPartBrowserAction {

	CreateDeviceWithPinsFromPartBrowserAction() {
		super(ResourceMgr.getString(CreateDeviceWithPinsFromPartBrowserAction.class,
				"CreateDeviceWithPinsFromPartBrowserAction.name.decl"),
				ResourceMgr.getString(CreateDeviceWithPinsFromPartBrowserAction.class,
						"CreateDeviceWithPinsFromPartBrowserAction.shortDesc.decl"),
				ResourceMgr.getString(CreateDeviceWithPinsFromPartBrowserAction.class,
						"CreateDeviceWithPinsFromPartBrowserAction.longDesc.decl"),
				(int) ResourceMgr.getMnemonic(CreateDeviceWithPinsFromPartBrowserAction.class,
						"CreateDeviceWithPinsFromPartBrowserAction.mnemonic"),
				CHSImageLoader.loadImageIcon(CHSImages.DEVICE_ICON_ENABLED));

	}

	@Nullable
	@Override
	public IAction getActionToPerform() {
		return CAFUtils.getInstance().getActiveCapletController().getAction(AddDeviceFromLibraryWithPinsAction.class);
	}
}