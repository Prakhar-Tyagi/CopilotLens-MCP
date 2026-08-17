/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.capture.actions.ddt.transmodel;

import chs.cof.project.ddtrans.IDDTType;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Creation information Date: Jul 25, 2005 Time: 9:31:54 AM Description:
 * <p/>
 * Transiently represents an IDDTType object. This is used for editing within the DDT Type editing dialog.
 */
public class DDTTypeTransient
{

	public static String DEFAULT_TYPE_NAME = "";
	private String m_name;

	/**
	 * Collection of strings representing the field list
	 */
	private List m_fields = new LinkedList();

	/**
	 * Collection of strings representing the field list
	 */
	private List m_pinFields = new LinkedList();

	/**
	 * Keeps track of the persistent type that this transient type represents. This may be null if the type is a new one.
	 */
	private IDDTType m_wrappedType = null;

	public DDTTypeTransient()
	{
		this(null);
	}

	public DDTTypeTransient(IDDTType dtype)
	{
		m_wrappedType = dtype;
		if (dtype != null) {
			m_name = dtype.getName();
			replaceFields(new LinkedList(dtype.getAllFields()));
			replacePinFields(new LinkedList(dtype.getPinFields().getAllFields()));
		}
		else {
			m_name = new String(DEFAULT_TYPE_NAME);
		}
	}

	public IDDTType getWrappedType()
	{
		return m_wrappedType;
	}

	public void addField(String field)
	{
		m_fields.add(field);
	}

	public void removeField(String field)
	{
		m_fields.remove(field);
	}

	public void replaceFields(Collection fields)
	{
		m_fields = new LinkedList(fields);
	}

	public void replacePinFields(Collection pinFields)
	{
		m_pinFields = new LinkedList(pinFields);
	}

	public void addPinField(String field)
	{
		m_pinFields.add(field);
	}

	public void removePinField(String field)
	{
		m_pinFields.remove(field);
	}

	public List getPinFields()
	{
		return m_pinFields;
	}

	public List getFields()
	{
		return m_fields;
	}

	public String getName()
	{
		return m_name;
	}

	public void setName(String newName)
	{
		m_name = newName;
	}
}
