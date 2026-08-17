/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.cof.logical.IDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.images.CHSImageLoader;

import javax.swing.Icon;

public abstract class AbstractInterconnectChildHyperlink extends AbstractInterconnectHyperlink
{

	private static final Icon DIAGRAM_ICON = CHSImageLoader.loadImageIcon("chs/images/app/ico_diagram.gif");

	protected AbstractInterconnectChildHyperlink(ISchemDiagram currentDiagram, IDesign design, ISchemDiagram diagram)
	{
		super(currentDiagram, design, diagram);
	}

	public Icon getIcon()
	{
		return DIAGRAM_ICON;
	}
}
