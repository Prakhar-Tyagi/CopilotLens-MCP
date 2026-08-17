/*
 * Copyright 2002-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.shared.properties;

import chs.caf.caplet.IPropertiedSet;
import chs.caf.caplet.IPropertiesClientComponent;
import chs.cof.logical.shared.ISharedObject;
import chs.utility.ui.IValidityListener;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import java.beans.PropertyChangeListener;
import java.util.Set;

/**
 * Class for saving away port information for dialog editing.
 */
// TODO jacobt FEAT14396: Remove this class - all conductors are ported
public class EditPort implements IPropertiesClientComponent
{

	public JPanel getWidget(IPropertiedSet propset)
	{
		assert false : "remove obsolete code?";
		return null;
	}

	public boolean isPropPage()
	{
		assert false : "remove obsolete code?";
		return false;
	}

	public String getTabName(IPropertiedSet propset)
	{
		assert false : "remove obsolete code?";
		return null;
	}

	public boolean acceptsSet(IPropertiedSet propset)
	{
		assert false : "remove obsolete code?";
		return false;
	}

	public boolean modifiesSet(IPropertiedSet propset)
	{
		assert false : "remove obsolete code?";
		return false;
	}

	public void edit(IPropertiedSet propset)
	{
		assert false : "remove obsolete code?";
	}

	public Set<ISharedObject> getSharedObjects()
	{
		assert false : "remove obsolete code?";
		return null;
	}

	public Set<ISharedObject> getEditedSharedObjects()
	{
		assert false : "remove obsolete code?";
		return null;
	}

	public void stopEditing(IPropertiedSet propset)
	{
		assert false : "remove obsolete code?";
	}

	public void destroy()
	{
		assert false : "remove obsolete code?";
	}

	public boolean isValid()
	{
		assert false : "remove obsolete code?";
		return false;
	}

	public void addValidityListener(IValidityListener listener)
	{
		assert false : "remove obsolete code?";
	}

	public void removeValidityListener(IValidityListener listener)
	{
		assert false : "remove obsolete code?";
	}

	@Override public void addPropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener)
	{
	}
}