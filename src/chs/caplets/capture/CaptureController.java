/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */
package chs.caplets.capture;

import chs.caf.caplet.ICaplet;
import chs.caf.caplet.action.IDeferredAction;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.caplet.helpers.IPropertiesClient;
import chs.caf.caplet.helpers.RegenerateGraphicsAction;
import chs.caplets.capture.actions.ddt.AssignDDTTypesAction;
import chs.caplets.capture.actions.ddt.EditDDTTypesAction;
import chs.caplets.logic.actions.DisconnectAction;
import chs.caplets.shared.BaseController;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.ctf.editui.LogicEditSelectionHelper;
import org.jetbrains.annotations.NotNull;

/**
 * Provides a Controller specific to {@link CaptureCaplet}
 */
public class CaptureController extends BaseController
{

	public CaptureController(ICaplet caplet, ILogicDesign design, ISchemDiagram diagram)
	{
		super(caplet, design, diagram); // false means is not Logic
		//
		// Need Logic action too (UI will sort out).
		//
		createLogicControllerActions();
		createCaptureControllerActions();

		getDeferredActionProcessor().addDeferredAction(RegenerateGraphicsAction.getInstance());
		getDeferredActionProcessor().addDeferredAction((IDeferredAction) ConductorRouteAction.getInstance());
	}

	protected void createCaptureControllerActions()
	{
		addAction(new EditDDTTypesAction(this));
		addAction(new AssignDDTTypesAction(this));
	}

	protected void createLogicControllerActions()
	{
		super.createLogicControllerActions();
		addAction(new DisconnectAction(this));
	}

	@Override @NotNull public IPropertiesClient createPropertiesClient()
	{
		return new CapturePropertiesClient(getCapletModel());
	}

	/**
	 * Creates and returns a "Capture" properties client object specifically tailored for the Quick Access Panel.
	 *
	 * @param willLockSharedObject Flag indicating whether to lock shared objects: {@code true} to lock,
	 * {@code false} to leave unlocked
	 * @return IPropertiesClient A properties client object tailored for the Quick Access Panel
	 */
	@NotNull
	@Override
	public IPropertiesClient createPropertiesClientForQep(boolean willLockSharedObject)
	{
		return new QAPCapturePropertiesClient(getCapletModel(), willLockSharedObject);
	}

	public String getDoubleClickAction()
	{
		LogicEditSelectionHelper hesHelper =
				new LogicEditSelectionHelper(getSelectMgr().getPreSelections());
		return hesHelper.getDoubleClickAction();
	}
}