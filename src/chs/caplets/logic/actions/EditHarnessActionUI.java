/*
 * Copyright 2006-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.caplet.selection.ISelectListener;
import chs.caf.caplet.selection.SelectEvent;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign,
		Application.SEElectricalDesign}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
public class EditHarnessActionUI extends ActionUI implements ISelectListener
{

	private static ISelectListener m_listener = null;
	private static boolean m_enabled = false;

	public EditHarnessActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon(CHSImages.EDIT_HARNESS_ICON);
		Integer iMnemonic =
				new Integer(ResourceMgr.getMnemonic(EditHarnessActionUI.class, "EditHarnessActionUI.mnemonic"));

		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, ResourceMgr.getStringForMenu(EditHarnessActionUI.class, "EditHarnessActionUI.name"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getStringForMenu(EditHarnessActionUI.class, "EditHarnessActionUI.short"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(EditHarnessActionUI.class, "EditHarnessActionUI.longDesc"));
		putValue(SMALL_ICON, icon);

		// the following code it to enable the action when there are items selected
		if (m_listener == null) {
			m_listener = new ISelectListener()
			{
				public void selectionChanged(SelectEvent e)
				{
					if (CAFUtils.getInstance().getActiveCapletController() != null &&
							!CAFUtils.getInstance().getActiveCapletController().getCapletModel().isEditable()) {
						return;
					}
					m_enabled = (e.getSelectSource().getSelectCount() > 0);
				}
			};
			getCaplet().getFIB().getAppActionMgr().addSelectListener(m_listener, true);
		}
		getCaplet().getFIB().getAppActionMgr().addSelectListener(this, true);
	}

	public String getActionClass()
	{
		return EditHarnessAction.class.getName();
	}

	/**
	 * @see chs.caf.caplet.selection.ISelectListener#selectionChanged(chs.caf.caplet.selection.SelectEvent)
	 */
	public void selectionChanged(SelectEvent e)
	{
		if (CAFUtils.getInstance().getActiveCapletController() != null &&
				!CAFUtils.getInstance().getActiveCapletController().getCapletModel().isEditable()) {
			return;
		}
		setEnabled(m_enabled);
	}

	/**
	 * @see chs.caf.caplet.helpers.ActionUI#isEnabled()
	 */
	public boolean isEnabled()
	{
		return m_enabled;
	}
}