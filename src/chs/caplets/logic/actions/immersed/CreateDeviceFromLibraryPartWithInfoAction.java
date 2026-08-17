/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.actions.immersed;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caplets.logic.actions.AddDeviceFromLibraryPartAction;
import chs.cof.library.PSDLibraryPartSelection;
import chs.cof.parts.ILibraryGraphic;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.LibraryBooleanType;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.subsystem.immersed.impl.object.devicemodel.CreateDeviceInfo;
import chs.system.FactoryMgr;
import org.jetbrains.annotations.NotNull;

import java.awt.event.ActionEvent;
import java.util.Set;

/**
 * This class is responsible for creating a device from a library part.
 * It extends the AddDeviceFromLibraryPartAction class to provide additional
 * functionality for handling device creation with specific library objects,
 * graphics and set of attributes and properties.
 */
public class CreateDeviceFromLibraryPartWithInfoAction extends AddDeviceFromLibraryPartAction
{

	@NotNull private CreateDeviceInfo m_deviceInfo;
	@NotNull private ILibraryObject m_libraryObject;
	@NotNull private Set<ILibraryGraphic> m_libraryGraphics;
	@NotNull private PSDLibraryPartSelection m_libraryPartSelection = new PSDLibraryPartSelection();

	public CreateDeviceFromLibraryPartWithInfoAction(@NotNull ICapletController controller, @NotNull ILibraryObject libraryObject,
			@NotNull Set<ILibraryGraphic> libraryGraphics)
	{
		super(controller);
		m_libraryObject = libraryObject;
		m_libraryGraphics = libraryGraphics;
	}

	/**
	 * Setter to "feed" the action with DeviceInfo
	 * Sets the device information for this action.
	 * This method provides the required inputs for device creation.
	 *
	 * @param deviceInfo The CreateDeviceInfo object containing details for device creation.
	 */
	public void setDeviceInfo(CreateDeviceInfo deviceInfo)
	{
		m_deviceInfo = deviceInfo;
	}

	@Override @NotNull protected PSDLibraryPartSelection pickLibraryPart()
	{
		for (ILibraryGraphic libraryGraphic : m_libraryGraphics) {
			if (libraryGraphic.getDefaultSymbol() == LibraryBooleanType.TRUE) {
				m_libraryPartSelection.setSelectedSymbol(libraryGraphic);
				FactoryMgr.getSystemFactory().getCHSSystem().getSymbolLibraryMgr().refreshLibraries();
			}
		}
		m_libraryPartSelection.setSelectedLibraryObject(m_libraryObject);

		return m_libraryPartSelection;
	}

	@Override protected IActionEnum activateAddWithoutSymbol(ActionEvent e, ILibraryPartSelection libraryPart)
	{
		AddParameterizedDeviceFromLibraryPartWithInfoAction action =
				new AddParameterizedDeviceFromLibraryPartWithInfoAction(getController(), libraryPart);
		action.setDeviceInfo(m_deviceInfo);
		subAction = action;
		return action.onActivate(e);
	}

	@Override protected IActionEnum activateAddWithSymbol(ActionEvent e, ILibraryPartSelection libraryPart)
	{
		AddLibraryPartWithSymbolAndInfoAction action =
				new AddLibraryPartWithSymbolAndInfoAction(getController(), libraryPart);
		action.setDeviceInfo(m_deviceInfo);
		subAction = action;
		return action.onActivate(e);
	}

	@Override public boolean isValid()
	{
		return true;
	}
}