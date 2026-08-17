/*
 * Copyright 2004-2016 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICAFSymbolLibraryMgr;
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletLifecycle;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ViewActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.Selection;
import chs.caplets.symbol.Model;
import chs.cof.symbol.IAbstractLibrary;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.ISymboledObject;
import chs.common.IUIDObject;
import chs.common.validation.ValidationHelper;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utility.SymbolUtils;
import chs.utility.helpers.CreationDeletionHelper;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import javax.swing.JFrame;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Feb 12, 2004 Time: 10:08:36 AM To change this template use Options |
 * File Templates.
 */

public class ViewRelatedSymbolAction extends ViewActionRT implements ICtxMenuProvider
{

	public ViewRelatedSymbolAction(ICapletView view)
	{
		super(view);
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		return IActionEnum.eCompleted;
	}

	protected boolean onTerminate(boolean successful)
	{
		if (successful) {
			JFrame mainFrame = CAFUtils.getInstance().getWindowMgr().getMainFrame();
			Cursor oldCursor = mainFrame.getCursor();
			try {
				mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				for (ISymboledObject operand : getOperands(getCurrentSelections())) {
					openSymbolInView(operand);
				}
			}
			finally {
				mainFrame.setCursor(oldCursor);
			}
		}
		return successful;
	}

	private void openSymbolInView(@NotNull ISymboledObject operand)
	{
		ISymbolDef symbolDef = SymbolUtils.getSymbolDef(operand.getSymbolRef());
		if (symbolDef == null) {
			displayPermissionRestrictedDialog();
			return;
		}
		IAbstractLibrary containerLibrary = symbolDef.getContainerLibrary();
		if (containerLibrary == null) {
			return;
		}
		ICapletLifecycle lifecycle = getController().getCaplet().getLifecycle();
		ICAFSymbolLibraryMgr sm = CAFUtils.getInstance().getSymbolLibraryMgr();
		if (!sm.isLibraryOpen(containerLibrary)) {
			sm.openLibrary(containerLibrary);
			ValidationHelper.validateAfterLoad(containerLibrary);
		}
		sm.setCurrentLibrary(containerLibrary);
		if (lifecycle.openExisting(Arrays.asList(containerLibrary, symbolDef))) {
			// Note that we don't pass any connectivity object in since we don't actually add
			// anything to connectivity from here. (The importer takes care of it). Just need to
			// make sure things are set up ok for undo.
			CreationDeletionHelper.getTheCreationHelper().processObjects();
		}
	}

	protected void displayPermissionRestrictedDialog()
	{
		ResourceBasedMessageContent content =
				new ResourceBasedMessageContent(ViewRelatedSymbolAction.class, "ViewRelatedSymbolAction.dialog");
		showMessage(PromptSeverity.WARNING, content);
	}

	protected void showMessage(PromptSeverity warning, ResourceBasedMessageContent messageContent)
	{
		Message.show(warning, messageContent);
	}

	public boolean isEnabled()
	{
		return isValidSelection(getCurrentSelections()) && super.isEnabled();
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	public void populateCtxMenu(@NotNull ActionContainer container, @NotNull SelectSet selections)
	{
		Action actionUI = getActionUI();
		if (actionUI != null && isValidSelection(selections)) {
			container.add(new ActionEntry(actionUI));
		}
	}

	private boolean isValidSelection(@NotNull SelectSet selections)
	{
		return !getOperands(selections).isEmpty();
	}

	@NotNull private List<ISymboledObject> getOperands(@NotNull SelectSet selections)
	{
		Model model = CommonUtils.cast(getController().getCapletModel(), Model.class);
		if (model != null) {
			List<ISymboledObject> params = new ArrayList<>(selections.getSelectCount());
			for (Selection selection : selections.getSelected()) {
				IUIDObject nonDeletedObject = selection.getNonDeletedObject();
				if (nonDeletedObject instanceof ISymboledObject) {
					params.add((ISymboledObject) nonDeletedObject);
				}
			}
			if (params.size() == 1) {
				return Collections.unmodifiableList(params);
			}
		}
		return Collections.emptyList();
	}

	public String getActionUIClass()
	{
		return ViewRelatedSymbolActionUI.class.getName();
	}

	public String getStatusbarText()
	{
		return ResourceMgr.getString(ViewRelatedSymbolAction.class, "ViewRelatedSymbolAction.StatusBar.text");
	}
}
