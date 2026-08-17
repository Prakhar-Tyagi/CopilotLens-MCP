/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.helpers.AbstractViewRelatedHyperlink;
import chs.caf.caplet.helpers.IHyperlink;
import chs.caf.caplet.helpers.ILogicHyperlink;
import chs.caf.helpers.GfxViewHelper;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IUID;
import chs.services.gfx.GfxView;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import chs.utility.helpers.NamedObjectComparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Defines each view related item in Logic as a hyperlink for View Related Items action
 */
public abstract class AbstractLogicHyperlink extends AbstractViewRelatedHyperlink<IDesign, ISchemDiagram>
		implements ILogicHyperlink
{

	protected AbstractLogicHyperlink(@NotNull ISchemDiagram currentDiagram)
	{
		super(currentDiagram.getProject(), currentDiagram.getDesign(), currentDiagram);
	}

	protected AbstractLogicHyperlink(@NotNull IDesign currentDesign)
	{
		super(currentDesign.getProject(), currentDesign, null);
	}

	protected abstract IUID getDesignUID();

	@Nullable protected abstract String getDesignName();

	@Override @Nullable public GfxView getView()
	{
		return GfxViewHelper.openLogicDiagram(getDiagram());
	}

	@Override public int compareTo(@NotNull IHyperlink o)
	{
		assert (AbstractLogicHyperlink.class.isAssignableFrom(o.getClass()));
		if (!AbstractLogicHyperlink.class.isAssignableFrom(o.getClass())) {
			return -1;
		}
		int myRanking = getRanking();
		int otherRanking = ((ILogicHyperlink) o).getRanking();
		if (myRanking < otherRanking) {
			return -1;
		}
		else if (myRanking > otherRanking) {
			return 1;
		}
		else {
			return NamedObjectComparator.caseInsensitiveComparator().compare(this, o);
		}
	}

	public final int getRanking()
	{

		double certainty = getConfidence();
		assert (certainty > 0.0 && certainty <= 1.0);
		if (certainty <= 0.0) {
			certainty = Double.MIN_VALUE; // This is minimum positive value
		}
		if (certainty > 1.0) {
			certainty = 1.0;
		}

		int cost = getCost();
		assert (cost >= 0 && cost <= 5);
		if (cost < 0) {
			cost = 0;
		}
		if (cost > 5) {
			cost = 5;
		}

		return (int) ((double) cost / certainty);
	}

	public int getCost()
	{
		int cost = 0;

		if (!isLinkedDiagramCurrent()) {
			cost += 1; // Not current design and diagram
		}

		if (!isFullyLoaded(getDiagramUID())) {
			cost += 2; // Need to load diagram
		}

		if (!isFullyLoaded(getDesignUID())) {
			cost += 2; // Need to load design
		}

		return cost;
	}

	private boolean isLinkedDiagramCurrent()
	{
		return m_currentDesign != null && m_currentDiagram != null &&
				m_currentDesign.getUID().isEquiv(getDesignUID()) && m_currentDiagram.getUID().isEquiv(getDiagramUID());
	}

	public ILogicDesign getDesign()
	{
		return m_project.getDesignMgr().getAbstractLogicDesign(getDesignUID());
	}

	@Nullable protected ISchemDiagram getDiagram()
	{
		return getDesign().getDiagram(getDiagramUID());
	}

	protected void showFewInvisibleItemsFoundWarning()
	{
		MessageHelper.showWarningMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
				ResourceMgr.getString(CrossLinkAction.class,
						"CrossLinkAction.InvisibleObjectsFound.Heading.text"),
				ResourceMgr.getString(CrossLinkAction.class,
						"CrossLinkAction.InvisibleObjectsFound.Message.text"));
	}

	protected void showNoVisibleItemsFoundWarning()
	{
		MessageHelper.showWarningMessage(getParentDialogFrame(),
				ResourceMgr.getString(CrossLinkAction.class,
						"CrossLinkAction.NoVisibleObjectsFound.Heading.text"),
				ResourceMgr.getString(CrossLinkAction.class,
						"CrossLinkAction.NoVisibleObjectsFound.Message.text"));
	}

	protected void showUnableToOpenDiagramWarning()
	{
		MessageHelper.showWarningMessage(getParentDialogFrame(),
				ResourceMgr.getString(CrossLinkAction.class, "CrossLinkAction.NoDiagram.Heading.text"),
				ResourceMgr.getString(CrossLinkAction.class, "CrossLinkAction.NoDiagram.Message.text"));
	}

	protected void showNoDiagramObjectsFoundWarning()
	{
		MessageHelper.showWarningMessage(getParentDialogFrame(),
				ResourceMgr.getString(CrossLinkAction.class, "CrossLinkAction.NoObjectsFound.Heading.text"),
				ResourceMgr.getString(CrossLinkAction.class, "CrossLinkAction.NoObjectsFound.Message.text"));
	}
}

