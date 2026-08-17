package chs.caplets.logic.actions.icdbrowser;

import chs.caf.CAFUtils;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.browser.ICDBrowserActionHelper;
import chs.caplets.logic.actions.partbrowser.AbstractCreateDeviceFromPartBrowserAction;
import chs.cof.browser.IBasePartsBrowser;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.Nullable;

public class CreateDeviceFromICDBrowserAction extends AbstractCreateDeviceFromPartBrowserAction
{

	public CreateDeviceFromICDBrowserAction()
	{
		super(ResourceMgr.getString(AddDeviceFromICDActionUI.class,
						"AddDeviceFromICDActionUI.name.decl"),
						ResourceMgr.getString(AddDeviceFromICDActionUI.class,
								"AddDeviceFromICDActionUI.shortDesc.decl"),
						ResourceMgr.getString(AddDeviceFromICDActionUI.class,
								"AddDeviceFromICDActionUI.longDesc.decl"),
						(int) ResourceMgr.getMnemonic(AddDeviceFromICDActionUI.class,
								"AddDeviceFromICDActionUI.mnemonic"),
						CHSImageLoader.loadImageIcon(CHSImages.DEVICE_ICON_ENABLED));
	}

	@Nullable @Override public IAction getActionToPerform()
	{
		return CAFUtils.getInstance().getActiveCapletController().getAction(AddDeviceFromICDAction.class);
	}

	@Override @Nullable protected IBasePartsBrowser getPartBrowser()
	{
		return ICDBrowserActionHelper.getICDBrowser();
	}

}
