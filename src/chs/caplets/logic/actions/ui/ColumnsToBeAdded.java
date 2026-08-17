/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2016-2026 Siemens
 */
package chs.caplets.logic.actions.ui;

import chs.caplets.logic.actions.ConductorConnectionChanger;
import chs.caplets.logic.actions.ManageConnectorChange;
import chs.caplets.logic.actions.ManageConnectorConnectionsInfo;
import chs.caplets.logic.actions.ManageConnectorItemChangeListenerProvider;
import chs.caplets.logic.actions.ManageConnectorPinDuplicationFinder;
import chs.caplets.logic.actions.ManageConnectorPinSelections;
import chs.caplets.logic.actions.ManageConnectorTableAesthetics;
import chs.caplets.logic.actions.ManageConnectorsAction;
import chs.cof.logical.shared.ISharedPinList;
import chs.common.IDesignDescriptor;
import chs.common.attr.IAttributeTypes;
import chs.ctf.caf.utils.IConductorProxy;
import chs.ctf.caf.utils.IPinProxy;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utility.ui.PinConductorConnectionSortHelper;
import chs.utility.ui.PinTableIconColumnProvider;
import com.mentor.capital.javafx.table.ColumnInformation;
import com.mentor.capital.javafx.table.Table;
import com.mentor.capital.javafx.table.cell.IGenericTableCell;
import com.mentor.capital.javafx.table.cell.TableAutoCompleteColumnType;
import com.mentor.capital.javafx.table.cell.TableColumnType;
import com.mentor.capital.javafx.table.helpers.IControlCreator;
import com.mentor.capital.javafx.table.menu.DefaultMenuItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ColumnsToBeAdded
{

	public static class ColumnsToBeAddParameters
	{

		@NotNull private final ManageConnectorItemChangeListenerProvider mProvider;
		@NotNull private final ManageConnectorPinSelections mManageConnectorPinSelections;
		@NotNull private final ManageConnectorPinDuplicationFinder mPinDuplicationFinder;
		@NotNull private final ConductorConnectionChanger mConductorConnectionChanger;
		@NotNull private final Supplier<PinConductorConnectionSortHelper> mSortHelper;
		@Nullable private ISharedPinList mSharedPinList;
		@NotNull private Collection<ManageConnectorConnectionsInfo> mtableData;

		public ColumnsToBeAddParameters(@NotNull ManageConnectorItemChangeListenerProvider provider,
				@NotNull ManageConnectorPinSelections manageConnectorPinSelections,
				@NotNull ConductorConnectionChanger conductorConnectionChanger,
				@NotNull ManageConnectorPinDuplicationFinder pinDuplicationFinder,
				@NotNull Supplier<PinConductorConnectionSortHelper> sortHelper,
				@Nullable ISharedPinList sharedPinList,
				@NotNull Collection<ManageConnectorConnectionsInfo> tableData)
		{
			mProvider = provider;
			mManageConnectorPinSelections = manageConnectorPinSelections;

			mPinDuplicationFinder = pinDuplicationFinder;
			mConductorConnectionChanger = conductorConnectionChanger;
			mSortHelper = sortHelper;
			mSharedPinList = sharedPinList;
			mtableData = tableData;
		}

		@NotNull public ManageConnectorItemChangeListenerProvider getProvider()
		{
			return mProvider;
		}

		@NotNull public ManageConnectorPinSelections getManageConnectorPinSelections()
		{
			return mManageConnectorPinSelections;
		}

		@NotNull public ManageConnectorPinDuplicationFinder getPinDuplicationFinder()
		{
			return mPinDuplicationFinder;
		}

		@NotNull public ConductorConnectionChanger getConductorConnectionChanger()
		{
			return mConductorConnectionChanger;
		}

		@NotNull public Supplier<PinConductorConnectionSortHelper> getSortHelper()
		{
			return mSortHelper;
		}

		@Nullable public ISharedPinList getSharedPinList()
		{
			return mSharedPinList;
		}

		@NotNull public Collection<ManageConnectorConnectionsInfo> getTableData()
		{
			return mtableData;
		}
	}

	private Collection<ColumnInformation<ManageConnectorConnectionsInfo>> columnsToBeAdded;

	private ManageConnectorItemChangeListenerProvider manageConnectorListenerProvider;

	private ManageConnectorPinSelections manageConnectorPinSelections;
	private ManageConnectorPinDuplicationFinder pinDuplicationFinder;

	private ConductorConnectionChanger conductorConnectionChanger;
	private Supplier<PinConductorConnectionSortHelper> sortHelper;
	private ColumnInformation<ManageConnectorConnectionsInfo> pinNameColumn;
	private ColumnInformation<ManageConnectorConnectionsInfo> wireNameColumn;
	private ISharedPinList sharedPinList;
	@NotNull private Collection<ManageConnectorConnectionsInfo> tableData;

	public ColumnsToBeAdded(@NotNull ColumnsToBeAddParameters parameters)
	{
		manageConnectorListenerProvider = parameters.getProvider();
		manageConnectorPinSelections = parameters.getManageConnectorPinSelections();
		pinDuplicationFinder = parameters.getPinDuplicationFinder();
		conductorConnectionChanger = parameters.getConductorConnectionChanger();
		sortHelper = parameters.getSortHelper();
		sharedPinList = parameters.getSharedPinList();
		tableData = parameters.getTableData();
	}

	@NotNull
	public Collection<ColumnInformation<ManageConnectorConnectionsInfo>> getColumnsIfNotAlreadyAdded()
	{

		if (columnsToBeAdded == null) {
			columnsToBeAdded = new ArrayList<>();

			columnsToBeAdded.addAll(getDefaultColumnsInManageConnectors());
		}

		return columnsToBeAdded;
	}

	protected Collection<ColumnInformation<ManageConnectorConnectionsInfo>> getDefaultColumnsInManageConnectors()
	{

		Collection<ColumnInformation<ManageConnectorConnectionsInfo>> columns = new ArrayList<>();
		columns.add(createManageConnectorColumnForIcon());
		columns.add(createManageConnectorColumnForPinName());
		columns.add(createManageConnectorColumnForWire());
		return columns;
	}

	protected void registerListeners(Table<ManageConnectorConnectionsInfo> givenTable)
	{
		manageConnectorListenerProvider.prepareRowAndColumnUpdate(pinNameColumn, givenTable);
	}

	protected ColumnInformation<ManageConnectorConnectionsInfo> createManageConnectorColumnForIcon()
	{
		ColumnInformation<ManageConnectorConnectionsInfo> pinIconColumn =
				new IconColumnProvider().getColumnInformationForIcon();
		return pinIconColumn;
	}

	public static class IconColumnProvider extends PinTableIconColumnProvider
	{

		@Override
		public boolean shouldDisplayMenuItem(DefaultMenuItem defaultMenuItemKey)
		{
			return defaultMenuItemKey != DefaultMenuItem.Select &&
					defaultMenuItemKey != DefaultMenuItem.Hide;
		}

		@Override @NotNull
		protected <T extends Pair<IPinProxy, IConductorProxy>> String getText(T row, Icon icon)
		{
			if (row instanceof ManageConnectorConnectionsInfo) {
				return ((ManageConnectorConnectionsInfo) row).isBackshellTermination()
						? ResourceMgr.getString(ManageConnectorsAction.class,
						"ManageConnectorsAction.ColumnName.Icon.BackshellTermination")
						: ResourceMgr.getString(ManageConnectorsAction.class,
						"ManageConnectorsAction.ColumnName.Icon.Pin");
			}
			return super.getText(row, icon);
		}

	}

	protected ColumnInformation<ManageConnectorConnectionsInfo> createManageConnectorColumnForPinName()
	{
		if (pinNameColumn != null) {
			return pinNameColumn;
		}

		Function<ManageConnectorConnectionsInfo, Object> readMethod =
				manageConnectorConnectionsInfo -> sortHelper.get()
						.createComparableForPin(manageConnectorConnectionsInfo.getFirst());

		BiConsumer<ManageConnectorConnectionsInfo, Object> writeMethod =
				(manageConnectorConnectionsInfo, o) -> {
					ManageConnectorChange change = manageConnectorConnectionsInfo.getChange();

					BiConsumer<IPinProxy, IPinProxy> handlePinChange = (pinProxy1, pinProxy2) ->
					{
						if (manageConnectorConnectionsInfo.getDesign() != null) {
							conductorConnectionChanger
									.addConnection(manageConnectorConnectionsInfo.getDesign(), pinProxy1,
											pinProxy2);
						}
					};

					change.applyIfChanged(handlePinChange);
				};

		Map<ManageConnectorConnectionsInfo, Collection<?>> availablePinsMap = new HashMap<>();
		populateAvailablePinsForEachRow(availablePinsMap);

		Function<IGenericTableCell<?>, Collection<?>> possibleValues = new Function<IGenericTableCell<?>, Collection<?>>()
		{

			@Override public Collection<?> apply(IGenericTableCell<?> t)
			{
				if (t != null && t.getRowItem() instanceof ManageConnectorConnectionsInfo) {
					ManageConnectorConnectionsInfo rowInfo = (ManageConnectorConnectionsInfo) t.getRowItem();

					if (availablePinsMap.containsKey(rowInfo)) {
						return availablePinsMap.get(rowInfo);
					}
				}
				return Collections.emptyList();
			}
		};

		TableColumnType tableColumnTypeForPinName = new TableAutoCompleteColumnType(
				null, possibleValues)
		{
			@Override public IControlCreator getControlCreator()
			{
				return ManageConnectorTableAesthetics
						.getControlCreatorForPinNameColumn(possibleValues, pinDuplicationFinder,
								manageConnectorPinSelections, sharedPinList);
			}
		};

		pinNameColumn =
				new ColumnInformation<ManageConnectorConnectionsInfo>(ResourceMgr.getString(
						ManageConnectorsAction.class, "ManageConnectorsAction.ColumnName.Pin"),
						ManageConnectorsDialog.PIN_NAME,
						readMethod,
						writeMethod,
						tableColumnTypeForPinName)
				{
					@Override public boolean displayDefaultMenuItem(DefaultMenuItem defaultMenuItemKey)
					{
						return defaultMenuItemKey != DefaultMenuItem.Hide;
					}
				};
		manageConnectorListenerProvider.setValidChangeValues(pinNameColumn, availablePinsMap);

		return pinNameColumn;
	}

	private void populateAvailablePinsForEachRow(
			@NotNull Map<ManageConnectorConnectionsInfo, Collection<?>> availablePinsMap)
	{
		Map<IDesignDescriptor, Collection<ManageConnectorConnectionsInfo>> designWiseTableData = new HashMap<>();
		tableData.forEach(sourceRowInfo -> {
			IDesignDescriptor srcDesign = sourceRowInfo.getDesign();
			if (srcDesign != null) {
				Collection<ManageConnectorConnectionsInfo> rowInfos = designWiseTableData.get(srcDesign);
				if (rowInfos == null) {
					rowInfos = new ArrayList<>();
					designWiseTableData.put(srcDesign, rowInfos);
				}
				rowInfos.add(sourceRowInfo);
			}
		});
		tableData.forEach(sourceRowInfo -> {
			IDesignDescriptor srcDesign = sourceRowInfo.getDesign();
			if (srcDesign == null) {
				return;
			}
			final List<Comparable<?>> sortedValidPinNames = new ArrayList<>();
			availablePinsMap.put(sourceRowInfo, sortedValidPinNames);
			if (sharedPinList != null) {
				Set<String> processedPinNames = new HashSet<>();
				if (designWiseTableData.containsKey(srcDesign)) {
					Collection<ManageConnectorConnectionsInfo> rowInfos =
							designWiseTableData.get(srcDesign);
					if (rowInfos != null) {
						rowInfos.forEach(targetRowInfo -> {
							if (!processedPinNames.contains(targetRowInfo.getOriginalValue())) {
								processedPinNames.add(targetRowInfo.getOriginalValue());
								if (sourceRowInfo.isMoveAcceptableWith(targetRowInfo) == null) {
									sortedValidPinNames.add(
											sortHelper.get().createComparableForPin(targetRowInfo.getOriginalPin()));
								}
							}
						});
					}
				}

				manageConnectorPinSelections.getUnconnectedSharedPinProxies().forEach(pinProxy -> {
					if (!processedPinNames.contains(pinProxy.getName())) {
						processedPinNames.add(pinProxy.getName());
						if (sourceRowInfo.checkPinChangeValid(sourceRowInfo.getOriginalPin(), pinProxy, srcDesign)
								.isSuccess() &&
								manageConnectorPinSelections.canUseThePinInDesign(pinProxy, srcDesign)) {
							sortedValidPinNames.add(sortHelper.get().createComparableForPin(pinProxy));
						}
					}
				});

				Collection<String> allAvailablePinsinDesign =
						manageConnectorPinSelections.getSharedPinsApplicableInDesign(srcDesign);
				allAvailablePinsinDesign.forEach(pin -> {
					if (!processedPinNames.contains(pin)) {
						processedPinNames.add(pin);
						IPinProxy pinProxy = manageConnectorPinSelections.getPinByName(pin, srcDesign);
						if (pinProxy != null &&
								sourceRowInfo.checkPinChangeValid(sourceRowInfo.getOriginalPin(), pinProxy, srcDesign)
										.isSuccess()) {
							sortedValidPinNames.add(sortHelper.get().createComparableForPin(pinProxy));
						}
					}
				});
			}
			else {
				manageConnectorPinSelections.getPins().forEach(pinProxy -> {
					if (sourceRowInfo.checkPinChangeValid(sourceRowInfo.getOriginalPin(), pinProxy, srcDesign)
							.isSuccess()) {
						sortedValidPinNames.add(sortHelper.get().createComparableForPin(pinProxy));
					}
				});
			}
			sortedValidPinNames.sort(PinConductorConnectionSortHelper.getDefaultComparator());
		});
	}

	protected ColumnInformation<ManageConnectorConnectionsInfo> createManageConnectorColumnForWire()
	{
		if (wireNameColumn != null) {
			return wireNameColumn;
		}
		Function<IConductorProxy, String> conductorNameProvider =
				conductorProxy -> conductorProxy.getValueOfAttribute(IAttributeTypes.NAME);

		Function<ManageConnectorConnectionsInfo, Object> readMethod = manageConnectorConnectionsInfo ->

		{
			PinConductorConnectionSortHelper.ConductorKeyValueProvider conductorKeyValueProvider =
					sortHelper.get().getConductorKeyValueProvider(conductorNameProvider);
			return sortHelper.get().createComparatorForWireColumn(manageConnectorConnectionsInfo,
					conductorNameProvider, conductorKeyValueProvider);
		};

		wireNameColumn =
				new ColumnInformation<ManageConnectorConnectionsInfo>(
						ResourceMgr
								.getString(ManageConnectorsAction.class, "ManageConnectorsAction.ColumnName.Wire"),
						ManageConnectorsDialog.WIRE_NAME,
						readMethod, null,
						ManageConnectorTableAesthetics.getNonEditableCellType())
				{

					@Override public boolean displayDefaultMenuItem(DefaultMenuItem defaultMenuItemKey)
					{
						return defaultMenuItemKey != DefaultMenuItem.Hide;
					}
				};

		return wireNameColumn;
	}

	protected Supplier<PinConductorConnectionSortHelper> getSortHelper()
	{
		return sortHelper;
	}
}
