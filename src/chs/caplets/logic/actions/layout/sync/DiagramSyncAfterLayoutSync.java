/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.layout.sync;

import chs.caf.cafmain.actions.CAFCommandHelper;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.DiagramSyncAfterConnectivityRefresh;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IInternalLink;
import chs.cof.logical.cable.IInternalPin;
import chs.cof.logical.schem.IGenericSchemPin;
import chs.cof.logical.schem.IInternalSchemPin;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemInternalLink;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.parts.ILibraryGraphic;
import chs.cof.symbol.ISymbolDef;
import chs.cofUtils.cmd.CommandListener;
import chs.common.ICommandListener;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.cmd.replacesymbol.ReplaceInstanceSymbolCmd;
import chs.common.cmd.replacesymbol.ReplaceInstanceSymbolOptions;
import chs.common.cmd.replacesymbol.ReplaceInstanceSymbolParams;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.CommonUtils;
import chs.utility.SymbolUtils;
import chs.utility.helpers.UtilsHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Synchronize layout diagram after connectivity sync/resync
 */
public class DiagramSyncAfterLayoutSync extends DiagramSyncAfterConnectivityRefresh
		implements IDiagramSyncAfterLayoutSync
{

	private List<SyncCablePinlist> syncCablePinlists = new ArrayList<>();

	protected DiagramSyncAfterLayoutSync(@NotNull ILogicDesign design)
	{
		super(design);
	}

	@Override public void prepare(@NotNull ISchemDiagram diagram)
	{
		if (design.getConnectivity() == null) {
			return;
		}
		doPrepare(diagram);
		for (chs.cof.logical.cable.IPinList aPinlist : design.getConnectivity().getPinLists()) {
			syncCablePinlists.add(new SyncCablePinlist(aPinlist, diagram));
		}
	}

	@Override public void run(@NotNull ISchemDiagram diagram, @NotNull Set<IUID> unusedUIDs, @NotNull
			Consumer<IDiagramObject> callback)
	{
		final Collection<IUIDObject> objectsToBeDeletedPerDiagram = new HashSet<>();
		for (SyncCablePinlist syncCablePinlist : syncCablePinlists) {
			syncCablePinlist.execute(objectsToBeDeletedPerDiagram, unusedUIDs);
		}
		reportToCallback(callback, objectsToBeDeletedPerDiagram);
		processDiagramSync(diagram, objectsToBeDeletedPerDiagram);
		doPostCleanup(diagram, callback);
	}

	private void reportToCallback(@NotNull Consumer<IDiagramObject> callback,
			@NotNull Collection<IUIDObject> objectsToBeDeletedPerDiagram)
	{
		for (IUIDObject iuidObject : objectsToBeDeletedPerDiagram) {
			if (iuidObject instanceof IDiagramObject) {
				callback.accept((IDiagramObject) iuidObject);
			}
		}
	}

	private void doPostCleanup(@NotNull ISchemDiagram diagram, @NotNull Consumer<IDiagramObject> callback)
	{
		final Collection<IUIDObject> objectsToBeDeletedPerDiagram = new HashSet<>();
		for (SyncCablePinlist syncCablePinlist : syncCablePinlists) {
			syncCablePinlist.postCleanup(objectsToBeDeletedPerDiagram);
		}
		if (!objectsToBeDeletedPerDiagram.isEmpty()) {
			reportToCallback(callback, objectsToBeDeletedPerDiagram);
			FactoryMgr.getSystemFactory().getCAFUtils().getLogicDeleteHelper()
					.delete(diagram, objectsToBeDeletedPerDiagram, false);
		}
	}

	private static class SyncCablePinlist
	{

		private List<SyncParameterizedPinlistInstance> paramPinlistInstances = new ArrayList<>();
		private List<SyncSymboledPinlistInstance> symboledPinlistInstances = new ArrayList<>();

		private SyncCablePinlist(chs.cof.logical.cable.IPinList pinList, IBaseDiagram candidateDiagramToBeSynced)
		{
			for (IDiagramObject diagramObject : candidateDiagramToBeSynced.getRepresentations(pinList.getUID())) {
				if (diagramObject instanceof IPinList) {
					if (SymbolUtils.isSymbolInstance(pinList, (IPinList) diagramObject)) {
						SyncSymboledPinlistInstance symboledInstance =
								new SyncSymboledPinlistInstance((IPinList) diagramObject, candidateDiagramToBeSynced);
						symboledPinlistInstances.add(symboledInstance);
					}
					else {
						SyncParameterizedPinlistInstance paramInstance =
								new SyncParameterizedPinlistInstance((IPinList) diagramObject,
										candidateDiagramToBeSynced);
						paramPinlistInstances.add(paramInstance);
					}
				}
			}
		}

		private void execute(@NotNull Collection<IUIDObject> objectsToBeDeletedPerDiagram,
				@NotNull Set<IUID> unusedUIDs)
		{
			for (SyncParameterizedPinlistInstance anInstance : paramPinlistInstances) {
				anInstance.execute(objectsToBeDeletedPerDiagram, unusedUIDs);
			}
			for (SyncSymboledPinlistInstance anInstance : symboledPinlistInstances) {
				anInstance.execute(objectsToBeDeletedPerDiagram, unusedUIDs);
			}
		}

		private void postCleanup(@NotNull Collection<IUIDObject> objectsToBeDeletedPerDiagram)
		{
			for (SyncSymboledPinlistInstance anInstance : symboledPinlistInstances) {
				if (!anInstance.isPinlistDeleted()) {
					anInstance.postCleanup(objectsToBeDeletedPerDiagram);
				}
			}
		}
	}

	private static class SyncDiagramPinlistInstance
	{

		protected IPinList mDiagramPinlistInstance;
		private List<SyncSchemDeviceSideConnectorInstance> dscInstances = new ArrayList<>();

		protected SyncDiagramPinlistInstance(@NotNull IPinList diagramPinlistInstance, @NotNull IBaseDiagram diagram)
		{
			mDiagramPinlistInstance = diagramPinlistInstance;
			for (IPinList schemDSC : diagramPinlistInstance.getAttachedPinListObjects(IPinList.EXCLUDE_MODULAR)) {
				if (CommonUtils.cast(schemDSC.getConnectivity(), IDeviceConnector.class) != null) {
					dscInstances.add(new SyncSchemDeviceSideConnectorInstance(schemDSC, diagram));
				}
			}
		}

		protected boolean isPinlistDeleted()
		{
			return UIDMgr.getNonDeletedObject(mDiagramPinlistInstance.getUID()) == null;
		}

		protected void execute(@NotNull Collection<IUIDObject> objectsToBeDeleted, @NotNull Set<IUID> unusedUIDs)
		{
			if (isPinlistDeleted()) {
				return;
			}
			for (SyncSchemDeviceSideConnectorInstance instance : dscInstances) {
				instance.execute(objectsToBeDeleted, unusedUIDs);
			}
		}
	}

	private static class SyncSymboledPinlistInstance extends SyncDiagramPinlistInstance
	{

		private List<SyncSchemInternalLinkInstance> internalLinkInstances = new ArrayList<>();
		private List<SyncSchemSymbolInternalPinInstance> symbolInternalPinInstances = new ArrayList<>();

		protected SyncSymboledPinlistInstance(@NotNull IPinList diagramPinlistInstance, @NotNull IBaseDiagram diagram)
		{
			super(diagramPinlistInstance, diagram);
			for (ISchemInternalLink schemLink : diagramPinlistInstance.getInternalLinks()) {
				internalLinkInstances.add(new SyncSchemInternalLinkInstance(schemLink, diagram));
			}

			for (IInternalSchemPin schemPin : diagramPinlistInstance.getInternalPins()) {
				symbolInternalPinInstances.add(new SyncSchemSymbolInternalPinInstance(schemPin, diagram));
			}
		}

		protected void execute(@NotNull Collection<IUIDObject> objectsToBeDeleted, @NotNull Set<IUID> unusedUIDs)
		{
			if (isPinlistDeleted()) {
				return;
			}
			if (!doPinsMatch()) {
				objectsToBeDeleted.add(mDiagramPinlistInstance);
				return;
			}
			super.execute(objectsToBeDeleted, unusedUIDs);
			for (SyncSchemInternalLinkInstance instance : internalLinkInstances) {
				instance.execute(objectsToBeDeleted, unusedUIDs);
			}

			for (SyncSchemSymbolInternalPinInstance instance : symbolInternalPinInstances) {
				instance.execute(objectsToBeDeleted, unusedUIDs);
			}
		}

		private void postCleanup(@NotNull Collection<IUIDObject> objectsToBeDeleted)
		{
			final boolean symbolUpdated = updateSymbol(mDiagramPinlistInstance);
			if (!symbolUpdated) {
				objectsToBeDeleted.add(mDiagramPinlistInstance);
			}
		}

		private boolean updateSymbol(@NotNull IPinList diagramPinlistInstance)
		{
			final ISchemDiagram diagram = diagramPinlistInstance.getDiagram();
			final ISymbolDef symbolDef = diagramPinlistInstance.getSymbolDef();
			if (diagram != null && symbolDef != null) {
				return replaceSymbol(diagramPinlistInstance, symbolDef, diagram);
			}
			return false;
		}

		private boolean replaceSymbol(@NotNull IPinList diagramPinlistInstance, @NotNull ISymbolDef symbolDef,
				@NotNull ISchemDiagram diagram)
		{
			ReplaceInstanceSymbolParams param =
					new ReplaceInstanceSymbolParams(diagram, diagramPinlistInstance.getUID());
			param.setSymbolRef(UtilsHelper.getCHSUtils().getSymbolFactory().constructSymbolRef(symbolDef));
			final ReplaceInstanceSymbolCmd replaceInstanceSymbolCmd =
					new ReplaceInstanceSymbolCmd(new CAFCommandHelper(), ConductorRouteAction.getInstance());
			final boolean autoMapping =
					replaceInstanceSymbolCmd.initPinMapping(param, true, ILibraryGraphic.ContextType.ELECTRICAL);
			if (!autoMapping) {
				return false;
			}
			//LOGIC-11222. LOGIC-11233:we don't want attributes/properties from symbol?
			//otherwise its overriding the includeOnBom also.
			ReplaceInstanceSymbolOptions options = new ReplaceInstanceSymbolOptions(false, false,
					false, true, false, false, false);
			param.setOptions(options);
			ICommandListener cmdListener = new CommandListener();
			replaceInstanceSymbolCmd.setCommandListener(cmdListener);
			replaceInstanceSymbolCmd.setParams(Collections.singletonList(param));
			return replaceInstanceSymbolCmd.execute();
		}

		private boolean doPinsMatch()
		{
			final chs.cof.logical.cable.IPinList cablePinlist = mDiagramPinlistInstance.getConnectivity();
			if (cablePinlist == null) {
				return false;
			}
			Set<IUID> pinsOnConnectivity = new HashSet<>();
			for (IAbstractPin abstractPin : cablePinlist.getPinCollection()) {
				pinsOnConnectivity.add(abstractPin.getUID());
			}
			Set<IUID> pinsFromSchematic = new HashSet<>();
			for (IPin pin : mDiagramPinlistInstance.getPins()) {
				final IAbstractPin cablePin = pin.getConnectivity();
				if (cablePin == null) {
					return false;
				}
				pinsFromSchematic.add(cablePin.getUID());
			}
			return pinsOnConnectivity.containsAll(pinsFromSchematic) &&
					pinsFromSchematic.containsAll(pinsOnConnectivity);
		}
	}

	private static class SyncParameterizedPinlistInstance extends SyncDiagramPinlistInstance
	{

		private List<SyncGenericPinInstance> generaicPinPinInstances = new ArrayList<>();
		private List<SyncStackPinInstance> stackPinPinInstances = new ArrayList<>();

		protected SyncParameterizedPinlistInstance(@NotNull IPinList diagramPinlistInstance,
				@NotNull IBaseDiagram diagram)
		{
			super(diagramPinlistInstance, diagram);
			for (IGenericSchemPin genericPin : diagramPinlistInstance.getGenericPins()) {
				generaicPinPinInstances.add(new SyncGenericPinInstance(genericPin, diagram));
			}

			for (ISchemStackPin genericPin : diagramPinlistInstance.getStackPins()) {
				stackPinPinInstances.add(new SyncStackPinInstance(genericPin, diagram));
			}
		}

		protected void execute(@NotNull Collection<IUIDObject> objectsToBeDeleted, @NotNull Set<IUID> unusedUIDs)
		{
			if (isPinlistDeleted()) {
				return;
			}
			super.execute(objectsToBeDeleted, unusedUIDs);
			for (SyncGenericPinInstance instance : generaicPinPinInstances) {
				instance.execute(objectsToBeDeleted, unusedUIDs);
			}
			for (SyncStackPinInstance instance : stackPinPinInstances) {
				instance.execute(objectsToBeDeleted, unusedUIDs);
			}
		}
	}

	private static class SyncBaseDiagramObjct extends RefreshBaseDiagramObjct
	{

		protected SyncBaseDiagramObjct(IBaseDiagram diagram)
		{
			super(diagram);
		}
	}

	private static class SyncGenericPinInstance extends SyncBaseDiagramObjct
	{

		private IGenericSchemPin mPin;

		protected SyncGenericPinInstance(IGenericSchemPin inputPin, IBaseDiagram diagram)
		{
			super(diagram);
			mPin = inputPin;
		}

		public void execute(@NotNull Collection<IUIDObject> objectsToBeDeleted, @NotNull Set<IUID> unusedUIDs)
		{
			super.execute(mPin, objectsToBeDeleted);
			if (!objectsToBeDeleted.contains(mPin)) {
				final IUID connectivityUID = mPin.getConnectivityUID();
				if (connectivityUID == null || unusedUIDs.contains(connectivityUID)) {
					objectsToBeDeleted.add(mPin);
				}
			}
		}
	}

	private static class SyncStackPinInstance extends SyncBaseDiagramObjct
	{

		private ISchemStackPin mStackPin;

		protected SyncStackPinInstance(ISchemStackPin stackPin, IBaseDiagram diagram)
		{
			super(diagram);
			mStackPin = stackPin;
		}

		public void execute(@NotNull Collection<IUIDObject> objectsToBeDeleted, @NotNull Set<IUID> unusedUIDs)
		{
			super.execute(mStackPin, objectsToBeDeleted);
			if (!objectsToBeDeleted.contains(mStackPin)) {
				final Set<IAbstractPin> toBeDeletedPins = new HashSet<>();
				for (IAbstractPin abstractPin : mStackPin.getAllConnectivity()) {
					if (unusedUIDs.contains(abstractPin.getUID())) {
						toBeDeletedPins.add(abstractPin);
					}
				}
				for (IAbstractPin toBeDeletedPin : toBeDeletedPins) {
					mStackPin.removePinFromStack(toBeDeletedPin);
				}
				if (mStackPin.getNumPins() == 0) {
					objectsToBeDeleted.add(mStackPin);
				}
			}
		}
	}

	private static class SyncSchemInternalLinkInstance extends SyncBaseDiagramObjct
	{

		private ISchemInternalLink mInternalLink;

		protected SyncSchemInternalLinkInstance(@NotNull ISchemInternalLink internalLink, @NotNull IBaseDiagram diagram)
		{
			super(diagram);
			mInternalLink = internalLink;
		}

		public void execute(@NotNull Collection<IUIDObject> objectsToBeDeleted, @NotNull Set<IUID> unusedUIDs)
		{
			super.execute(mInternalLink, objectsToBeDeleted);
			final IInternalLink cableInternalLink = mInternalLink.getConnectivity();
			if (objectsToBeDeleted.contains(mInternalLink) || cableInternalLink == null ||
					unusedUIDs.contains(cableInternalLink.getUID())) {
				mInternalLink.delete();
			}
		}
	}

	private static class SyncSchemDeviceSideConnectorInstance extends SyncBaseDiagramObjct
	{

		private IPinList mDSCSchem;

		protected SyncSchemDeviceSideConnectorInstance(@NotNull IPinList dscSchem, @NotNull IBaseDiagram diagram)
		{
			super(diagram);
			mDSCSchem = dscSchem;
		}

		public void execute(@NotNull Collection<IUIDObject> objectsToBeDeleted, @NotNull Set<IUID> unusedUIDs)
		{
			super.execute(mDSCSchem, objectsToBeDeleted);
			final IDeviceConnector connectivity = CommonUtils.cast(mDSCSchem.getConnectivity(), IDeviceConnector.class);
			if (objectsToBeDeleted.contains(mDSCSchem) || connectivity == null ||
					unusedUIDs.contains(connectivity.getUID())) {
				mDSCSchem.delete();
			}
		}
	}

	private static class SyncSchemSymbolInternalPinInstance extends SyncBaseDiagramObjct
	{

		private IInternalSchemPin mInternalPin;

		protected SyncSchemSymbolInternalPinInstance(@NotNull IInternalSchemPin internalPin,
				@NotNull IBaseDiagram diagram)
		{
			super(diagram);
			mInternalPin = internalPin;
		}

		public void execute(@NotNull Collection<IUIDObject> objectsToBeDeleted, @NotNull Set<IUID> unusedUIDs)
		{
			super.execute(mInternalPin, objectsToBeDeleted);
			final IInternalPin internalCablePin = mInternalPin.getConnectivity();
			if (objectsToBeDeleted.contains(mInternalPin) || internalCablePin == null ||
					unusedUIDs.contains(internalCablePin.getUID())) {
				mInternalPin.delete();
			}
		}
	}
}
