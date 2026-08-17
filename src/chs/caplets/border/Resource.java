/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2003-2023 Siemens
 */
package chs.caplets.border;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.ActionSeparator;
import chs.caf.AppAction;
import chs.caf.CAFUtils;
import chs.caf.ICAFWindow;
import chs.caf.ICtxMenuProvider;
import chs.caf.IFIB;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ICapletWindow;
import chs.caf.caplet.helpers.SmartEditActionUI;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.helpers.ui.common.CapletResourceBuilder;
import chs.caplets.border.actions.CreateFormboardRegionDatumActionUI;
import chs.caplets.border.actions.EditUserDefinedZonesActionUI;
import chs.caplets.border.actions.UserZonePropertiesActionUI;
import chs.caplets.shared.BaseSymbolResource;
import chs.caplets.shared.actions.ModifyZoneAreaActionUI;
import chs.caplets.symbol.Model;
import chs.caplets.symbol.actions.AddDrillPointDatumActionUI;
import chs.caplets.symbol.actions.AddGenericDatumActionUI;
import chs.caplets.symbol.actions.DeleteActionUI;
import chs.cof.draw.ICompoundObject;
import chs.cof.drawplus.BorderHolder;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.logic.ISymbolModel;
import com.mentor.capital.ui.IToggleAction;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Resource initialization for Border.
 */
public class Resource extends BaseSymbolResource
{

	private ToggleConstructionGraphicsAction toggleConstructionGfxAction;

	public Resource(ICaplet theCaplet)
	{
		super(theCaplet);
	}

	@SuppressWarnings({"ResultOfObjectAllocationIgnored"}) protected void initActions()
	{
		super.initActions();
		new SmartEditActionUI(caplet);
	}

	protected void initEditMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		super.initEditMenu(rb, menu);
		menu.add(new ActionSeparator());
		rb.addActionUI(new DeleteActionUI(caplet), menu);
		rb.addActionUI(new ModifyZoneAreaActionUI(caplet), menu);
		rb.addActionUI(new UserZonePropertiesActionUI(caplet), menu);
		menu.add(new ActionSeparator());
		rb.addActionUI(getPropertiesActionUI(), menu);
	}

	protected void initViewMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		super.initViewMenu(rb, menu);
		menu.add(new ActionEntry(getDisplayConstructionGraphics()));
	}

	@Override protected void initAddMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		rb.addActionUI(new AddGenericDatumActionUI(caplet), menu);
		rb.addActionUI(new AddDrillPointDatumActionUI(caplet), menu);
		rb.addActionUI(new CreateFormboardRegionDatumActionUI(caplet), menu);
		rb.addActionUI(new EditUserDefinedZonesActionUI(caplet), menu);
	}

	public AppAction getDisplayConstructionGraphics()
	{
		if (toggleConstructionGfxAction == null) {
			toggleConstructionGfxAction = new ToggleConstructionGraphicsAction(caplet.getFIB());
		}
		return toggleConstructionGfxAction;
	}

	@ApplicationSpecification(
			includeIn = {Application.CapitalSymbolDesigner, Application.CapitalSymbolForCapture,
					Application.CapitalEssentialsSymbolDesigner,
					Application.XSCSymbol, Application.SEElectricalSymbol})
	private class ToggleConstructionGraphicsAction extends AppAction implements ICtxMenuProvider, IToggleAction
	{

		private boolean toggleState = true;

		ToggleConstructionGraphicsAction(IFIB fib)
		{
			super(fib);
			putValue(NAME, ResourceMgr.getString(Resource.class, "Resource.putValue.action.text"));
			putValue(SHORT_DESCRIPTION, getValue(NAME));
			putValue(LONG_DESCRIPTION, ResourceMgr.getString(Resource.class, "Resource.putValue.action.text_1"));
			putValue(MNEMONIC_KEY, Integer.valueOf(KeyEvent.VK_S));
			putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"));
		}

		public void updateUI()
		{
		}

		public void actionPerformed(ActionEvent e)
		{
			toggleState = !toggleState;
			ICapletController controller = CAFUtils.getInstance().getActiveCapletController();
			ICompoundObject co = getBorderHolder();
			if (co != null && co instanceof BorderHolder) {
				((BorderHolder) co).setDisplayingConstructionGraphics(toggleState);
			}
			for (ICAFWindow window : CAFUtils.getInstance().getWindowMgr().getWindows()) {
				if (window instanceof ICapletWindow) {
					ICapletWindow cw = (ICapletWindow) window;
					if (cw.getController() == controller) {
						//
						// One of mine - Clear selections too
						//
						controller.getSelectMgr().getPreSelections().clear();
						controller.getSelectMgr().getCurrentSelections().clear();
						controller.getSelectMgr().getPreviewSelections().clear();
						cw.getCurrentView().invalidate(IViewInvalidationEnum.eFull);
					}
				}
			}
		}

		@Override public boolean isOn()
		{
			return toggleState;
		}

		public void populateCtxMenu(ActionContainer container, SelectSet selections)
		{
		}

		public void populateActiveCtxMenu(ActionContainer container)
		{
		}

		@Nullable private ICompoundObject getBorderHolder()
		{
			ICapletController controller = CAFUtils.getInstance().getActiveCapletController();
			if (controller == null) {
				return null;
			}
			ICapletModel model = controller.getCapletModel();
			if (model == null) {
				return null;
			}
			if (model instanceof Model) {
				return ((ISymbolModel) model).getSymbolDef().getGfx();
			}
			return null;
		}
	}
}
