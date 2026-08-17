/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.shared;

public class SaveParameters
{

	private boolean m_saveAlways;
	private boolean m_runDRCs;
	private boolean m_isModelModified;

	public SaveParameters saveAlways(boolean saveAlways)
	{
		m_saveAlways = saveAlways;
		return this;
	}

	public SaveParameters runDRCs(boolean runDRCs)
	{
		m_runDRCs = runDRCs;
		return this;
	}

	public SaveParameters modelModified(boolean isModelModified)
	{
		m_isModelModified = isModelModified;
		return this;
	}

	public boolean getSaveAlways()
	{
		return m_saveAlways;
	}

	public boolean getRunDRCs()
	{
		return m_runDRCs;
	}

	public boolean getModelModified()
	{
		return m_isModelModified;
	}
}