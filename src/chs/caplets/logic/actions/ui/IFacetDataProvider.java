package chs.caplets.logic.actions.ui;

import chs.cof.COFTypeEnum;
import chs.common.INamedPropertiedObject;
import chs.common.IUID;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author chandras on 01-04-2018.
 */
public interface IFacetDataProvider
{

	@NotNull INamedPropertiedObject getObject();

	@NotNull String getName();

	@NotNull IUID getUID();

	@NotNull COFTypeEnum getType();

	@NotNull Collection<IMergeFacet> getAttributes();

	@NotNull Collection<IMergeFacet> getProperties();
}
