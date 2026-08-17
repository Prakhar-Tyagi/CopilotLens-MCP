/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024-2026 Siemens
 */

package chs.ctf.caf.ui;

import chs.cof.logical.cable.IBlockDevice;
import chs.cof.logical.cable.IFunction;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.shared.ISharedFunction;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedSplice;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.symbol.ISymbolDef;
import chs.common.IMultiSymbolledPinlist;
import chs.utility.SymbolUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Class used to configure settings to be used for building panel containing options for pinList placement -
 * for pin list related dialogs, actions, etc.
 */
public class PinListPlaceOptionParams extends AbstractPlacementOptionParams
{

	/**
	 * Constructor to initialize PinListPlaceOptionParams with a given PinListTypeEnum and shared flag.
	 * This constructor is used when the type of pin list and whether it is shared are known.
	 *
	 * @param pinListType the type of pin list to be used for configuring the options.
	 * @param isShared    true if the pin list is shared.
	 */
	public PinListPlaceOptionParams(@NotNull PinListTypeEnum pinListType, boolean isShared)
	{
		if (pinListType == PinListTypeEnum.TypeSplice) {
			return;
		}
		if (pinListType == PinListTypeEnum.TypeFunction) {
			enableIndividuallyOption(true);
			enableAutoGenerateOption(true);
			enableAsGroupOption(true);
			enableLoadSharedPinInfoOption(isShared);
			return;
		}
		if (pinListType == PinListTypeEnum.TypeRingTerminal) {
			enableAsReferenceOption(true);
			enableLoadSharedPinInfoOption(isShared);
			return;
		}
		if (pinListType == PinListTypeEnum.TypeDevice || pinListType.isConnector()) {
			enableIndividuallyOption(true);
			enableAutoGenerateOption(true);
			enableAsStackOption(true);
			enableAsGroupOption(true);
			enableAsReferenceOption(true);
			enableLoadSharedPinInfoOption(isShared);
		}
	}

	/**
	 * Constructor to initialize PinListPlaceOptionParams with a given ISymbolDef.
	 * This constructor is used when the symbol definition is provided and the options need to be configured
	 * based on the type of symbol.
	 *
	 * @param symDef the symbol definition to be used for configuring the options.
	 */
	public PinListPlaceOptionParams(@NotNull ISymbolDef symDef)
	{
		if (!SymbolUtils.isFunctionSymbol(symDef) && !SymbolUtils.isGroundSymbol(symDef)) {
			enableAsReferenceOption(true);
			if (symDef.getNumBlocks() > 0){
				// For composite symbols
				enableIndividuallyOption(true);
				enableAsGroupOption(true);
				enableAsStackOption(false);
				enableAutoGenerateOption(false);
			}
		}
	}

	/**
	 * Constructor to initialize PinListPlaceOptionParams with a given IPinList and ISymbolDef.
	 * This constructor is used when both the pin list and symbol definition are provided and the options
	 * need to be configured based on the type of pin list and symbol.
	 *
	 * @param pinList the pin list to be used for configuring the options.
	 * @param symDef  the symbol definition to be used for configuring the options.
	 */
	public PinListPlaceOptionParams(@NotNull IPinList pinList, @Nullable ISymbolDef symDef)
	{
		if (pinList instanceof ISplice) {
			return;
		}
		if (pinList instanceof IBlockDevice) {
			enableIndividuallyOption(true);
			enableAutoGenerateOption(true);
			enableAsStackOption(true);
			enableAsGroupOption(true);
			enableShowUsedPinsOptionIfValid(pinList.getProject());
			return;
		}
		if (pinList instanceof IFunction) {
			enableIndividuallyOption(true);
			enableAutoGenerateOption(true);
			enableAsGroupOption(true);
			return;
		}

		if (pinList instanceof IMultiSymbolledPinlist) {
			enableIndividuallyOption(true);
			enableAutoGenerateOption(true);
			enableAsStackOption(true);
			enableAsGroupOption(true);
			enableAsReferenceOption(pinList.canHaveReferencePin());
		}
		else if (!isRingTerminal(pinList)) {
			enableIndividuallyOption(true);
			enableAsStackOption(true);
			enableAsGroupOption(true);
			enableAutoGenerateOption(pinList instanceof IGenericInlineConnector);
		}
		if (symDef == null || (!SymbolUtils.isFunctionSymbol(symDef) && !SymbolUtils.isGroundSymbol(symDef))) {
			enableAsReferenceOption(true);
		}
	}

	/**
	 * Constructor to initialize PinListPlaceOptionParams with a given ISharedPinList.
	 * This constructor is used when the shared pin list is provided and the options need to be configured
	 * based on the type of shared pin list.
	 *
	 * @param sharedPinList the shared pin list to be used for configuring the options.
	 */
	public PinListPlaceOptionParams(@NotNull ISharedPinList sharedPinList)
	{
		enableLoadSharedPinInfoOptionIfValid(sharedPinList);
		if (sharedPinList instanceof ISharedSplice) {
			return;
		}
		if (sharedPinList instanceof ISharedFunction) {
			enableIndividuallyOption(true);
			enableAutoGenerateOption(true);
			enableAsGroupOption(true);
			return;
		}
		if (isSharedRingTerminal(sharedPinList)) {
			enableAsReferenceOption(true);
			return;
		}
		enableIndividuallyOption(true);
		enableAutoGenerateOption(true);
		enableAsGroupOption(true);
		enableAsStackOption(true);
		enableAsReferenceOption(true);
	}
}
