/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.symbol;

import chs.caf.caplet.helpers.GridScaleSettings;
import chs.cof.symbol.IAbstractLibrary;
import chs.cof.symbol.SymbolTypeEnum;
import chs.ctf.caf.ui.SimpleOkCancelDialog;
import org.jetbrains.annotations.NotNull;

import java.awt.Dimension;
import java.awt.Frame;

public class NewSymbolDialog extends SimpleOkCancelDialog
{

	private static final int DIALOG_PREF_SIZE_WIDTH = 300;
	private static final int DIALOG_PREF_SIZE_HEIGHT = 220;
	private static final int GRID_SCALE_SYM_DIALOG_SIZE_WIDTH = 350;
	private static final int GRID_SCALE_SYM_DIALOG_SIZE_HEIGHT = 315;
	private static final int GRID_SCALE_NONRESIZABLE_SYM_DIALOG_SIZE_HEIGHT = 250;
	@NotNull private final IAbstractLibrary m_library;
	private GridScaleSettings m_gridScaleSettings;

	NewSymbolDialog(Frame frame, String title, @NotNull IAbstractLibrary library,
			@NotNull SymbolTypeEnum symbolType)
	{
		super(frame, title);
		m_library = library;
		m_gridScaleSettings = new GridScaleSettings(this, m_library.getGrid().getRealMapping(),
				m_library.getGrid(), m_library.getSymbolScaleType(),
				symbolType.isResizeable() && m_library.getResizable(), symbolType);
		pack();
		m_gridScaleSettings.registerValidtyListener(getOkListener());
		setSymbolType(symbolType);
	}

	public GridScaleSettings getGridSettings()
	{
		return m_gridScaleSettings;
	}

	public void setSymbolType(@NotNull SymbolTypeEnum symbolType)
	{
		getGridSettings().getPanel().setVisible(symbolType.isSymbolScaleSupported());
		getGridSettings().updateResizeablility(symbolType, symbolType.isResizeable() && m_library.getResizable());
		adjustSize(symbolType);
	}

	private void adjustSize(@NotNull SymbolTypeEnum symbolType)
	{
		Dimension newSize;
		if (symbolType.isSymbolScaleSupported()) {
			if (symbolType.isResizeable()) {
				newSize = new Dimension(GRID_SCALE_SYM_DIALOG_SIZE_WIDTH, GRID_SCALE_SYM_DIALOG_SIZE_HEIGHT);
			}
			else {
				newSize =
						new Dimension(GRID_SCALE_SYM_DIALOG_SIZE_WIDTH, GRID_SCALE_NONRESIZABLE_SYM_DIALOG_SIZE_HEIGHT);
			}
		}
		else {
			newSize = new Dimension(DIALOG_PREF_SIZE_WIDTH, DIALOG_PREF_SIZE_HEIGHT);
		}
		setMinimumSize(newSize);
		setSize(newSize);
	}
}
