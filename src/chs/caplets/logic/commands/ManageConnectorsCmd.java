/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2016-2026 Siemens
 */
package chs.caplets.logic.commands;

import chs.caplets.logic.actions.ConductorConnectionChanger;
import chs.caplets.logic.actions.ManageConnectorConnectionsInfo;
import chs.caplets.logic.actions.ManageConnectorDesignScope;
import chs.caplets.logic.actions.ui.ColumnsToBeAdded;
import chs.caplets.logic.actions.ui.IConductorConnectionChangeSavePredicate;
import chs.caplets.logic.actions.ui.ManageConnectorsDialog;
import chs.caplets.logic.actions.ui.RowData;
import chs.caplets.logic.actions.ui.TopoInlineInserterManageConnectorsDialog;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.IManageConnectorsCmd;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.project.IProject;
import chs.cof.project.buildlist.IBuildList;
import chs.cofUtils.cmd.CHSCommand;
import chs.common.IDesignDescriptor;
import chs.ctf.caf.utils.IConductorProxy;
import chs.ctf.caf.utils.IPinProxy;
import chs.images.CHSImageLoader;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utility.helpers.LogHelper;
import com.mentor.capital.javafx.table.ColumnInformation;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ManageConnectorsCmd extends CHSCommand implements IManageConnectorsCmd
{

	protected IGenericInlineConnector mInlineConnector;
	private Collection<IWireConductor> mWires;
	protected ManageConnectorDesignScope mDesignScope;

	@NotNull
	public IManageConnectorsCmd setInlineConnector(@NotNull IGenericInlineConnector inlineConnector)
	{
		mInlineConnector = inlineConnector;
		return this;
	}

	@NotNull
	@Override
	public IManageConnectorsCmd setDesigns(@NotNull Collection<ILogicDesign> associatedDesigns)
	{
		final Set<IDesignDescriptor> designDescriptors = associatedDesigns.stream()
				.map(IDesignDescriptor.class::cast)
				.collect(Collectors.toSet());
		mDesignScope = new ManageConnectorDesignScope(designDescriptors, true);
		return this;
	}

	@NotNull
	@Override public IManageConnectorsCmd setWires(@NotNull Collection<IWireConductor> wires)
	{
		mWires = wires;
		return this;
	}

	@NotNull
	@Override
	public IManageConnectorsCmd setDesignsInScope(@NotNull ISharedPinList sharedConnector,
			@NotNull Collection<ILogicDesign> associatedDesigns,
			@NotNull Collection<ILogicDesign> inlinePlacedDesigns)
	{
		IProject project = sharedConnector.getProject();
		IBuildList activeBuildList = project.getBuildListMgr().getActiveBuildList();
		if (activeBuildList != null) {
			mDesignScope = new ManageConnectorDesignScope(activeBuildList, sharedConnector, false, null);

			// Add inline connector placed designs into designs in scope which are not a part of active build list
			boolean showWarning = false;
			Collection<IDesignDescriptor> designsInScope = mDesignScope.getDesignsInScope();
			Collection<IDesignDescriptor> addedDesignsInScope = new LinkedHashSet<>();
			for (ILogicDesign inlinePlacedDesign : inlinePlacedDesigns) {
				if (!designsInScope.contains(inlinePlacedDesign)) {
					mDesignScope.addDesignToDesignsInScope(inlinePlacedDesign);
					addedDesignsInScope.add(inlinePlacedDesign);
					showWarning = true;
				}
			}
			if (showWarning) {
				LogHelper.appMsgSafe(ResourceMgr.getString(ManageConnectorsCmd.class,
						"ManageConnectorsCmd.warning.inlineplaceddesignsarenotpartofactivebuildlist"));
				for (IDesignDescriptor design : addedDesignsInScope) {
					LogHelper.appMsgSafe(design.getName() + ":" + design.getRevision());
				}
			}
		}
		else {
			mDesignScope = new ManageConnectorDesignScope(sharedConnector);
			mDesignScope.restrictDesignsInScope(associatedDesigns);
		}
		return this;
	}

	@NotNull
	@Override public IManageConnectorsCmd updateEditableDesignsInScope()
	{
		mDesignScope.makeAllDesignsInScopeEditable();
		return this;
	}

	@Override public void unlockDesigns()
	{
		if (mDesignScope != null) {
			mDesignScope.releaseDesignLocks();
		}
	}

	@Override protected boolean doExecute()
	{
		if (mInlineConnector == null) {
			return false;
		}
		final ISharedPinList sharedPinList = mInlineConnector.getSharedPinList();
		final ConductorConnectionChanger conductorConnectionChanger;
		if (sharedPinList == null) {
			conductorConnectionChanger =
					new ConductorConnectionChanger(mInlineConnector, new ConductorConnectionSavePredicate());
		}
		else {
			conductorConnectionChanger =
					new ConductorConnectionChanger(sharedPinList, new ConductorConnectionSavePredicate());
		}
		ManageConnectorsDialog okCancelDialog = createDialog(mInlineConnector, conductorConnectionChanger);
		if (showDialog(okCancelDialog)) {
			return conductorConnectionChanger.changeConnections();
		}
		return false;
	}

	protected static class ConductorConnectionSavePredicate implements IConductorConnectionChangeSavePredicate
	{

		@Override public boolean shouldSaveForeignDesigns()
		{
			return true;
		}

		@Override public boolean isCurrentDesign(IDesignDescriptor designDescriptor)
		{
			return false;
		}

		@Override public Collection<ILogicDesign> getOpenedDesignsToBeSaved()
		{
			return Collections.emptyList();
		}

		@Override public void doPostSave()
		{

		}
	}

	@NotNull protected ManageConnectorsDialog createDialog(@NotNull IGenericInlineConnector inlineConnector,
			@NotNull ConductorConnectionChanger connectionChanger)
	{
		final String namesOfMates = inlineConnector.getMatedInlines()
				.stream()
				.map(IGenericInlineConnector::getName)
				.collect(Collectors.joining(","));

		Function<ColumnsToBeAdded.ColumnsToBeAddParameters, ColumnsToBeAdded> columnsToBeAddedSupplier = parameters ->
				new ColumnsToBeAdded(parameters)
				{
					@NotNull
					protected ColumnInformation<ManageConnectorConnectionsInfo> createManageConnectorColumnForIcon()
					{
						return new InsertInlinePinTableIconColumnProvider().getColumnInformationForIcon();
					}
				};

		String dialogTitle =
				ResourceMgr.getString(ManageConnectorsCmd.class, "ManageConnectorsCmd.dialog.title",
						inlineConnector.getName(), namesOfMates);
		final ManageConnectorsDialog connectorsDialog =
				initConnectorsDialog(dialogTitle, inlineConnector, connectionChanger)
						.setRowDataSupplier(dialog -> getRowData(inlineConnector, dialog))
						.setColumnsToBeAddedSupplier(columnsToBeAddedSupplier);
		final ISharedPinList sharedPinList = mInlineConnector.getSharedPinList();
		if (sharedPinList != null) {
			connectorsDialog.setSharedInline(sharedPinList);
		}
		return connectorsDialog;
	}

	@NotNull protected ManageConnectorsDialog initConnectorsDialog(@NotNull String dialogTitle,
			@NotNull IGenericInlineConnector inlineConnector,
			@NotNull ConductorConnectionChanger connectionChanger)
	{
		return new TopoInlineInserterManageConnectorsDialog(dialogTitle, inlineConnector, connectionChanger,
				mDesignScope);
	}

	@NotNull
	protected RowData getRowData(@NotNull IGenericInlineConnector inlineConnector, ManageConnectorsDialog dialog)
	{
		final RowData rowData = new RowData(dialog)
				.setNewWireConnections(mWires)
				.setDesignsInScope(mDesignScope);
		final ISharedPinList sharedPinList = inlineConnector.getSharedPinList();
		if (sharedPinList == null) {
			rowData.setPinList(inlineConnector);
		}
		else {
			rowData.setSharedPinList(sharedPinList);
		}
		return rowData;
	}

	protected boolean showDialog(@NotNull ManageConnectorsDialog dialog)
	{
		return dialog.showDialog(true);
	}

	protected static class InsertInlinePinTableIconColumnProvider extends ColumnsToBeAdded.IconColumnProvider
	{
		@NotNull
		protected <T extends Pair<IPinProxy, IConductorProxy>> Icon getPinStateIcon(T proxyPair)
		{
			if (proxyPair instanceof ManageConnectorConnectionsInfo) {
				final ManageConnectorConnectionsInfo info =
						(ManageConnectorConnectionsInfo) proxyPair;
				if (info.isNewConnection()) {
					boolean isShared = info.getFirst().getSharedPin() != null;
					if (info.isLibraried() && isShared) {
						return CHSImageLoader.loadImageIcon("chs/images/javafx_ui/new-shared-libraried-pin-small.png");
					}
					if (info.isLibraried()) {
						return CHSImageLoader.loadImageIcon("chs/images/javafx_ui/new-libraried-pin-small.png");
					}
					if (isShared) {
						return CHSImageLoader.loadImageIcon("chs/images/javafx_ui/new-shared-pin-small.png");
					}
					return CHSImageLoader.loadImageIcon("chs/images/javafx_ui/new-pin-small.png");
				}
			}
			return super.getPinStateIcon(proxyPair);
		}
	}
}
