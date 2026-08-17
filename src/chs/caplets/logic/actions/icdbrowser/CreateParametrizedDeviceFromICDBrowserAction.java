/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */
package chs.caplets.logic.actions.icdbrowser;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.browser.ICDBrowserActionHelper;
import chs.caplets.logic.actions.partbrowser.AbstractCreateDeviceFromPartBrowserAction;
import chs.cof.browser.IBasePartsBrowser;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.Nullable;

public class CreateParametrizedDeviceFromICDBrowserAction extends AbstractCreateDeviceFromPartBrowserAction
{

	public CreateParametrizedDeviceFromICDBrowserAction()
	{
		super(ResourceMgr.getStringForMenu(AddParametrizedDeviceFromICDActionUI.class,
						"AddParametrizedDeviceFromICDActionUI.name.decl"),
				ResourceMgr.getStringForMenu(AddParametrizedDeviceFromICDActionUI.class,
						"AddParametrizedDeviceFromICDActionUI.shortDesc.decl"),
				ResourceMgr.getString(AddParametrizedDeviceFromICDActionUI.class,
						"AddParametrizedDeviceFromICDActionUI.longDesc.decl"),
				(int) ResourceMgr.getMnemonic(AddParametrizedDeviceFromICDActionUI.class,
						"AddParametrizedDeviceFromICDActionUI.mnemonic"),
				CHSImageLoader.loadImageIcon(CHSImages.DEVICE_ICON_ENABLED));
	}

	@Nullable @Override public IAction getActionToPerform()
	{
		return getActiveCapletController().getAction(AddParametrizedDeviceFromICDAction.class);
	}

	@Override @Nullable protected IBasePartsBrowser getPartBrowser()
	{
		return ICDBrowserActionHelper.getICDBrowser();
	}

	protected ICapletController getActiveCapletController()
	{
		return CAFUtils.getInstance().getActiveCapletController();
	}

}