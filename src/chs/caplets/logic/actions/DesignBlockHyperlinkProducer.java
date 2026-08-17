/*
 * Copyright 2005-2013 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.helpers.ILogicHyperlink;
import chs.caf.caplet.helpers.ILogicHyperlinkProducer;
import chs.cof.logical.IDesign;
import chs.cof.project.IProject;
import chs.cof.project.buildlist.IBuildList;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.dataservices.DesignBlockUsageInfo;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import chs.utility.logic.LogicUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DesignBlockHyperlinkProducer implements ILogicHyperlinkProducer
{

	private IDesign m_Design;
	private Set<DesignBlockUsageInfo> m_usages = new HashSet<DesignBlockUsageInfo>();

	public DesignBlockHyperlinkProducer(@NotNull IDesign design, @NotNull Set<DesignBlockUsageInfo> usages)
	{
		m_Design = design;
		m_usages.addAll(usages);
	}

	@SuppressWarnings({"ConstantConditions", "OverlyLongMethod"})
	public List<ILogicHyperlink> createHyperlinks()
	{
		List<ILogicHyperlink> links = new ArrayList<ILogicHyperlink>(1);
		Collection<IUID> designsToLoad = IUIDObject.Statics.getUIDs(m_usages);
		m_Design.getProject().getDesignMgr().getDesigns(designsToLoad);
		for (DesignBlockUsageInfo info : m_usages) {
			if (info.getDesign() != null) {
				addLink(links, new DesignBlockHyperlink(m_Design, info));
			}
		}
		return links;
	}

	private boolean addLink(List<ILogicHyperlink> links, ILogicHyperlink link)
	{
		if (link != null && link.getConfidence() > 0.0) {
			return links.add(link);
		}
		else {
			return false;
		}
	}

	@Override public boolean hasValidSelection()
	{
		return m_Design != null;
	}

	public void handleNoLinksState()
	{
		IProject project = m_Design.getProject();
		IBuildList activeBL =
				LogicUtils.getApplicableActiveBuildListForBlockAssociation(project, m_Design.getDesignType());
		String msgKey = "CrossLinkAction.NoLinksFound.Project.text";
		if (activeBL != null) {
			msgKey = "CrossLinkAction.NoLinksFound.ActiveBuildList.text";
		}
		String msg = ResourceMgr.getString(CrossLinkAction.class, msgKey, m_Design.getName());

		String title = ResourceMgr.getString(CrossLinkAction.class, "CrossLinkAction.NoLinksFound.Title.text");
		String heading = ResourceMgr.getString(CrossLinkAction.class, "CrossLinkAction.NoLinksFound.Heading.text");
		Frame parentDialogFrame = getParentDialogFrame();
		showWarningMessage(msg, title, heading, parentDialogFrame);
	}

	protected void showWarningMessage(String msg, String title, String heading, Frame parentDialogFrame)
	{
		MessageHelper.showWarningMessage(parentDialogFrame, title, heading, msg);
	}

	public Frame getParentDialogFrame()
	{
		return CAFUtils.getInstance().getWindowMgr().getDialogFrame();
	}

	public String getViewRelatedDialogTitle()
	{
		return ResourceMgr.getString(CrossLinkAction.class, "CrossLinkAction.Dialog.Title.text",
				m_Design.getName());
	}
}
