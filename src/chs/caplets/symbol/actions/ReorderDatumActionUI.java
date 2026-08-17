/*
 * Copyright 2007-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.caplet.selection.ISelectListener;
import chs.caf.caplet.selection.SelectEvent;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;

import javax.swing.Icon;

/**
 * Created by IntelliJ IDEA. User: skelkar Date: Jun 26, 2007 Time: 1:40:50 PM To change this template use File |
 * Settings | File Templates.
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalSymbolDesigner, Application.CapitalSymbolForCapture, Application.CapitalEssentialsSymbolDesigner,
				Application.XSCSymbol, Application.SEElectricalSymbol})
public class ReorderDatumActionUI extends ActionUI implements ISelectListener
{

	public ReorderDatumActionUI(ICaplet caplet)
	{
		super(caplet);
		setupUI();
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon(CHSImages.TRANSPARENT_ICON);
		putValue(NAME, "Reoreder Datums");
		putValue(SHORT_DESCRIPTION, "Reoreder Datums");
		putValue(LONG_DESCRIPTION, "Reoreder Datums");
		putValue(SMALL_ICON, icon);
		getFIB().getAppActionMgr().addSelectListener(this, true);
	}

	public String getActionClass()
	{
		return ReorderDatumAction.class.getName();
	}

	public void selectionChanged(SelectEvent e)
	{
	}
}

