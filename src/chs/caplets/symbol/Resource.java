/*
 * Copyright 2002-2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol;

// caf imports

import chs.caf.ActionCheckBox;
import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.ActionSeparator;
import chs.caf.CAFUtils;
import chs.caf.IFIB;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.cafmain.actions.SelectByNameAction;
import chs.caf.cafmain.actions.SelectByPropertyAction;
import chs.caf.cafmain.actions.analysis.AttachModelActionUI;
import chs.caf.cafmain.actions.analysis.AttachSVModelActionUI;
import chs.caf.cafmain.actions.analysis.BuildModelActionUI;
import chs.caf.cafmain.actions.analysis.EditModelActionUI;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.action.IActionUI;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.caplet.helpers.SmartEditActionUI;
import chs.caf.helpers.ui.common.CapletResourceBuilder;
import chs.caplets.shared.BaseSymbolResource;
import chs.caplets.shared.ViewHelper;
import chs.caplets.symbol.actions.AddAttributeDatumActionUI;
import chs.caplets.symbol.actions.AddBlockInstanceActionUI;
import chs.caplets.symbol.actions.AddDatumActionUI;
import chs.caplets.symbol.actions.AddDrillPointDatumActionUI;
import chs.caplets.symbol.actions.AddEngineeringDatumActionUI;
import chs.caplets.symbol.actions.AddFixturePlacementDatumActionUI;
import chs.caplets.symbol.actions.AddGenericDatumActionUI;
import chs.caplets.symbol.actions.AddInternalPinActionUI;
import chs.caplets.symbol.actions.AddPinActionUI;
import chs.caplets.symbol.actions.AddPortActionUI;
import chs.caplets.symbol.actions.ConvertPinTypeActionUI;
import chs.caplets.symbol.actions.ConvertToDiodeLinkActionUI;
import chs.caplets.symbol.actions.ConvertToFuseLinkActionUI;
import chs.caplets.symbol.actions.ConvertToResistanceLinkActionUI;
import chs.caplets.symbol.actions.CreateGridDatumActionUI;
import chs.caplets.symbol.actions.CreateInternalLinkDiodeActionUI;
import chs.caplets.symbol.actions.CreateInternalLinkFuseActionUI;
import chs.caplets.symbol.actions.CreateInternalLinkResistanceActionUI;
import chs.caplets.symbol.actions.CreateNameTextActionUI;
import chs.caplets.symbol.actions.CreateXRefTextActionUI;
import chs.caplets.symbol.actions.DeleteActionUI;
import chs.caplets.symbol.actions.FlattenBlockActionUI;
import chs.caplets.symbol.actions.ReorderDatumActionUI;
import chs.caplets.symbol.actions.ReverseDiodeDirectionActionUI;
import chs.caplets.symbol.actions.UpdateInstanceActionUI;
import chs.caplets.symbol.actions.ViewRelatedSymbolActionUI;
import chs.cof.EngineeringDatumType;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.SymbolTypeEnum;
import chs.images.CHSImageLoader;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utility.AnalysisHelper;
import chs.utility.logic.ISymbolModel;
import com.mentor.capital.ui.IToggleAction;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Iterator;
import java.util.Locale;

/**
 * Resource initialization for CSymbol.
 */
public class Resource extends BaseSymbolResource
{

	public Resource(ICaplet theCaplet)
	{
		super(theCaplet);
	}

	public void init(Locale locale)
	{
		super.init(locale);
	}

	@SuppressWarnings({"ResultOfObjectAllocationIgnored"}) protected void initActions()
	{
		super.initActions();
		new UpdateInstanceActionUI(caplet);
		new ViewRelatedSymbolActionUI(caplet);
		new ReorderDatumActionUI(caplet);
		new ConvertPinTypeActionUI(caplet);
		new ReverseDiodeDirectionActionUI(caplet);
		new ConvertToResistanceLinkActionUI(caplet);
		new ConvertToDiodeLinkActionUI(caplet);
		new ConvertToFuseLinkActionUI(caplet);
		new SmartEditActionUI(caplet);
	}

	protected void initViewMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		super.initViewMenu(rb, menu);
		Action toggleLinksVisibilityAction =
				new ToggleLinksVisibilityAction();
		ActionCheckBox toggleLinksVisibility =
				new ActionCheckBox(toggleLinksVisibilityAction, FactoryMgr.getDrawFactory().isLinksVisibilityOn());
		menu.add(toggleLinksVisibility);
	}

	protected void initEditMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		super.initEditMenu(rb, menu);

		menu.add(new ActionSeparator());
		rb.addActionUI(new DeleteActionUI(caplet), menu);

		menu.add(new ActionSeparator());
		rb.addAppAction(new SelectByNameActionImpl(caplet.getFIB()), menu);
		rb.addAppAction(new SymbolSelectByPropertyAction(), menu);
		menu.add(new ActionSeparator());
		rb.addActionUI(getPropertiesActionUI(), menu);
	}

	protected void addAnalysisActions()
	{
		Integer iMnemonic = KeyEvent.VK_Y;
		ActionContainer analysisMenu =
				new ActionContainer(ResourceMgr.getString(Resource.class, "Resource.Menu.Analysis.Label"),
						false,
						CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"),
						iMnemonic,
						ResourceMgr.getString(Resource.class, "Resource.Menu.AnalysisMenu.Label"));

		iMnemonic = KeyEvent.VK_C;
		ActionContainer analysisToolsMenu =
				new ActionContainer(ResourceMgr.getString(Resource.class, "Resource.Menu.AnalysisComponent.Label"),
						true,
						CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"),
						iMnemonic,
						ResourceMgr.getString(Resource.class, "Resource.Menu.AnalysisComponentMenu.Label"));

		if (AnalysisHelper.getInstance().isLegacyAnalysisMode()) {
			IActionUI attachModelAction = new AttachModelActionUI(caplet);
			analysisToolsMenu.add(new ActionEntry(attachModelAction));

			IActionUI editModelAction = new EditModelActionUI(caplet);
			analysisToolsMenu.add(new ActionEntry(editModelAction));

			IActionUI buildModelAction = new BuildModelActionUI(caplet);
			analysisToolsMenu.add(new ActionEntry(buildModelAction));
		}
		else {
			ActionUI svAttachModelAction = new AttachSVModelActionUI(caplet);
			analysisToolsMenu.add(new ActionEntry(svAttachModelAction));
		}

		analysisMenu.add(analysisToolsMenu);
		menus.add(analysisMenu);
	}

	@Override protected void initAddMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		// base class should not be adding anything to this menu - ignore it if it does!
		rb.addActionUI(new AddPinActionUI(caplet), menu);
		rb.addActionUI(new AddPortActionUI(caplet), menu);
		rb.addActionUI(new AddInternalPinActionUI(caplet), menu);
		rb.addActionUI(new AddBlockInstanceActionUI(caplet), menu);
		rb.addActionUI(new AddDatumActionUI(caplet), menu);
		rb.addActionUI(new CreateGridDatumActionUI(caplet), menu);

		ActionContainer addEngineeringDatumMenu =
				CapletResourceBuilder.createSubContainer("AddEngineeringDatum", getClass());
		initAddEngineeringDatumMenu(rb, addEngineeringDatumMenu);
		menu.add(addEngineeringDatumMenu);

		rb.addActionUI(new AddAttributeDatumActionUI(caplet), menu);
		rb.addActionUI(new AddDrillPointDatumActionUI(caplet), menu);
		rb.addActionUI(new AddFixturePlacementDatumActionUI(caplet), menu);
		rb.addActionUI(new AddGenericDatumActionUI(caplet), menu);

		menu.add(new ActionSeparator());
		initAddInternalLink(rb, menu);
	}

	private void initAddEngineeringDatumMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		for (EngineeringDatumType resType : EngineeringDatumType.values()) {
			rb.addActionUI(new AddEngineeringDatumActionUI(caplet, resType.getEngineeringDatumType()), menu);
		}
	}

	protected void initLayoutMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		rb.addActionUI(new FlattenBlockActionUI(caplet), menu);
		super.initLayoutMenu(rb, menu);
	}

	protected void initAddInternalLink(CapletResourceBuilder rb, ActionContainer menu)
	{
		ActionContainer internalLinksMenu = CapletResourceBuilder.createSubContainer("AddInternalLinks", getClass());
		rb.addActionUI(new CreateInternalLinkResistanceActionUI(caplet), internalLinksMenu);
		rb.addActionUI(new CreateInternalLinkFuseActionUI(caplet), internalLinksMenu);
		rb.addActionUI(new CreateInternalLinkDiodeActionUI(caplet), internalLinksMenu);
		menu.add(internalLinksMenu);
	}

	protected void initToolbars(CapletResourceBuilder rb)
	{
		super.initToolbars(rb);
		ActionContainer symbolToolBar = new ActionContainer("Symbol");
		ActionContainer addPinToolBar = new ActionContainer("Add Pin", true);
		rb.addActionUIEntry(AddPinActionUI.class, addPinToolBar);
		rb.addActionUIEntry(AddInternalPinActionUI.class, addPinToolBar);
		symbolToolBar.add(addPinToolBar);

		ActionContainer addLinkToolBar = new ActionContainer("Add Link", true);
		rb.addActionUIEntry(CreateInternalLinkResistanceActionUI.class, addLinkToolBar);
		rb.addActionUIEntry(CreateInternalLinkFuseActionUI.class, addLinkToolBar);
		rb.addActionUIEntry(CreateInternalLinkDiodeActionUI.class, addLinkToolBar);
		symbolToolBar.add(addLinkToolBar);

		rb.addActionUIEntry(AddBlockInstanceActionUI.class, symbolToolBar);
		rb.addActionUIEntry(CreateNameTextActionUI.class, symbolToolBar);
		rb.addActionUIEntry(CreateXRefTextActionUI.class, symbolToolBar);
		toolbars.add(symbolToolBar);
	}

	protected Class<?> getResourceClass()
	{
		return Resource.class;
	}

	@ApplicationSpecification(
			includeIn = {Application.CapitalSymbolDesigner, Application.CapitalSymbolForCapture, Application.CapitalEssentialsSymbolDesigner,
					Application.XSCSymbol, Application.SEElectricalSymbol})
	private class SymbolSelectByPropertyAction extends SelectByPropertyAction
	{

		protected SymbolSelectByPropertyAction()
		{
			super(caplet.getFIB());
		}

		protected Iterator<ICapletView> getViews()
		{
			return ViewHelper.getAllActiveSymbolViews();
		}
	}

	@ApplicationSpecification(
			includeIn = {Application.CapitalSymbolDesigner, Application.CapitalSymbolForCapture, Application.CapitalEssentialsSymbolDesigner,
					Application.XSCSymbol, Application.SEElectricalSymbol})
	private static class ToggleLinksVisibilityAction extends AbstractAction implements IToggleAction
	{

		protected ToggleLinksVisibilityAction()
		{
			super(ResourceMgr.getString(Resource.class, "Resource.ShowInternalLinks.name"),
					CHSImageLoader.loadImageIcon("chs/images/app/ico_show_internal_connectivity.gif"));
			putValue(LONG_DESCRIPTION, "Show InternalLinks");
		}

		public void actionPerformed(ActionEvent e)
		{
			ICapletView view = CAFUtils.getInstance().getActiveCapletView();
			ISymbolModel m_symModel = (ISymbolModel) view.getController().getCapletModel();

			boolean newState = !m_symModel.isLinksVisible();
			if (newState) {
				putValue(SMALL_ICON, "chs/images/app/ico_hide_internal_connectivity.gif");
			}
			else {
				putValue(SMALL_ICON, "chs/images/app/ico_show_internal_connectivity.gif");
			}
			m_symModel.setLinksVisibility(newState);
		}

		public boolean isOn()
		{
			return isEnabled() && FactoryMgr.getDrawFactory().isLinksVisibilityOn();
		}

		public boolean isEnabled()
		{
			ICapletView view = CAFUtils.getInstance().getActiveCapletView();
			ISymbolModel m_symModel = view != null ? (ISymbolModel) view.getController().getCapletModel() : null;
			ISymbolDef symbolDef = m_symModel != null && m_symModel.getSymbolDef() instanceof ISymbolDef ?
					(ISymbolDef) m_symModel.getSymbolDef() : null;
			boolean isFunctionSymbol = symbolDef != null && symbolDef.getSymbolType().equals(SymbolTypeEnum.FUNCTION);
			return !isFunctionSymbol && super.isEnabled();
		}
	}

	public static class SelectByNameActionImpl extends SelectByNameAction
	{

		protected SelectByNameActionImpl(IFIB fib)
		{
			super(fib);
		}

		public Iterator<ICapletView> getViews()
		{
			return ViewHelper.getAllActiveSymbolViews();
		}
	}
}
