package chs.caplets.logic.actions;

import chs.cof.logical.cable.IPinList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.geom.Point2D;

/**
 * @author chandras on 03-03-2019.
 */
public class StackPinArgs extends AbstractAddPinArgs
{

	private IPinList m_cablePinList;

	public StackPinArgs(@NotNull Point2D loc, @NotNull IPinList cablePinList)
	{
		super(loc);
		m_cablePinList = cablePinList;
	}

	@Nullable @Override public IPinList getCablePinlist()
	{
		return m_cablePinList;
	}
}
