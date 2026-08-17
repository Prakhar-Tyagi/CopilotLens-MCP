/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic.actions.serviceDocumentation.offPage.mateGeneration;

import chs.caplets.logic.actions.serviceDocumentation.offPage.FetchOffPageContentHelper;
import chs.caplets.logic.commands.BulkAutoShareIntoCmd;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPinList;
import chs.common.IUIDObject;
import chs.utility.IMessageCollectorAndReporter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Uses {@link BulkAutoShareIntoCmd} to share into the replicated pin list
 */
class ShareIntoExecutor implements IShareIntoExecutor
{

	protected ISchemDiagram m_activeDiagram;
	protected IMessageCollectorAndReporter m_messageReporter;

	ShareIntoExecutor(ISchemDiagram activeDiagram, IMessageCollectorAndReporter messageReporter)
	{
		m_activeDiagram = activeDiagram;
		m_messageReporter = messageReporter;
	}

	public void doShareInto(IPinList replicatedPinList, @NotNull IPinList cablePinList)
	{
		ISharedPinList sharedPinList = cablePinList.getSharedPinList();
		assert sharedPinList != null;
		doShareInto(sharedPinList, replicatedPinList, cablePinList);
	}

	private void doShareInto(@NotNull ISharedObject sharedPinList, ILogicObject replicatedPinList,
			ILogicObject cablePinList)
	{
		Map<IUIDObject, ISharedObject> objectsToBeSharedInto = new HashMap<>();
		objectsToBeSharedInto.put(replicatedPinList, sharedPinList);
		ILogicDesign design = m_activeDiagram.getDesign();
		assert design != null;
		Map<ILogicObject, ILogicObject> newVsOld = new HashMap<>();
		newVsOld.put(replicatedPinList, cablePinList);
		final Map<Object, Object> contextMap = Collections.unmodifiableMap(newVsOld);
		IMessageCollectorAndReporter reporter = FetchOffPageContentHelper.getReporter(m_messageReporter, contextMap);
		new BulkAutoShareIntoCmd(objectsToBeSharedInto, design, m_activeDiagram, reporter).execute();
	}
}
