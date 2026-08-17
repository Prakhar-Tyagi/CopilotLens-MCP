/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025-2026 Siemens
 */
package chs.caplets.logic.actions.inlineassist;

import chs.caf.cafmain.actions.CAFCommandHelper;
import chs.caf.caplet.ICapletController;
import chs.caplets.logic.commands.AssociateLibraryPartCommand;
import chs.caplets.topology.inlineassist.IDiagramConnectorDataCalculator;
import chs.caplets.topology.inlineassist.IInlineAssistConductor;
import chs.caplets.topology.inlineassist.ILongestSegmentFinder;
import chs.caplets.topology.inlineassist.ILongestSegmentFinderClient;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.parts.ILibraryBaseConnector;
import chs.common.IReadOnlyNamedObject;
import chs.ctf.caf.utils.IPinMapperHelper;
import chs.ctf.caf.utils.IPinProxy;
import chs.ctf.caf.utils.InlineInsertPinMapperHelper;
import chs.ctf.caf.utils.LibraryPinMapProvider;
import chs.ctf.caf.utils.LibrarySharedAllPinsPinMapProvider;
import chs.ctf.caf.utils.PinToLibraryCavityMap;
import chs.utilities.CommonUtils;
import chs.utility.topology.inlineconn.InlineShieldTerminationInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Factory for creating {@link IInlineInsertionController} instances.
 */
public class InlineInsertionControllerFactory implements IInlineInsertionControllerFactory
{

	@NotNull private Map<ICapletController, IInlineInsertionController> insertionControllerMap = new HashMap<>();

	@Override @NotNull
	public IInlineInsertionController getInlineInsertionController(@NotNull ICapletController capletController)
	{
		if (!insertionControllerMap.containsKey(capletController)) {
			insertionControllerMap.put(capletController, new InlineInsertionController(capletController));
		}
		return insertionControllerMap.get(capletController);
	}

	public void clearInsertionControllerMap()
	{
		insertionControllerMap.clear();
	}

	@NotNull @Override
	public ILongestSegmentFinder createLongestSegmentFinder(@NotNull ILongestSegmentFinderClient client,
			@NotNull Collection<IInlineAssistConductor> conductors, @NotNull Collection<IConductor> targetConductors,
			@NotNull Collection<IgnoredConductorInformation> ignoredConductors)
	{
		return new LongestSegmentFinder(client, conductors, targetConductors, ignoredConductors);
	}

	/**
	 * @see IInlineInsertionControllerFactory#assignLibraryPart(ILibraryBaseConnector, IPinList, Set)
	 */
	@Override public void assignLibraryPart(@NotNull ILibraryBaseConnector libraryConn, @NotNull IPinList graphicalConn,
			@NotNull Set<PinToLibraryCavityMap> viablePinMapping)
	{
		ISchemDiagram diagram =
				CommonUtils.cast(IBaseDiagram.Statics.getOwningDiagram(graphicalConn), ISchemDiagram.class);
		if (diagram == null) {
			return;
		}
		final chs.cof.logical.cable.IPinList logicalConnector = graphicalConn.getConnectivity();
		AssociateLibraryPartCommand cmd =
				new AssociateLibraryPartCommand(new CAFCommandHelper(), diagram, graphicalConn, libraryConn);
		cmd.setSilent(true);
		final ISharedPinList sharedPinList = logicalConnector.getSharedPinList();

		// For non shared inlines always use the mapper, for shared inline only use it on the first instance, once
		// the first instance has been processed the shared pinlist will have a library ref
		if (sharedPinList == null || sharedPinList.getLibraryRef() == null) {
			// Construct viability pin mapper if mapping is provided.
			if (!viablePinMapping.isEmpty()) {
				InlineInsertPinMapperHelper inlineInsertPinMapperHelper =
						new InlineInsertPinMapperHelper(libraryConn, logicalConnector, sharedPinList,
								viablePinMapping);
				cmd.setPinMapper(inlineInsertPinMapperHelper);
			}
			else {
				final LibraryPinMapProvider libraryPinMapProvider = sharedPinList != null ?
						new LibrarySharedAllPinsPinMapProvider(libraryConn, logicalConnector, sharedPinList) :
						new LibraryPinMapProvider(libraryConn, logicalConnector);
				cmd.setPinMapper(new IPinMapperHelper()
				{
					@Nullable @Override public Map<IReadOnlyNamedObject, IPinProxy> promptPinMapping(boolean hasNext)
					{
						return libraryPinMapProvider.generateMapping();
					}
				});
			}
		}

		if (!cmd.prepare()) {
			return;
		}
		cmd.execute();
	}

	@Override public void invokeInlineInsertManageConnections(@NotNull ISharedPinList sharedPinList,
			@NotNull Map<IAbstractPin, IPinProxy> viableMapping)
	{
		new InlineInsertManageConnections(sharedPinList, viableMapping).invoke();
	}

	@Override @NotNull
	public IDiagramConnectorDataCalculator createDiagramConnectorDataCalculator(
			@NotNull InlineShieldTerminationInfo shieldTerminationInfo)
	{
		return new DiagramConnectorDataCalculator(shieldTerminationInfo);
	}
}
