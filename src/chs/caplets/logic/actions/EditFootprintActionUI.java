/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

import javax.swing.ImageIcon;

/**
 * @author chandras on 07-07-2017.
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign,
		Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED
)
public class EditFootprintActionUI extends ActionUI
{

	public EditFootprintActionUI(@NotNull ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		String name = ResourceMgr.getStringForMenu(EditFootprintActionUI.class,
				"EditFootprintActionUI.name.decl");
		String sdesc = ResourceMgr.getStringForMenu(EditFootprintActionUI.class,
				"EditFootprintActionUI.shortDesc.decl");
		String ldesc = ResourceMgr.getStringForMenu(EditFootprintActionUI.class,
				"EditFootprintActionUI.longDesc.decl");
		char mnemonic = ResourceMgr.getMnemonic(EditFootprintActionUI.class,
				"EditFootprintActionUI.mnemonic.decl");

		putValue(NAME, name);
		putValue(SHORT_DESCRIPTION, sdesc);
		putValue(LONG_DESCRIPTION, ldesc);
		putValue(MNEMONIC_KEY, (int) mnemonic);
		ImageIcon imageIcon = CHSImageLoader.loadImageIcon("chs/images/javafx_ui/edit-device-connector-small.png");
		putValue(SMALL_ICON, imageIcon);
	}

	@Override public String getActionClass()
	{
		return EditFootprintAction.class.getName();
	}
}
