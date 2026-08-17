/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.capture.actions.ddt;

import chs.caf.CAFUtils;
import chs.caplets.capture.actions.ddt.transmodel.DDTTypeTransient;
import chs.cof.project.IProjectFactory;
import chs.cof.project.ddtrans.IDDTType;
import chs.cof.project.ddtrans.IDDTTypeMgr;
import chs.common.ICommonFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Creation information Date: Jul 22, 2005 Time: 10:23:41 AM Description:
 * <p/>
 * This maintains the transient state of the DDT Type information. We must do this to revert things back to the way they
 * were on a "cancel".  This model just duplicates what is in the typeMgr to begin with.  After the fact the type mgr
 * can be replaced with the results. This is done carefully to preserve UIDs where possible.
 */
public class EditDDTTypesDialogModel
{

	private Map m_nameTypeMap;
	private IDDTTypeMgr m_originalTypeMgr;

	public EditDDTTypesDialogModel(IDDTTypeMgr typeMgr)
	{
		setTypeMgr(typeMgr);
	}

	public Collection getTypes()
	{
		return m_nameTypeMap.values();
	}

	public Set getTypeNames()
	{
		return m_nameTypeMap.keySet();
	}

	public void applyTransientChanges(IProjectFactory projFact)
	{
		ICommonFactory commonFact = CAFUtils.getInstance().getCommonFactory();
		// Copy to avoid concurrent mod. exception since getDDTTypes returns its internals..
		Collection removedTypes = new LinkedHashSet(m_originalTypeMgr.getDDTTypes());
		for (Iterator itr = m_nameTypeMap.values().iterator(); itr.hasNext();) {
			DDTTypeTransient transType = (DDTTypeTransient) itr.next();
			IDDTType ddtType = m_originalTypeMgr.findTypeByName(transType.getName());

			if (ddtType == null) {
				ddtType = projFact.constructDDTType(commonFact.createUID());
				m_originalTypeMgr.addDDTType(ddtType);
			}
			else {
				removedTypes.remove(ddtType);
			}

			// From here, the rest are just strings so we can just blow them away and replace them with
			// the transient pins.
			ddtType.setName(transType.getName());
			ddtType.setAllFields(transType.getFields());
			ddtType.getPinFields().setAllFields(transType.getPinFields());
		}

		// If there are types that were in our type manager to begin with but are no longer in this model
		// then they must have been deleted.
		for (Iterator remTypesIter = removedTypes.iterator(); remTypesIter.hasNext();) {
			IDDTType ddtType = (IDDTType) remTypesIter.next();
			m_originalTypeMgr.removeDDTType(ddtType);
		}
	}

	public DDTTypeTransient getType(String selectedTypeName)
	{
		return (DDTTypeTransient) m_nameTypeMap.get(selectedTypeName);
	}

	public void removeTypeByName(String selectedTypeName)
	{
		m_nameTypeMap.remove(selectedTypeName);
	}

	public void addNewType(DDTTypeTransient newType)
	{
		m_nameTypeMap.put(newType.getName(), newType);
	}

	public IDDTTypeMgr getTypeMgr()
	{
		return m_originalTypeMgr;
	}

	public void setTypeMgr(IDDTTypeMgr typeMgr)
	{
		m_nameTypeMap = new LinkedHashMap();

		for (Iterator itr = typeMgr.getDDTTypes().iterator(); itr.hasNext();) {
			IDDTType dtype = (IDDTType) itr.next();

			DDTTypeTransient transType = new DDTTypeTransient(dtype);

			m_nameTypeMap.put(dtype.getName(), transType);
		}
		m_originalTypeMgr = typeMgr;
	}
}
