/*
 * Copyright 2004-2013 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ConfirmSaveDialog;
import chs.caf.caplet.helpers.ILogicHyperlink;
import chs.caf.caplet.helpers.ILogicHyperlinkProducer;
import chs.caplets.helpers.HyperlinkList;
import chs.cof.project.IProject;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

import java.awt.Frame;
import java.util.List;

public abstract class CrossLinkHelper
{

	private ILogicHyperlink m_selectedHyperlink;
	private ILogicHyperlinkProducer hyperlinkProducer;
	private IProject m_project;

	protected CrossLinkHelper(IProject project, ILogicHyperlinkProducer hyperlinkProducerT)
	{
		m_project = project;
		hyperlinkProducer = hyperlinkProducerT;
	}

	protected abstract String getViewRelatedDialogTitle();

	protected abstract Frame getViewRelatedDialogParent();

	protected abstract String getConfirmDialogTitle();

	protected String getConfirmDialogMessage()
	{
		return ResourceMgr.getString(CrossLinkAction.class, "CrossLinkAction.Confirm.Message.text");
	}

	public IActionEnum onActivate()
	{

		if (hyperlinkProducer.hasValidSelection()) {

			//766561:Make sure we are not working on deleted designs
			m_project.refreshDesignListAndFolderMgr();

			List<ILogicHyperlink> links = hyperlinkProducer.createHyperlinks();

			if (links == null || links.isEmpty()) {
				hyperlinkProducer.handleNoLinksState();
				return IActionEnum.eCanceled;
			}

			if (links.size() == 1) {
				// If there's only one cross-reference, skip the dialog and go with the only choice.
				m_selectedHyperlink = links.get(0);
			}
			else {
				HyperlinkList linkList = new HyperlinkList(links);
				final ViewRelatedDialog dialog = getViewRelatedDialog(linkList);
				dialog.setVisible(true);

				if (dialog.isCancelled()) {
					return IActionEnum.eCanceled;
				}

				if (linkList.getSelectedValue() instanceof ILogicHyperlink) {
					m_selectedHyperlink = (ILogicHyperlink) linkList.getSelectedValue();
				}
			}

			boolean isCancelled = showConfirmSaveDialog();

			if (m_selectedHyperlink != null &&
					(m_selectedHyperlink.getCost() < ILogicHyperlink.HIGH_COST || !isCancelled)) {
				return IActionEnum.eCompleted;
			}
		}
		return IActionEnum.eCanceled;
	}

	protected boolean showConfirmSaveDialog()
	{
		return (new ConfirmSaveDialog(getClass().getName(), getConfirmDialogTitle(), getConfirmDialogMessage(), false))
				.userCanceled();
	}

	protected ViewRelatedDialog getViewRelatedDialog(@NotNull HyperlinkList linkList)
	{
		return getViewRelatedDialog(linkList, getViewRelatedDialogParent(), getViewRelatedDialogTitle());
	}

	protected ViewRelatedDialog getViewRelatedDialog(@NotNull HyperlinkList hyperlinkList, Frame frame, String title)
	{
		return new ViewRelatedDialog(hyperlinkList, frame, title);
	}

	protected boolean handleSelection(@NotNull ILogicHyperlink selectedHyperlink)
	{
		return selectedHyperlink.handleSelection();
	}

	public boolean onTerminate(boolean successful)
	{
		if (successful && m_selectedHyperlink != null) {
			return handleSelection(m_selectedHyperlink);
		}
		return false;
	}
}
