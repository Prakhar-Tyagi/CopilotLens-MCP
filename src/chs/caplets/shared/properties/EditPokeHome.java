/*
 * Copyright 2003-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.shared.properties;

import chs.caf.CAFUtils;
import chs.caf.caplet.IPropertiedSet;
import chs.caf.caplet.IPropertiesClientComponent;
import chs.caplets.shared.actions.PokeHomeDialog;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.shared.ISharedObject;
import chs.common.IProjectPreferenceMgr;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.PreferenceContext;
import chs.system.UIDMgr;
import chs.utilities.ResourceMgr;
import chs.utility.ui.IValidityListener;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class EditPokeHome implements IPropertiesClientComponent
{

	private static final String TAB_LABEL = ResourceMgr.getString(EditPokeHome.class, "EditPokeHome.Tab.Label");

	private PokeHomeDialog m_pokeHomeDialog;

	public EditPokeHome()
	{
	}

	public String getTabName(IPropertiedSet propset)
	{
		return TAB_LABEL;
	}

	/**
	 * Given a particular "PropertiedSet" object, checks to see if it can operate on the set.
	 */
	public boolean acceptsSet(IPropertiedSet propset)
	{
		if (isImplicitPokeHomeEnabled()) {
			// We dont use explicit PokeHome if implicit PokeHome is enabled, so we dont need Poke Home tab(dts0100853385)
			return false;
		}
		Iterator<IUID> iter = propset.iterator();
		while (iter.hasNext()) {
			IUID uid = iter.next();
			Object obj = UIDMgr.getObject(uid);
			if (obj instanceof IConductor) {
				IConductor cond = (IConductor) obj;
				chs.cof.logical.cable.IConductor ccond = cond.getConnectivity();
				if (ccond instanceof IWireConductor || ccond instanceof IShieldConductor) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isImplicitPokeHomeEnabled()
	{
		IProjectPreferenceMgr prefs = CAFUtils.getInstance().getCurrentProjectPreferences();
		final PreferenceContext context = PreferenceContext.determineContext(CAFUtils.getInstance().getActiveDiagram());
		assert prefs != null : "Preoject pref can't be null";
		return prefs.getEnableImplicitPokeHome(context);
	}

	public boolean modifiesSet(IPropertiedSet propset)
	{
		return acceptsSet(propset) && propset.isConnectivityEditable();
	}

	private List<IConductor> getSelectedConductors(IPropertiedSet ipropset)
	{
		List<IConductor> selectedWireList = new ArrayList<>();
		Iterator<IUID> iter = ipropset.iterator();
		while (iter.hasNext()) {
			IUID uid = iter.next();
			IUIDObject uidObj = UIDMgr.getObject(uid);
			if (uidObj instanceof IConductor) {
				IConductor schemCond = (IConductor) uidObj;
				chs.cof.logical.cable.IConductor cableCond = schemCond.getConnectivity();
				if ((cableCond instanceof IWireConductor) ||
						(cableCond instanceof IShieldConductor)) {
					selectedWireList.add(schemCond);
				}
			}
		}
		return selectedWireList;
	}

	/**
	 * Creates a panel for editing the poke home
	 */
	public JPanel getWidget(IPropertiedSet propset)
	{
		List<IConductor> list = getSelectedConductors(propset);
		m_pokeHomeDialog =
				constructPokeHomeDialog(list);   // CAFUtils.getInstance().getWindowMgr().getDialogFrame(), list);
		return m_pokeHomeDialog.getPanel(list);
	}

	@NotNull protected PokeHomeDialog constructPokeHomeDialog(List<IConductor> list)
	{
		return new PokeHomeDialog();
	}

	/**
	 * Returns true - this component should have it's own tab
	 */
	public boolean isPropPage()
	{
		return true;
	}

	/**
	 * Make necessary data changes
	 */
	public void edit(IPropertiedSet propset)
	{
//        m_pokeHomeDialog.setCancelled(false);
		m_pokeHomeDialog.editModel();
	}

	/**
	 *
	 */
	public void stopEditing(IPropertiedSet propset)
	{
	}

	/**
	 * @see IPropertiesClientComponent#destroy()
	 */
	public void destroy()
	{
	}

	/**
	 * @see IPropertiesClientComponent#isValid()
	 */
	public boolean isValid()
	{
		// todo Auto-generated method stub
		return true;
	}

	/**
	 * @see IPropertiesClientComponent#addValidityListener(IValidityListener)
	 */
	public void addValidityListener(IValidityListener listener)
	{
	}

	/**
	 * @see IPropertiesClientComponent#removeValidityListener(IValidityListener)
	 */
	public void removeValidityListener(IValidityListener listener)
	{
		System.err.println("EditPokeHoome.removeValidityListener() not implemented");
	}

	@Override public void addPropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener)
	{
	}

	public Set<ISharedObject> getSharedObjects()
	{
		return Collections.emptySet();
	}

	public Set<ISharedObject> getEditedSharedObjects()
	{
		return Collections.emptySet();
	}
}
