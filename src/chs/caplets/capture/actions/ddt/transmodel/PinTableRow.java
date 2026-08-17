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

import chs.cof.logical.cable.IAbstractPin;

import java.util.Map;

/**
 * Simple storeage container.  Makes it a bit easier to keep track of things. Represents, transiently, a pin field row
 * in the pin field table.
 */
public class PinTableRow
{

	private String m_pinName;
	private IAbstractPin m_existingPin;
	private Map m_fieldValues;
	private boolean m_nameOverridden = false;

	public PinTableRow(String pinName_in, Map fieldValues_in)
	{
		m_pinName = pinName_in;
		m_fieldValues = fieldValues_in;
	}

	public void setPin(IAbstractPin pin)
	{
		m_existingPin = pin;
	}

	public IAbstractPin getExistingPin()
	{
		return m_existingPin;
	}

	public String getName()
	{
		return m_pinName;
	}

	public boolean isNameOverridden()
	{
		return m_nameOverridden;
	}

	public Map getFieldValues()
	{
		return m_fieldValues;
	}

	public void setName(String name)
	{
		m_pinName = name;
		m_nameOverridden = true;
	}
}
