/*
 * Copyright 2002-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic;

import chs.caf.caplet.ICaplet;
import chs.caf.caplet.IEditClient;
import chs.caf.caplet.ILogicController;
import chs.caf.caplet.action.IDeferredAction;
import chs.caf.caplet.action.IDeferredActionProcessor;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.caplet.helpers.RegenerateGraphicsAction;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.actions.ConvertInlineToPlugJackPairAction;
import chs.caplets.logic.actions.CreateModularSchematicsAction;
import chs.caplets.logic.actions.DisconnectAction;
import chs.caplets.logic.actions.JoinPinlistsAction;
import chs.caplets.logic.actions.ManageConnectorsAction;
import chs.caplets.logic.actions.concurrency.LockLogicObjectsAction;
import chs.caplets.shared.BaseController;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cofUtils.logical.concurrency.LogicConcurrencyHelper;
import chs.ctf.editui.LogicEditSelectionHelper;
import chs.system.FactoryMgr;
import chs.utilities.BuildInfo;
import org.jetbrains.annotations.Nullable;

public class LogicController extends BaseController implements ILogicController
{

	public LogicController(ICaplet caplet, ILogicDesign design, ISchemDiagram diagram)
	{
		super(caplet, design, diagram); // true means is Logic
		createLogicControllerActions();
		FactoryMgr.getSystemFactory().getCHSSystem().getCHSUtils().setObjectBrowser(new LogicObjectGraphBrowser());
		IDeferredActionProcessor deferredActionProcessor = getDeferredActionProcessor();
		deferredActionProcessor.addDeferredAction(RegenerateGraphicsAction.getInstance());
		deferredActionProcessor.addDeferredAction((IDeferredAction) ConductorRouteAction.getInstance());
//		deferredActionProcessor.addDeferredAction(new LogicTableDataChangeAction(design, getCapletModel()));
	}

	protected void createLogicControllerActions()
	{
		super.createLogicControllerActions();
		addAction(new JoinPinlistsAction(this));
		addAction(new ManageConnectorsAction(this));
		addAction(new DisconnectAction(this));
		addAction(new ConvertInlineToPlugJackPairAction(this));
		addAction(new CreateModularSchematicsAction(this));
		if (LogicConcurrencyHelper.isLogicInMultiUserMode(getProject()) &&
				BuildInfo.getBuildInfo().areQAExtensionsEnabled()) {
			// Waiting for Marketing nod to add this in production code
			final LockLogicObjectsAction action = new LockLogicObjectsAction(this);
			getCaplet().addActionUI(action);
			addAction(action);
		}
	}

	public String getDoubleClickAction()
	{
		LogicEditSelectionHelper hesHelper =
				new LogicEditSelectionHelper(getSelectMgr().getPreSelections());
		return hesHelper.getDoubleClickAction();
	}

	@Nullable public IEditClient getEditClient(SelectSet selections, @Nullable Object owner)
	{
		LogicEditSelectionHelper hesHelper = new LogicEditSelectionHelper(selections);
		return hesHelper.getEditClient(this);
	}
}
