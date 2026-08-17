/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.function;

import chs.caf.cafmain.actions.bridges.BridgeOptionAction;
import chs.caf.cafmain.actions.topology.networks.AvionicsExportAction;
import chs.caf.caplet.IBrowserClient;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.IEditClient;
import chs.caf.caplet.action.IDeferredAction;
import chs.caf.caplet.action.IDeferredActionProcessor;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.caplet.helpers.IPropertiesClient;
import chs.caf.caplet.helpers.RegenerateGraphicsAction;
import chs.caf.caplet.helpers.browser.FunctionBrowserTree;
import chs.caf.caplet.helpers.browser.LogicBrowserTree;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.capture.actions.CaptureDeleteAction;
import chs.caplets.logic.BrowserClient;
import chs.caplets.logic.FunctionBrowserClient;
import chs.caplets.logic.LogicController;
import chs.caplets.logic.LogicObjectGraphBrowser;
import chs.caplets.logic.actions.DeleteAction;
import chs.caplets.logic.actions.DisconnectFunctionPortAction;
import chs.caplets.logic.actions.UpdateDictionaryAction;
import chs.caplets.shared.BaseController;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.ctf.editui.LogicEditSelectionHelper;
import chs.system.FactoryMgr;
import chs.utilities.CapabilityHelper;
import chs.utilities.SupportedFeatureInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides a controller specific to the {@link FunctionCaplet}
 */
public class FunctionController extends BaseController
{

	public FunctionController(ICaplet caplet, ILogicDesign design, ISchemDiagram diagram)
	{
		super(caplet, design, diagram); // true means is Logic
		createLogicControllerActions();
		FactoryMgr.getSystemFactory().getCHSSystem().getCHSUtils().setObjectBrowser(new LogicObjectGraphBrowser());
		IDeferredActionProcessor deferredActionProcessor = getDeferredActionProcessor();
		deferredActionProcessor.addDeferredAction(RegenerateGraphicsAction.getInstance());
		deferredActionProcessor.addDeferredAction((IDeferredAction) ConductorRouteAction.getInstance());
		addAction(new AvionicsExportAction(this));
		addAction(new BridgeOptionAction(this));
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

	protected Class<? extends BaseController> getResourceClass()
	{
		return LogicController.class;
	}
	@Override @NotNull protected LogicBrowserTree constructDesignBrowserTree(@NotNull IBrowserClient client, @NotNull String name)
	{
		return new FunctionBrowserTree(client, name);
	}
	@Override protected void createLogicControllerActions()
	{
		super.createLogicControllerActions();
		addAction(new DisconnectFunctionPortAction(this));
	}

	@Override protected void addUpdateDictionaryAction()
	{
		addAction(new UpdateDictionaryAction(this));
	}

	@Override @NotNull public IPropertiesClient createPropertiesClient()
	{
		return new FunctionPropertiesClient(getCapletModel());
	}

	/**
	 * Creates and returns a "Function" properties client object specifically tailored for the Quick Access Panel.
	 *
	 * @param willLockSharedObject Flag indicating whether to lock shared objects: {@code true} to lock,
	 * {@code false} to leave unlocked
	 * @return IPropertiesClient A properties client object tailored for the Quick Access Panel
	 */
	@NotNull
	@Override
	public IPropertiesClient createPropertiesClientForQep(boolean willLockSharedObject)
	{
		return new QAPFunctionPropertiesClient(getCapletModel(), willLockSharedObject);
	}

	@NotNull @Override protected DeleteAction getDeleteAction()
	{
		return new CaptureDeleteAction(this);
	}

	protected boolean areSharedObjectsSupported()
	{
		return CapabilityHelper.supports(SupportedFeatureInfo.Feature.LOGIC_SHARED_OBJECTS);
	}

	protected boolean isPartsBrowserSupported()
	{
		return false;
	}

	protected boolean isICDBrowserSupported()
	{
		return false;
	}

	protected BrowserClient getDesignConnectivityBrowserClient()
	{
		BrowserClient browserClient = new FunctionBrowserClient(this);
		browserClient.setTreeNodeDimmer(getTreeNodeDimmer());
		return browserClient;
	}

	public void addAnalysisTab(IDesign design)
	{
	}
}
