package chs.caplets.logic.updateSymbol;

import chs.cof.logical.schem.IPin;
import chs.cof.parts.ILibraryObject;
import chs.cof.symbol.ISymbolDef;
import chs.common.IPropertiedObject;
import chs.common.IProperty;
import chs.common.IPropertyIterator;
import chs.common.IReadOnlyNamedObject;
import chs.common.cmd.replacesymbol.BaseSymInstAdapter;
import chs.ctf.caf.utils.SymbolPinMapProvider;
import chs.system.FactoryMgr;
import chs.utility.SymbolUtils;
import chs.utility.helpers.LibraryHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;


abstract class SymbolInfomationUpdator<S extends IPropertiedObject, T extends IPropertiedObject>
{

	protected List<IReadOnlyNamedObject> symbolMap;
	protected S pinlist;
	private ISymbolDef m_symbolDef;

	SymbolInfomationUpdator(S pinlist, @Nullable ISymbolDef symbolDef)
	{
		this.pinlist = pinlist;
		m_symbolDef = symbolDef;
	}

	/**
	 * Given a library object that is associated with a pinlist, this function would add the library associated symbol
	 * to SPL.
	 * <p>
	 * Assumptio - the library cavities are reconciled with symbol pins & by this time, SPL names are updated with
	 * cavity information [any extra library cavities have their shared pins created. Num of shared pins can be more
	 * than cavities]
	 *
	 * @param libraryObject - the library definition that is being associated with SPL
	 */
	public void addLibraryPartAssociatedSymbol(ILibraryObject libraryObject)
	{
		ISymbolDef symDef = m_symbolDef != null ? m_symbolDef : getDefaultLibrarySymbol(libraryObject);
		if (symDef != null) {
			symDef = (ISymbolDef) symDef.getContainerLibrary().loadFully(symDef);
			initializeSymbolInfomation(libraryObject, symDef);
			// We should not copy properties from symbol, as properties already copied from library part during part assignment
			ensureSymbolAssociation(symDef, false);
			createAndUpdatePinsAndInternalPins(symDef);
			updateInternalLinks(symDef);
		}
	}

	abstract void ensureSymbolAssociation(ISymbolDef symDef, boolean copyProperties);

	abstract void updateInternalLinks(ISymbolDef symbolDef);

	@Nullable public ISymbolDef getDefaultLibrarySymbol(@Nullable ILibraryObject libObj)
	{
		return LibraryHelper.getDefaultLibrarySymbol(libObj);
	}

	abstract void updateBlockAssociation(S pinOwner, ISymbolDef symbolDef);

	protected void initializeSymbolInfomation(ILibraryObject libraryObject, ISymbolDef symbolDef)
	{
		Set<IPin> schemPins = getSymbolPins(symbolDef);
		List<BaseSymInstAdapter.SchemPinForDisplay> instancePinsForDisplay =
				BaseSymInstAdapter.SchemPinForDisplay.createPinsForDisplay(symbolDef, schemPins);
		symbolMap = SymbolPinMapProvider.getCavityToSymbolMappings(libraryObject, instancePinsForDisplay);
	}

	@NotNull protected Set<IPin> getSymbolPins(ISymbolDef symbolDef)
	{
		return SymbolUtils.collectAllSymbolPins(symbolDef);
	}

	abstract void createAndUpdatePinsAndInternalPins(ISymbolDef symbolDef);

	/**
	 * Called to copy the properties from one object to another. Properties are cloned, given new UID's and have the
	 * stability set to fixed.
	 *
	 * @param src the propertied object we are adding from
	 * @param dest the propertied object we are adding to
	 */
	public static void copyProperties(IPropertiedObject src, IPropertiedObject dest)
	{
		if (src != null && dest != null) {
			IPropertyIterator iterProps = src.getProperties();

			while (iterProps.hasNext()) {
				IProperty property = iterProps.getNext();
				if (!dest.hasProperty(property)) {
					dest.addProperty(cloneProperty(property, dest, false));
				}
			}
		}
	}

	private static IProperty cloneProperty(IProperty prop, @NotNull IPropertiedObject dest, boolean bFixed)
	{
		return FactoryMgr.getCommonFactory().constructProperty(prop, dest, bFixed);
	}
}
