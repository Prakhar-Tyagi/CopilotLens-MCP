/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.serviceDocumentation.shared;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.Selection;
import chs.caf.caplet.selection.SelectionIterator;
import chs.caplets.logic.actions.AddPinAction;
import chs.caplets.logic.actions.AddPinActionUI;
import chs.cof.drawplus.IConnectivityObjectProvider;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.common.IUIDObject;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.List;
import java.util.Set;

/**
 * Following are the differences of Publisher add pin action with its parent
 * <p>
 * <p>
 * 1. Only place pins/shared pins and not add pins
 * <p>
 * 2. not show the stack pins checkbox
 * <p>
 * 3. mark added objects as supplementary
 * <p>
 * 4. add mated pins so that connectivity is not changed
 */
public class PublisherAddPinAction extends AddPinAction
{

	public PublisherAddPinAction(ICapletController controller)
	{
		super(controller);
	}

	@NotNull @Override public String getActionUIClass()
	{
		return UI.class.getName();
	}

	@Override public boolean isEnabled()
	{
		boolean enabled = super.isEnabled();
		SelectSet selections = getController().getSelectMgr().getPreSelections();
		if (isAnySelectionNonShared(selections)) {
			return false;
		}
		return enabled;
	}

	private static boolean isAnySelectionNonShared(SelectSet selections)
	{
		for (SelectionIterator sitr = selections.getSelected(); sitr.hasNext(); ) {
			Selection sel = sitr.getNext();
			IUIDObject uobj = sel.getObject();
			if (uobj instanceof IRepresentedObject) {
				uobj = ((IConnectivityObjectProvider) uobj).getRawConnectivity();
			}
			if (isShared(uobj) || isDesignWideShared(uobj)) {
			}
			else {
				return true;
			}
		}
		return false;
	}

	private static boolean isShared(@Nullable IUIDObject uobj)
	{
		return uobj instanceof ILogicObject && ((ILogicObject) uobj).getSharedObject() != null;
	}

	private static boolean isDesignWideShared(@Nullable IUIDObject uobj)
	{
		if (!(uobj instanceof ILogicObject)) {
			return false;
		}
		ILogicObject logicObject = (ILogicObject) uobj;
		ILogicDesign logicDesign = logicObject.getLogicDesign();
		if (logicDesign == null) {
			return false;
		}
		IDesignWideUsageMgr designWideUsageMgr = logicDesign.getDesignWideUsageMgr();
		List<IDesignSharedUsage> usages = designWideUsageMgr.getUsages(logicObject);
		return usages.size() > 1;
	}

	@Override protected boolean initAddPinModel()
	{
		boolean initResult = super.initAddPinModel();
		Set<IPinList> pinLists = m_addPinActionModel.getPinLists();
		if (m_addPinActionPresenter instanceof PublisherAddPinActionHelper) {
			((PublisherAddPinActionHelper) m_addPinActionPresenter).cacheSharedPinDetails(pinLists);
		}
		return initResult;
	}

	protected void setupActionHelper()
	{
		m_addPinActionPresenter = new PublisherAddPinActionHelper(this, false, true);
	}

	protected boolean initializePresenter(boolean altPress, boolean shiftNotPressed)
	{
		return m_addPinActionPresenter.initialize(m_addPinActionModel, true, shiftNotPressed);
	}

	@ApplicationSpecification(
			includeIn = {Application.SvcDoc})
	public static class UI extends AddPinActionUI
	{

		public UI(ICaplet caplet)
		{
			super(caplet);
		}

		@NotNull @Override public String getActionClass()
		{
			return PublisherAddPinAction.class.getName();
		}

		@Override public void setupUI()
		{
			super.setupUI();
			Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/publisher-place-pin-small.png");
			putValue(NAME, ResourceMgr.getString(PublisherAddPinAction.class, "PublisherAddPinAction.name.decl"));
			putValue(SHORT_DESCRIPTION,
					ResourceMgr.getString(PublisherAddPinAction.class, "PublisherAddPinAction.shortDesc.decl"));
			putValue(LONG_DESCRIPTION,
					ResourceMgr.getString(PublisherAddPinAction.class, "PublisherAddPinAction.longDesc.decl"));
			putValue(SMALL_ICON, icon);
		}

		@Override public Icon getInactiveIcon()
		{
			return CHSImageLoader.loadImageIcon("chs/images/app/publisher-place-pin-small.png");
		}
	}
}
