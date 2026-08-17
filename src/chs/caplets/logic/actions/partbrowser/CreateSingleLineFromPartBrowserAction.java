/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.partbrowser;

import chs.caf.CAFUtils;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.cafmain.actions.partbrowser.PartBrowserAction;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.browser.PartBrowserActionHelper;
import chs.caplets.logic.actions.CreateSingleLineAction;
import chs.cof.parts.ILibraryMulticore;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.Nullable;

/*
 * This class supports CreateSingleLine Right Mouse Click Action
 * for SingleLine supported Library parts in Parts Tab
 *
 * @created July 05, 2024
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner})
public class CreateSingleLineFromPartBrowserAction extends PartBrowserAction
{

	public CreateSingleLineFromPartBrowserAction()
	{
		super(ResourceMgr.getString(CreateSingleLineFromPartBrowserAction.class,
						"CreateSingleLineFromPartBrowserAction.name.decl"),
				ResourceMgr.getString(CreateSingleLineFromPartBrowserAction.class,
						"CreateSingleLineFromPartBrowserAction.shortDesc.decl"),
				ResourceMgr.getString(CreateSingleLineFromPartBrowserAction.class,
						"CreateSingleLineFromPartBrowserAction.longDesc.decl"),
				(int) ResourceMgr.getMnemonic(CreateSingleLineFromPartBrowserAction.class,
						"CreateSingleLineFromPartBrowserAction.mnemonic"),
				CHSImageLoader.loadImageIcon(CHSImages.SINGLE_LINE_ICON));
	}

	@Nullable @Override public IAction getActionToPerform()
	{
		return CAFUtils.getInstance().getActiveCapletController().getAction(CreateSingleLineAction.class);
	}

	@Override public boolean isApplicable(ILibraryObject libObj)
	{
		return (libObj instanceof ILibraryMulticore);
	}

	public boolean isEnabled()
	{
		if (super.isEnabled()) {
			ILibraryMulticore libraryConnector = getSelectedPart();
			return libraryConnector != null;
		}
		return false;
	}

	@Nullable private ILibraryMulticore getSelectedPart()
	{
		ILibraryPartSelection libPart = getPartSelection();
		if (libPart != null && libPart.getSelectedObject() instanceof ILibraryMulticore libraryMulticore) {
			return libraryMulticore;
		}
		return null;
	}

	@Nullable protected ILibraryPartSelection getPartSelection()
	{
		return PartBrowserActionHelper.getCurrentSelectedBrowserPart();
	}
}