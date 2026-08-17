/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.cof.draw.IGfxObject;
import chs.cof.drawplus.IDiagramAttributeText;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.cable.IInternalLink;
import chs.cof.logical.cable.IInternalPin;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IGenericSchemPin;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemInternalLink;
import chs.cof.parts.ILibraryCavity;
import chs.cof.parts.ILibraryCavityContainer;
import chs.cof.symbol.IBlock;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolDef;
import chs.common.ISymbolledPin;
import chs.common.IUID;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.utility.SymbolUtils;
import chs.view.assist.IPinInfo;
import chs.view.assist.PinInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adds an existing connectivity pinlist to a Logic diagram as a symbolled instance
 */
public class AddSymbolledPinListAction extends AddInstanceAction
{

	private IPinList pinlist;
	private ISymbolDef symDef;
	private Map<IUID, ISymbolledPin> symbolledPinMap;

	public AddSymbolledPinListAction(ICapletController controller, IPinList pinlist, ISymbolDef symDef, IBlock block,
			boolean reference)
	{
		this(controller, pinlist, symDef, block, null, reference);
	}

	public AddSymbolledPinListAction(ICapletController controller, IPinList pinlist, ISymbolDef symDef, IBlock block,
			Map<IUID, ISymbolledPin> symbolledPinMap, boolean reference)
	{
		super(controller);
		this.pinlist = pinlist;
		this.symDef = symDef;
		this.block = block;
		this.symbolledPinMap = symbolledPinMap;
		this.reference = reference;
	}

	public void setPinList(chs.cof.logical.schem.IPinList pl)
	{
		m_pinlist = pl;
	}

	/**
	 * Overridden here to return the symbol specified on construction of this sub-action
	 *
	 * @return The symbol specified on construction of this sub-action
	 */
	@Override protected IStamp acquireSymbol()
	{
		return symDef;
	}

	/**
	 * Overridden here clean up fields after the terminate
	 */
	@Override public boolean onTerminate(boolean successful)
	{
		boolean result = super.onTerminate(successful);
		pinlist = null;
		symDef = null;
		block = null;
		return result;
	}

	/**
	 * Overridden here to use the existing connectivity of the pinlist, rather than the new connectivity created at
	 * activation time.  We must override here because some changes need to be done during the terminate or else we get
	 * Undo problems.
	 */
	protected boolean addInstance()
	{
		if (m_pinlist != null && pinlist != null) {
			// the user has placed the instance
			// now we must replace the temporary connectivity created during activation with the existing connectivity
			IPinList tempConn = m_pinlist.getConnectivity();
			m_pinlist.setConnectivity(pinlist);
			SymbolUtils.reassignInternalLinksAndProperties(m_pinlist, pinlist, symbolledPinMap);
			//(SP1206)dts0100855341 Root Cause Defect: VALIDATION FAILURE: InternalLink - LINK1 start pin should not be null  (dts0100854298)
			//Delete the transient connectivity object here itself. If there are any problems, they'll be uncovered here itself.
			tempConn.delete();
		}

		// now we just throw away the replicated connectivity pinlist - it seems that we don't have to clear it up here?

		return super.addInstance();
	}

	@Nullable private IGenericPin findConnectivityPin(IGenericSchemPin pin)
	{
		assert pinlist != null;
		IGenericPin symbolPin = pin.getConnectivity();
		assert symbolPin.getLogicDesign() == null;
		IUID ref = symbolPin.getReference();
		assert ref != null;

		IGenericPin existingCablePin = null;
		if (pinlist != null) {
			if (symbolledPinMap != null && symbolledPinMap.containsKey(ref)) {
				existingCablePin = (IGenericPin) symbolledPinMap.get(ref);
				if (existingCablePin.getOwner() == pinlist) {
					return existingCablePin;
				}
			}
			for (IGenericPin apin : pinlist.getGenericPins()) {
				if (apin != null && apin.getReference() == ref) {
					existingCablePin = apin;
					break;
				}
			}
		}

		return existingCablePin;
	}

	@Override protected IGfxObject updateTransients()
	{

		return updateTransients(false);
	}

	protected IGfxObject updateTransients(boolean updateTracesOnly)
	{
		if (!updateTracesOnly) {
			super.updateTransients();
		}
		m_dynamics.removeAllTransientGfx();
		if (m_placementObject != null) {
			m_dynamics.addTransientGfx(m_placementObject);
		}
		if (m_placementObject instanceof chs.cof.logical.schem.IPinList && !reference) {
			final chs.cof.logical.schem.IPinList schemPinList = (chs.cof.logical.schem.IPinList) m_placementObject;
			IPinInfo pinInfo = new PinInfo(schemPinList)
			{
				@Nullable @Override protected IAbstractPin getCablePin(@NotNull IAbstractSchemPin schemPin, @NotNull
				String pinName)
				{
					if (!(schemPin instanceof IPin)) {
						return null;
					}
					IPin normalSchemPin = (IPin) schemPin;
					if (normalSchemPin.isReference()) {
						return null;
					}
					IGenericPin genericPin = findConnectivityPin(normalSchemPin);
					if (genericPin instanceof IAbstractPin && pinName.equalsIgnoreCase(genericPin.getName())) {
						return (IAbstractPin) genericPin;
					}
					return null;
				}
			};

			List<String> pinNames = new ArrayList<>(schemPinList.getPins().size());
			for (IPin schemPin : schemPinList.getPins()) {
				IGenericPin genericPin = findConnectivityPin(schemPin);
				if (genericPin instanceof IAbstractPin) {
					pinNames.add(genericPin.getName());
				}
			}
			List<IDynamicGfx> dynamicGfxs = new ArrayList<>();
			ISchemDiagram diagram = getModel().getDiagram();
			ObjectConnectionsGetter
					.createTransientGraphics(pinNames, diagram, pinInfo, m_dynamics, dynamicGfxs);

			// Show transient graphics for connectorpins connectivity
			TransientMatePinInfo matedPinInfo = new TransientMatePinInfo(schemPinList);
			ObjectConnectionsGetter
					.createTransientGraphics(pinNames, diagram, matedPinInfo, m_dynamics,
							dynamicGfxs);
		}
		return m_placementObject;
	}

	private class TransientMatePinInfo extends PinInfo
	{

		TransientMatePinInfo(@NotNull chs.cof.logical.schem.IPinList schemDevice)
		{
			super(schemDevice);
		}

		@Nullable @Override protected IAbstractPin getCablePin(@NotNull IAbstractSchemPin schemPin, @NotNull
		String pinName)
		{
			if (!(schemPin instanceof IPin)) {
				return null;
			}
			IPin normalSchemPin = (IPin) schemPin;
			if (normalSchemPin.isReference()) {
				return null;
			}
			IGenericPin genericPin = findConnectivityPin(normalSchemPin);
			if (genericPin instanceof IAbstractPin && pinName.equalsIgnoreCase(genericPin.getName())) {
				for (IPinList connectorPinList : ((IAbstractPin) genericPin).getConnectedPinLists()) {
					IAbstractPin connectorPin = ((IAbstractPin) genericPin).getConnectedPin(connectorPinList);
					if (connectorPin != null) {
						return connectorPin;
					}
				}
			}
			return null;
		}
	}

	@Override public void mouseMoved(MouseEvent e)
	{
		super.mouseMoved(e);
		updateTransients(true);
	}
}
