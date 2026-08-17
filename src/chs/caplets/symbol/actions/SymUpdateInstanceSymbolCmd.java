/*
 * Copyright 2006-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol.actions;

import chs.cof.draw.ICommentSymbol;
import chs.cof.draw.ICompoundObject;
import chs.cof.drawplus.IAttributeText;
import chs.cof.drawplus.ICrossReferenceable;
import chs.cof.drawplus.IPropText;
import chs.cof.drawplus.IXRefPlaceholder;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.symbol.IBlock;
import chs.cof.symbol.ISheetAdapter;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolDef;
import chs.cofUtils.cmd.CommandHelper;
import chs.common.IPropertiedObject;
import chs.common.IProperty;
import chs.common.IReadOnlyNamedObject;
import chs.common.cmd.replacesymbol.InstanceAdapter;
import chs.common.cmd.replacesymbol.ReplaceInstanceSymbolParams;
import chs.common.cmd.replacesymbol.SymbolAdapter;
import chs.common.cmd.replacesymbol.UpdateInstanceSymbolCmd;
import chs.ctf.caf.utils.IGenericPinProxy;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utility.CavityProxy;
import chs.utility.helpers.PropertyHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class SymUpdateInstanceSymbolCmd - Updates a block instance on a symbol
 * <p/>
 * Responsibilites- Override UpdateInstanceSymbolCmd to support updating blocks
 * <p/>
 * Collaborators- UpdateInstanceAction
 */

public class SymUpdateInstanceSymbolCmd extends UpdateInstanceSymbolCmd
{

	@Override protected boolean replaceGraphicalAttributes(final InternalParams params,
			final List<IAttributeText> sourceAttributeTexts, final List<IAttributeText> destAttributeTexts,
			final ICompoundObject destObject, final boolean transformExistingText, final boolean translateToBlock)
	{
		if (destObject instanceof ICommentSymbol) {
			return false;
		}
		return super.replaceGraphicalAttributes(params, sourceAttributeTexts, destAttributeTexts, destObject,
				transformExistingText, translateToBlock);
	}

	private boolean mShouldCreateReferencedProps = true; // For pin properties

	public SymUpdateInstanceSymbolCmd(CommandHelper commandHelper)
	{
		super(commandHelper);
	}

	/**
	 * @see UpdateInstanceSymbolCmd#doExecute()
	 */
	@Override protected boolean doExecute()
	{
		internalToSymbolApp = true;
		boolean modified = super.doExecute();
		if (modified) {
			ISheetAdapter sheet = CommonUtils.cast(getDiagram(), ISheetAdapter.class);
			if (sheet != null) {
				ISymbolDef symbolDef = CommonUtils.cast(sheet.getSymbol(), ISymbolDef.class);
				if (symbolDef != null) {
					// getNumPins will *update* the m_nPinCount data member if the symbol is fully loaded, it should be 
					symbolDef.getNumPins();
					symbolDef.getNumInternalPins();
					symbolDef.connectInternalLinks(false);
				}
			}
		}
		return modified;
	}

	/**
	 * @see UpdateInstanceSymbolCmd#doIsValid(ReplaceInstanceSymbolParams)
	 */
	@Override protected boolean doIsValid(ReplaceInstanceSymbolParams params)
	{
		return params.isValidForUpdateInSymbol();
	}

	@Override protected boolean doReplaceInstanceSymbol(final InternalParams params)
	{
		return super.doReplaceInstanceSymbol(params, false);
	}

	protected boolean removePropTexts(final InstanceAdapter instance, final IProperty property)
	{
		Set<IPropText> combinedPropTexts = new HashSet<IPropText>();
		combinedPropTexts.addAll(instance.getAllPropTexts(property));
		combinedPropTexts.addAll(getSymbolProperties(instance, property));
		for (IPropText propText : combinedPropTexts) {
			doDeleteObject(propText);
		}
		return !combinedPropTexts.isEmpty();
	}

	private List<IPropText> getSymbolProperties(final InstanceAdapter block, final IProperty property)
	{
		ISheetAdapter sheetAdapter = CommonUtils.cast(block.getDiagram(), ISheetAdapter.class);
		if (sheetAdapter != null) {
			final IStamp stamp = sheetAdapter.getSymbol();
			return PropertyHelper.getPropTexts(stamp.getGfx(), property);
		}
		return Collections.emptyList();
	}

	/**
	 * there are no symbol dictionaries available in Capital Symbol, so this method always returns false here
	 * @param schemObject the symbol being updated
	 * @return false indicating that no symbo ldictionary should be used
	 */
	@Override protected boolean useSymbolDictionary(ICompoundObject schemObject)
	{
		return false;
	}

	/**
	 * This method is overriden from UpdateInstanceSymbolCmd, because block properties a treated differently to block pin
	 * properties. Block properties become properties of the composite symbol itself. This is consistent with block
	 * instancing.
	 *
	 * @param params InternalParams - options, instance, symbol
	 *
	 * @return boolean True iff properties were added/removed/modified
	 */
	@Override protected boolean doReplaceInstanceProperties(final InternalParams params,
			final boolean didLockSharedObject)
	{
		try {
			final ReplaceInstanceSymbolParams actualParams = params.getParams();
			ISheetAdapter sheetAdapter = CommonUtils.cast(actualParams.getDiagram(), ISheetAdapter.class);
			if (sheetAdapter != null) {
				final InstanceAdapter instance = params.getInstance();
				final SymbolAdapter symbol = params.getSymbol();
				final IStamp stamp = sheetAdapter.getSymbol();
				final String symbolName = stamp.getName();
				final IPropertiedObject destPropertiedObject = stamp.getPropertyHolder();

				final IPropertiedObject propertyParent = symbol.getPropertiedObject();
				final List<IProperty> sourceProperties = getProperties(propertyParent);
				final List<IProperty> destProperties = getProperties(destPropertiedObject);
				final ICompoundObject destSchemObject = instance.getSchemObject();

				/*{
					@SuppressWarnings({"UnnecessaryLocalVariable"})
					public void onlyInFirst(IProperty o)
					{
						// only in source, so add to dest
						if (isCopyAttrPropValuesRequired(params)) {
							IProperty sourceProperty = o;
							IProperty destProperty = addProperty(params, propertyParent, sourceProperty,
									destPropertiedObject);
							if (destProperty != null) {
								replacePropTexts(params, symbol.getSchemObject(), sourceProperty, destProperty,
										destSchemObject, destPropertiedObject, true, true);
								appendMessage("Message.PropertyAdded", destProperty.getDisplayName(),
										destProperty.getAsString(), symbolName);
								setDifferent();
							}
						}
					}

					@SuppressWarnings({"UnnecessaryLocalVariable"})
					public void onlyInSecond(IProperty o)
					{
						// Never remove a property from the composite symbol but do sync the texts
						IProperty propertyTextToRemove = o;
						if (removePropTexts(instance, propertyTextToRemove)) {
							setDifferent();
							appendMessage("Message.PropertyRemoved", propertyTextToRemove.getDisplayName(),
									propertyTextToRemove.getAsString(), instance.getName());
						}
					}

					@SuppressWarnings({"UnnecessaryLocalVariable"})
					public void inBoth(IProperty o1, IProperty o2)
					{
						if (isCopyAttrPropValuesRequired(params)) {
							IProperty sourceProperty = o1;
							IProperty destProperty = o2;
							if (replacePropTexts(params, symbol.getSchemObject(), sourceProperty, destProperty,
									destSchemObject, destPropertiedObject, true, true)) {
								setDifferent();
								// TODO: no need to show use originalPropName, props cannot be renamed now
								// TODO: test with renaming properties using project property types
								appendMessage("Message.PropertyModified", destProperty.getDisplayName(),
										destProperty.getAsString(),
										symbolName);
							}
						}
					}
				};*/

				// Update the block name in case the symbol name was changed
				IBlock block = instance.getBlock();
				if (block != null) {
					ISymbolDef symDef = getCachedSymbolDef(instance.getSymbolRef());
					if (symDef != null) {
						String blockName = block.getName();
						if (internalToSymbolApp) {
							String symName = symDef.getName();
							String substr1 = null;
							String substr2 = null;
							if (symName.length() <= blockName.length()) {
								substr1 = blockName.substring(0, symName.length());
								substr2 = blockName.substring(symName.length());
							}
							//No need to change symbol name in this case
							if (substr1 != null && substr1.equals(symName) && substr2 != null &&
									substr2.length() != 0) {

							}
							else {
								block.setName(symDef.getName());
								int nextHighestBlockCnt = ((ISymbolDef) stamp).getNextBlockCount(block);
								//If this is the only block instance corresponding to a block symbol, why suffix the
								//name with count? - dts0100748393(Updating a block of a composite symbol, renames the block incorrectly) 
								if (nextHighestBlockCnt != 2) {
									String blkName = block.getName() + Integer.toString(nextHighestBlockCnt);
									block.setName(blkName);
								}
							}
						}
						else {
							block.setName(symDef.getName()); // To update the block name if the symbol name was changed
						}
					}
				}
				// Hack - we set this flag that ensures ReplaceInstanceSymbolCmd doesn't create IReferencedProperty
				// on this composite symbol, however when we're updating pins the flag will be true, so we can
				// create IReferencedProperties and convert to/from IProperty the same as on a Logic diagram
				mShouldCreateReferencedProps = false;
				return replaceProperties(params, symbol.getSchemObject(), propertyParent, sourceProperties,
						destSchemObject, destPropertiedObject, destProperties, true, true, true,
						getPropertyComparator(instance.getBlock(), sourceProperties, destPropertiedObject));
			}
		}
		finally {
			// Probably over defensive but in case of exception ensure we can still create IReferencedProperties
			mShouldCreateReferencedProps = true;
		}
		return false;
	}

	protected Comparator<IProperty> getPropertyComparator(final IBlock block,
			final Collection<IProperty> blockProperties, IPropertiedObject destPropertiedObject)
	{
		if (shouldPrefixBlockName(block, destPropertiedObject)) {

			final Map<IProperty, IProperty> blkPropeties = new HashMap<IProperty, IProperty>(blockProperties.size());
			for (IProperty property : blockProperties) {
				blkPropeties.put(property, property);
			}

			return new Comparator<IProperty>()
			{

				@Override public int compare(IProperty o1, IProperty o2)
				{
					boolean isBlkProp1 = containsProperty(o1, blkPropeties);
					boolean isBlkProp2 = containsProperty(o2, blkPropeties);

					if (isBlkProp1 ^ isBlkProp2) {
						String name1 = isBlkProp1 ? getPropertyName(o1, block, null) : o1.getName();
						String name2 = isBlkProp2 ? getPropertyName(o2, block, null) : o2.getName();
						return name1.compareToIgnoreCase(name2);
					}
					else {
						return comparePropertyName(o1, o2);
					}
				}

				private boolean containsProperty(IProperty object, Map<IProperty, IProperty> propertyMap)
				{
					IProperty propInMap = propertyMap.get(object);
					return propInMap != null && propInMap == object;
				}
			};
		}

		return getPropertyComparator();
	}

	/**
	 * This is overriden because on symbols xref text are IXRefPlaceholder, on diagrams they are IXRefTextContainer
	 *
	 * @param params InternalParams - options, instance, symbol
	 *
	 * @return boolean True iff xref text were added/removed/modified
	 */
	@Override protected boolean doReplaceXrefText(final InternalParams params)
	{
		final InstanceAdapter instance = params.getInstance();
		if (!instance.replaceXrefTextRequired()) {
			return false;
		}
		final SymbolAdapter symbol = params.getSymbol();

		boolean modified = false;
		final ICrossReferenceable destObject =
				CommonUtils.cast(instance.getSchemObject(), ICrossReferenceable.class);
		if (destObject != null) {
			final List<IXRefPlaceholder> symbolXRefTexts =
					CollectionUtils.createList(symbol.getXRefPlaceholders().iterator());
			// Destination list must be mutable, hence make copy
			final List<IXRefPlaceholder> instanceXRefTexts =
					new ArrayList<IXRefPlaceholder>(
							CollectionUtils.createList(instance.getXRefPlaceholders().iterator()));
			modified = replaceTextLists(symbolXRefTexts, instanceXRefTexts,
					new TextReplicator<IXRefPlaceholder, IXRefPlaceholder>()
					{
						@Nullable public IXRefPlaceholder replicateAndAdd(IXRefPlaceholder sourceText)
						{
							IXRefPlaceholder replicatedText = symbol.replicateXRefPlaceholder(sourceText);
							if (replicatedText != null) {
								destObject.getCompoundObject().addObject(replicatedText);
								// NOTE: IXRefPlaceholders are undoable objects unlike IXRefTextContainer in Logic
								addCreationObject(replicatedText);
							}
							return replicatedText;
						}
					});

			assert instanceXRefTexts.size() == symbolXRefTexts.size();
			// Replace instance geometry and graphical attributes with that from symbol
			for (int i = 0; i < instanceXRefTexts.size(); ++i) {
				IXRefPlaceholder instanceXRefText = instanceXRefTexts.get(i);
				IXRefPlaceholder symbolXRefText = symbolXRefTexts.get(i);
				ReplaceInstanceSymbolParams.DEBUGlog(mLogger, "SymUpdateInstanceSymbolCmd.doReplaceXrefText : ",
						Integer.toString(i), " REPLACING ",
						ReplaceInstanceSymbolParams.DEBUGgetObjectString(instanceXRefText), " with ",
						ReplaceInstanceSymbolParams.DEBUGgetObjectString(symbolXRefText));
				modified |= symbol.replaceText(symbolXRefText, instanceXRefText, true);
				modified |= instance.transformGfx(symbol, symbolXRefText, instanceXRefText, true, true);
			}
		}
		return modified;
	}

	// TODO: check if this even needs to be overriden, can't find UI for generated names
	@Override protected boolean doRenamePin(InternalParams params, IGenericPin sourcePin,
			IGenericPin destPin)
	{
		String sourcePinName = sourcePin.getName();
		String origDestPinName = destPin.getName();
		Map<IReadOnlyNamedObject, ? extends IGenericPinProxy> mappings = params.getParams().getPinMappings();
		for (IReadOnlyNamedObject obj : mappings.keySet()) {
			if (obj instanceof CavityProxy && ((CavityProxy) obj).getPin().getConnectivity() == sourcePin) {
				sourcePinName = obj.getName();
			}
		}
		boolean modified = false;
		if (destPin.isDefaultName()) {
			// need to set the index to -1, set the name and indicate the name is overriden.
			destPin.setNameWithoutNotifyingNameMgr(sourcePinName);
			modified = true;
		}
		if (!origDestPinName.equals(sourcePinName)) {
			// Does this notify the schem text and update accordingly?
			destPin.setName(sourcePinName);
			modified = true;
		}
		return modified;
	}

	/**
	 * The base implementation of this method sets the IAttributeText to not be a placeholder, as they never are in Logic
	 * Diagrams - in symbol we preserve that flag
	 *
	 * @param replicatedText Replicated IAttributeText
	 */
	@Override protected void doPostProcessReplicatedAttributeText(IAttributeText replicatedText)
	{
		// IAttributeText doesn't automatically display the provider value,
		// update it's internal value explicitily
		replicatedText.setString(replicatedText.getProducedString());
	}

	@Override protected InternalParams createInternalParams(final ReplaceInstanceSymbolParams params)
	{
		return new InternalUpdateSymbolBlockParams(params);
	}

	class InternalUpdateSymbolBlockParams extends InternalUpdateParams
	{

		protected InternalUpdateSymbolBlockParams(final ReplaceInstanceSymbolParams params)
		{
			super(params);
		}

		@Override protected InstanceSymbolState doGetInstanceSymbolState()
		{
			if (!isSymbolFound()) {
				return InstanceSymbolState.SYMBOL_NOT_FOUND;
			}
			return InstanceSymbolState.SYMBOL_OUT_OF_DATE;
		}
	}
}
