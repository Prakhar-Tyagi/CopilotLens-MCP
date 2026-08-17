/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.capture;

import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caplets.logic.BaseSystemLogicLifecycle;
import chs.caplets.shared.BaseLifecycle;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;


public class CaptureLifecycle extends BaseSystemLogicLifecycle
{

	@Nullable private Icon m_Icon;
	public CaptureLifecycle(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * @see BaseLifecycle#createController(ICaplet, ILogicDesign, ISchemDiagram)
	 */
	protected ICapletController createController(ICaplet caplet, ILogicDesign design, ISchemDiagram diagram)
	{
		return new CaptureController(caplet, design, diagram);
	}

	/**
	 * @see BaseLifecycle#getResourceClass()
	 */
	protected Class<? extends BaseLifecycle> getResourceClass()
	{
		return CaptureLifecycle.class;
	}

	@Override public boolean doesSupportObjectForInvoke(@NotNull final Object object)
	{
		return true;
	}

	
	@Override public void setDiagramTabIcon(@Nullable Icon icon)
	{
		m_Icon = icon;
	}

	@Nullable public Icon getDiagramIcon()
	{
		return m_Icon;
	}
}