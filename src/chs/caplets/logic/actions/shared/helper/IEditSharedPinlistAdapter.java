package chs.caplets.logic.actions.shared.helper;

import chs.caplets.logic.actions.shared.EditSharedPinListModel;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.utility.helpers.revisioning.ValidationObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface IEditSharedPinlistAdapter
{

	void initSelectSharedComponent(@NotNull EditSharedPinListModel esplModel, @NotNull ILogicDesign design,
			boolean fromSymbol);

	void initEditSymbolComponent(@NotNull EditSharedPinListModel esplModel);

	void initMapperComponent(@NotNull EditSharedPinListModel esplModel, @NotNull ILogicDesign design);

	void initReuseComponent(@NotNull EditSharedPinListModel esplModel, @NotNull ILogicDesign design);

	void initAnalysisComponent(@NotNull ISharedPinList spl, @NotNull ILogicDesign design);

	void initModularComponent(@NotNull ISharedPinList spl);

	boolean hasSelectSharedComponent();

	boolean hasEditSymbolComponent();

	boolean hasMapperComponent();

	boolean hasReuseComponent();

	boolean hasAnalysisComponent();

	boolean isModularClientModified();

	boolean hasAnalysisComponentChanged();

	@NotNull ValidationObject getModularErrors();

	void updateAnalysisPinMap(@Nullable Map<ISharedPin, String> sharedPinAndTransientNameMap);

	boolean isBackshellCompatible(@NotNull ISharedPinList spl);

	boolean canMakePinsReserved();

	void initSharedDomainComponent(@NotNull ISharedPinList spl);

	boolean hasSharedDomainChanged();
}
