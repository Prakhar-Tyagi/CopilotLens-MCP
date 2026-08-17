/*
 * Copyright 2010 Mentor Graphics Corporation
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

/**
 * Created by IntelliJ IDEA. User: nagamani Date: Feb 8, 2010 Time: 11:05:53 AM To change this template use File |
 * Settings | File Templates.
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalSymbolDesigner, Application.CapitalSymbolForCapture, Application.CapitalEssentialsSymbolDesigner,
				Application.XSCSymbol, Application.SEElectricalSymbol})
public class ConvertPinTypeActionUI extends ActionUI
{

	private String m_name;

	public ConvertPinTypeActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setName(String name)
	{
		m_name = name;
	}

	public void setupUI()
	{
		putValue(NAME, m_name);
	}

	/**
	 * Return our matching ActionRT class
	 */
	public String getActionClass()
	{
		return ConvertPinTypeAction.class.getName();
	}
}
