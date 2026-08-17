package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelection;
import chs.cof.COFTypeEnum;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.shared.ISharedConductor;
import chs.common.DesignAbstractionType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: nagamani Date: Jul 15, 2010 Time: 3:09:58 PM To change this template use File |
 * Settings | File Templates.
 */
public class AddLibraryInnercoreNetAction extends AbstractAddLibraryWireAction
{

	public AddLibraryInnercoreNetAction(ICapletController controller, ISpecialSelection libSelectMgr)
	{
		super(controller, libSelectMgr, INetConductor.class);
	}

	public String getActionUIClass()
	{
		return AddLibraryInnercoreNetActionUI.class.getName();
	}

	@Override public boolean isEnabled()
	{
		//dts0100716240-FEAT15007: A shared muticore conductor should not be allowed to create as net in one design and as wire in another.
		boolean bEnabled = super.isEnabled();
		if (bEnabled) {
			ISharedConductor sharedCond = m_helper.getSharedConductor();
			if (sharedCond != null) {
				String type = sharedCond.getType();
				bEnabled = type == null || (type != null && type.compareToIgnoreCase("net") == 0);
			}
		}
		return bEnabled;
	}

	@Override COFTypeEnum getObjectType()
	{
		return COFTypeEnum.Net;
	}

	@Override
	@NotNull protected Class<?> snappingSource()
	{
		return INetConductor.class;
	}

	/**
	 * In "System Block" we allow only the highway creation and in "Fluid" there are only Wire instances created.
	 */
	@Override protected boolean shouldAllowInCurrentAbstraction(@NotNull DesignAbstractionType designAbstractionType)
	{
		Set<DesignAbstractionType> disallowedAbstractionTypes =
				new LinkedHashSet<>(Arrays.asList(
						DesignAbstractionType.SYTEM_BLOCK, DesignAbstractionType.FLUID));

		return !disallowedAbstractionTypes.contains(designAbstractionType);
	}
}
