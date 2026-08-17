/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic;

import chs.caf.CAFUtils;
import chs.caf.SymbolLibraryBrowser;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.IEditClient;
import chs.caf.caplet.IPublisherCapletController;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.action.IActionMgr;
import chs.caf.caplet.helpers.IPropertiesClient;
import chs.caf.caplet.helpers.graphics.AddCommentSymbolAction;
import chs.caf.caplet.helpers.graphics.SymbolPlaceAsGraphicsAction;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.actions.AddInstanceAction;
import chs.caplets.logic.actions.DeleteAction;
import chs.caplets.logic.actions.SmartEditAction;
import chs.caplets.logic.actions.serviceDocumentation.PublisherDeleteAction;
import chs.caplets.logic.actions.serviceDocumentation.PublisherSmartEditAction;
import chs.caplets.logic.actions.serviceDocumentation.PublisherSmartEditPropertiesAction;
import chs.caplets.logic.actions.serviceDocumentation.PublisherUnPlaceAction;
import chs.caplets.logic.actions.serviceDocumentation.SelectActionImpl;
import chs.caplets.logic.actions.serviceDocumentation.offPage.FetchOffPageConnectivityAction;
import chs.caplets.logic.actions.serviceDocumentation.offPage.FetchWithOnlyPinsInSignalAction;
import chs.caplets.logic.actions.serviceDocumentation.shared.PublisherAddBackshellTerminationAction;
import chs.caplets.logic.actions.serviceDocumentation.shared.PublisherAddPinAction;
import chs.caplets.publisher.PublisherPropertiesAction;
import chs.caplets.shared.BaseController;
import chs.caplets.shared.actions.SelectAction;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolDef;
import chs.ctf.caf.ui.TextAttributesEditor;
import chs.ctf.editui.LogicEditSelectionHelper;
import chs.utility.SymbolUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;

/**
 * FEAT14997 - Offline Service Documentation User: kayyagar Date: Oct 12, 2010 Time: 7:28:07 PM
 */
public class SvcDocController extends LogicController implements IPublisherCapletController
{

	public SvcDocController(ICaplet caplet, ILogicDesign design, ISchemDiagram diagram)
	{
		super(caplet, design, diagram); // false means is not Logic
	}

	@Override @NotNull 	public IPropertiesClient createPropertiesClient()
	{
		return new SvcDocPropertiesClient(getCapletModel());
	}

	@NotNull @Override public IPropertiesClient createPropertiesClientForQep(boolean willLockSharedObject)
	{
		return new QAPSvcDocPropertiesClient(getCapletModel(), willLockSharedObject);
	}

	@NotNull @Override protected DeleteAction getDeleteAction()
	{
		return new PublisherDeleteAction(this);
	}

	@NotNull @Override protected DeleteAction getUnplaceAction()
	{
		return new PublisherUnPlaceAction(this);
	}

	@NotNull @Override protected SelectAction getSelectAction()
	{
		return new SelectActionImpl(this);
	}

	protected Class<? extends BaseController> getResourceClass()
	{
		// 	dts0100518262 - use LogicController properties
		return LogicController.class;
	}

	public String getDoubleClickAction()
	{
		LogicEditSelectionHelper hesHelper = new LogicEditSelectionHelper(getSelectMgr().getPreSelections());
		return "chs.caf.caplet.helpers.PropertiesAction".equals(hesHelper.getDoubleClickAction().trim()) ?
				"chs.caplets.publisher.PublisherPropertiesAction" :
				("chs.caplets.logic.actions.SmartEditAction".equals(hesHelper.getDoubleClickAction().trim()) ?
						"chs.caplets.logic.actions.serviceDocumentation.PublisherSmartEditAction" :
						hesHelper.getDoubleClickAction());
	}

	@Nullable public IEditClient getEditClient(SelectSet selections, @Nullable Object owner)
	{
		LogicEditSelectionHelper hesHelper = new LogicEditSelectionHelper(selections);
		return hesHelper.getEditClient(this);
	}

	public void addAnalysisTab(IDesign design)
	{
		//overriding the super class method as we dont need to add the analysis tab.
	}

	@Override protected void createLogicControllerActions()
	{
		super.createLogicControllerActions();
		removeAction((new SmartEditAction(this)).getActionName());
		addAction(new PublisherPropertiesAction(this, createPropertiesClient(), new TextAttributesEditor()));
		addAction(new PublisherSmartEditAction(this));
		addAction(new PublisherSmartEditPropertiesAction(this, createPropertiesClient(), new TextAttributesEditor()));
		addAction(new PublisherAddPinAction(this));
//		addAction(new PublisherAddPinWNAccelAction(this));
//		addAction(new PublisherAddBackshellAction(this));
		addAction(new PublisherAddBackshellTerminationAction(this));
		addPublisherControllerActions();
	}

	@Override public void addPublisherControllerActions()
	{
		IPublisherCapletController.super.addPublisherControllerActions();
		addAction(new FetchWithOnlyPinsInSignalAction(this));
		addAction(new FetchOffPageConnectivityAction(this));
	}

	@Override protected void initLibraryBrowser()
	{
		m_libBrowser = new SymbolLibraryBrowser(true)
		{
			protected void restart(IAction previousAction)
			{
				Class<?> actionClass = previousAction.getClass();
				if (actionClass == AddCommentSymbolAction.class) {
					int modifiers = 0;
					doubleClickFired(modifiers);
				}
			}

			protected void doubleClickFired(int mouseModifiers)
			{
				if (getCapletModel().isEditable()) {
					//
					IStamp sub = CAFUtils.getInstance().getCHSSystem().getSymbolLibraryMgr().getActiveSymbol();
					if (sub == null || !(sub instanceof ISymbolDef)) {
						return;
					}
					ISymbolDef subsd = (ISymbolDef) sub;

					IAction action =
							CAFUtils.getInstance().getActiveCapletController().getAction(AddInstanceAction.class);
					if (action == null || !action.isEnabled()) {
						if (SymbolUtils.isCommentSymbol(subsd)) {
							action = CAFUtils.getInstance().getActiveCapletController()
									.getAction(AddCommentSymbolAction.class);
						}
						//If its not a comment symbol and if the application is Publisher(SvcDoc)
						//Add the symbol as a comment symbol
						if (action == null || !action.isEnabled()) {
							action = CAFUtils.getInstance().getActiveCapletController()
									.getAction(SymbolPlaceAsGraphicsAction.class);
						}
					}
					if (action != null && action.isEnabled()) {
						ActionEvent ae =
								new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "addinstance", mouseModifiers);
						IActionMgr acm = CAFUtils.getInstance().getActiveActionMgr();
						if (acm != null) {
							// No reason it should be here, but the nullable says we must protect.
							acm.actionPerformed(action, ae);
						}
					}
				}
			}
		};
	}

	protected boolean areSharedObjectsSupported()
	{
		return false;
	}

	@Override protected boolean isSharedObjectCleanupSupported()
	{
		return true;
	}
}
