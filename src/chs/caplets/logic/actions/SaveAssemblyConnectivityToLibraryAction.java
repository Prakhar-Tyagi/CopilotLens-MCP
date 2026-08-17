/*
 * Copyright 2006-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.CAFUtils;
import chs.caf.cafmain.actions.bridges.BridgeCAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.helpers.CommandClientCtxMenuAction;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.commands.SaveAssemblyConnectivityToLibraryCommand;
import chs.cof.parts.ILibraryAssembly;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.Library;
import chs.cof.parts.LibraryCriteriaHelper;
import chs.cof.parts.configure.ConfigurationTypeEnum;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cof.parts.partselector.ILibraryPartSelector;
import chs.cof.parts.partselector.PartSelectionContext;
import chs.cof.project.IProject;
import chs.cof.security.FunctionalPermissionEnum;
import chs.common.IDesignContainer;
import chs.common.criteria.ICriteria;
import chs.system.FactoryMgr;
import chs.utilities.ui.messaging.Choice;
import chs.utilities.ui.messaging.IMessagingChoices;
import chs.utilities.ui.messaging.Question;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Action to update a library part for a single object on a logic diagram.
 */
public class SaveAssemblyConnectivityToLibraryAction
		extends CommandClientCtxMenuAction<SaveAssemblyConnectivityToLibraryCommand>
{

	private IProject m_project;
	private IDesignContainer m_design;

	public SaveAssemblyConnectivityToLibraryAction(ICapletController controller)
	{
		super(controller);
	}

	@Override public boolean isEnabled()
	{
		if (!super.isEnabled()) {
			return false;
		}

		if (!getController().getCapletModel().isEditable()) {
			return false;
		}

		return hasEditLibraryAssemblyPermission();
	}

	private boolean hasEditLibraryAssemblyPermission()
	{
		return FactoryMgr.getCHSSystem().getFunctionalPermissionMgr().hasPermission(
                FunctionalPermissionEnum.LibraryComponentAssembly);
	}

	@Override public String getActionUIClass()
	{
		return SaveAssemblyConnectivityToLibraryActionUI.class.getName();
	}

	@Override protected boolean doOnActivateCmdInitialize()
	{
		m_design = BridgeCAFUtils.getDesign();
		if (m_design != null) {
			m_project = CAFUtils.getInstance().getCurrentProject();
			final SaveAssemblyConnectivityToLibraryCommand cmd =
					new SaveAssemblyConnectivityToLibraryCommand(m_design);
			setCommand(cmd);
			final SaveAssemblyConnectivityToLibraryCommandListener<SaveAssemblyConnectivityToLibraryAction> listener =
					new SaveAssemblyConnectivityToLibraryCommandListener<SaveAssemblyConnectivityToLibraryAction>(
							SaveAssemblyConnectivityToLibraryAction.class);
			cmd.setCommandListener(listener);
			setProgressDialogParams(true, false);
			return true;
		}
		return false;
	}

	// Library Parts needs to be saved first and then the connectivity data will be populated.
	// Hence we do not want any outer level transaction to be created
	@Override protected boolean handleTransactionsInAction()
	{
		return false;
	}

	protected boolean onTerminate(boolean successful)
	{
		if (!successful) {
			return true; // dialog cancelled, nothing else to do here
		}
		Library.getInstance().refresh();
		ILibraryPartSelection partSelection = getLibraryPartSelection();
		if (partSelection != null) {
			final ILibraryObject selectedObject = partSelection.getSelectedObject();
			if (selectedObject instanceof ILibraryAssembly) {
				if (shouldProceedWithAction()) {
					getCommand().setSelectedLibraryPart((ILibraryAssembly) selectedObject);
					return terminate();
				}
			}
		}
		return false;
	}

	protected boolean terminate()
	{
		return super.onTerminate(true);
	}

	@Nullable protected ILibraryPartSelection getLibraryPartSelection()
	{
		ILibraryPartSelector selector =
				Library.getInstance().getLibraryPartSelector(CAFUtils.getInstance().getWindowMgr().getDialogFrame());

		ICriteria<ILibraryAssembly> criteria = LibraryCriteriaHelper.createCriteria(ILibraryAssembly.class);

		return getiLibraryPartSelection(selector, criteria);
	}

	@Nullable protected ILibraryPartSelection getiLibraryPartSelection(@NotNull ILibraryPartSelector selector,
			ICriteria<ILibraryAssembly> criteria)
	{
        PartSelectionContext partSelectionContext = getPartSelectionContext();
        return getiLibraryPartSelection(selector, criteria, partSelectionContext);
	}

    @Nullable protected ILibraryPartSelection getiLibraryPartSelection(@NotNull ILibraryPartSelector selector,
            ICriteria<ILibraryAssembly> criteria, PartSelectionContext partSelectionContext)
    {
        return selector.selectPart(criteria, m_project, partSelectionContext, ConfigurationTypeEnum.LOGICAL, m_design);
    }

	protected PartSelectionContext getPartSelectionContext() {
		return new PartSelectionContext() {
			@Override
			public boolean shouldIgnorePartStatusCheck() {
				return true; //Requirement is to ignore the 'Part status check' project preference during save as COTs
			}
		};
	}

	protected boolean shouldProceedWithAction()
	{
		ResourceBasedMessageContent content = new ResourceBasedMessageContent(this,
				"SaveAssemblyConnectivityToLibraryAction.saveConnectivity");

		Choice proceedChoice = new Choice(content.getResourceReader(), "continueButton");
		Choice cancelChoice =
				new Choice(IMessagingChoices.class, "messaging.choices.cancel", Choice.DefaultSetting.DEFAULT);
		Choice response = Question.show(content, proceedChoice, cancelChoice);
		return response == proceedChoice;
	}

	@Override public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		// do not populate context menu
	}
}
