package chs.caplets.logic.actions.serviceDocumentation.delete;

import chs.caf.caplet.selection.SelectSet;
import chs.cof.draw.ICommentSymbol;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramText;
import chs.cof.drawplus.IPropertiedGfxGroup;
import chs.cof.drawplus.IPropertiedGraphic;
import chs.cof.drawplus.IReadOnlySupplementaryObject;
import chs.cof.drawplus.table.ITableGraphic;
import chs.common.IUIDObject;
import org.jetbrains.annotations.NotNull;

public interface IDeletableSelectionHelper
{

	boolean isDeletableAsPerLogic(IUIDObject obj, @NotNull SelectSet sset);

	default boolean isSupplementaryObject(@NotNull IDiagramObject obj)
	{
		if (obj instanceof IReadOnlySupplementaryObject) {
			return ((IReadOnlySupplementaryObject) obj).isSupplementary();
		}
		return false;
	}

	default boolean isNonConnectivityDeletable(IUIDObject obj)
	{
		return (obj instanceof IPropertiedGfxGroup) || (obj instanceof ITableGraphic) ||
				(obj instanceof IPropertiedGraphic) || (obj instanceof ICommentSymbol)
				|| (obj instanceof IDiagramText);
	}
}
