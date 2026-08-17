/*
 * Copyright 2006-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.CAFUtils;
import chs.caf.caplet.action.IActionEnum;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IFunction;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UnsharePinListActionHelper implements IShareActionHelper
{

	private GenericPinListUnshareHelper m_helper = null;
	protected ILogicDesign design;
	private IUID m_sharedCablePinListUid;

	public UnsharePinListActionHelper(ILogicDesign theDesign)
	{
		design = theDesign;
	}

	@Override
	@NotNull public IActionEnum setup(@NotNull BaseShareActionOperands operands, @Nullable String dialogTitle,
			@Nullable ISchemDiagram diagram)
	{
		CAFUtils.getInstance().getOutputWindow().clearPane("Unshare Action");
		m_helper = createUnshareHelper(operands.getCablePinList(), diagram);
		if (operands.getCablePinList() != null) {
			m_sharedCablePinListUid = operands.getCablePinList().getSharedObjectUID();
		}
		return m_helper.setup(operands, diagram);
	}

	public GenericPinListUnshareHelper createUnshareHelper(@Nullable IPinList cablePinList,
			@Nullable ISchemDiagram diagram)
	{
		if (cablePinList instanceof IConnector) {
			if (isInlineConnector(cablePinList)) {
				return new InlineConnectorUnshareHelper(design, diagram);
			}
			else if (isModularConnector((IConnector) cablePinList)) {
				return new ModularConnectorUnshareHelper(design, diagram);
			}
			else {
				return new ConnectorUnshareHelper(design, diagram);
			}
		}
		else {
			if (cablePinList instanceof IFunction) {
				return new FunctionUnshareHelper(design, diagram);
			}
			return new DeviceUnshareHelper(design, diagram);
		}
	}

	protected boolean isInlineConnector(IPinList cablePinList)
	{
		return cablePinList instanceof IGenericInlineConnector;
	}

	protected boolean isModularConnector(IConnector connector)
	{
		return connector.getNumPosition() != 0 || connector.getOccupiedPosition() != null;
	}

	public boolean doEdit()
	{
		return m_helper.doEdit();
	}

	@Override public void cleanup()
	{
		if (m_helper != null) {
			m_helper.cleanup();
		}
	}

	@Override public boolean isNewSharedObject()
	{
		return m_helper.isNewSharedObject();
	}

	public GenericPinListUnshareHelper getUnshareHelper()
	{
		return m_helper;
	}

	@Override @Nullable public IUID getSharedObjectUID()
	{
		return m_sharedCablePinListUid;
	}

	@Override public boolean isShareInto()
	{
		return false;
	}
}
