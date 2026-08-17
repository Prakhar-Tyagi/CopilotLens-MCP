package chs.caplets.symbol;

import chs.cof.symbol.IAbstractLibrary;
import chs.cof.symbol.SymbolTypeEnum;
import chs.utilities.ResourceMgr;

import java.util.HashMap;
import java.util.Map;

public class XSCSymbolCreationParameterHolder extends Lifecycle.SymbolCreationParameterHolder
{

	XSCSymbolCreationParameterHolder(IAbstractLibrary symbolLib)
	{
		super(symbolLib);
	}

	@Override
	protected Map<SymbolTypeEnum, String> getSymbolTypeEnumStringMap()
	{
		final Map<SymbolTypeEnum, String> typeMap = new HashMap<SymbolTypeEnum, String>();

		/*if (CapabilityHelper.supports(SupportedFeatureInfo.Feature.HARNESS_BACKSHELL)) {
			typeMap.put(SymbolTypeEnum.BACKSHELL,
					ResourceMgr.getString(Lifecycle.class, "Lifecycle.symbolType.backshell"));
		}
		*/
		typeMap.put(SymbolTypeEnum.COMMENT, ResourceMgr.getString(Lifecycle.class, "Lifecycle.symbolType.comment"));

		return typeMap;
	}

}
