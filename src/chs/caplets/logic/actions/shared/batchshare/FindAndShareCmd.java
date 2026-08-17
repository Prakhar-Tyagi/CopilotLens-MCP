/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare;

import chs.cof.logical.ILogicDesign;
import chs.cof.project.IProject;
import chs.utilities.OptionalString;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utility.helpers.LogHelper;
import chs.utility.ui.HTMLHelper;
import chs.utility.ui.HTMLTable;
import chs.utility.ui.HTMLTableRow;
import chs.utility.ui.progress.IProgress;
import chs.utility.ui.progress.ProgressGroup;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Cmd for discovering and then sharing objects across designs based on given object types and share criteria
 */
public class FindAndShareCmd extends AbstractFindAndShareCmd implements IFindAndShareCmd
{

	@NotNull private Set<IEntityShareCriteria> m_entitiesShareCriteria;
	@NotNull private IProgress m_progress;
	@NotNull private IProgress m_findProgress;
	@NotNull private IProgress m_shareProgress;
	@NotNull private IProgress m_shareIntoProgress;
	@NotNull private IProgress m_findForConductorsProgress;
	@NotNull private IProgress m_shareForConductorsProgress;
	@NotNull private IProgress m_shareIntoForConductorsProgress;

	public FindAndShareCmd(@NotNull IProject project, @NotNull Set<ILogicDesign> designs,
			@NotNull Set<IEntityShareCriteria> entitiesShareCriteria)
	{
		super(project, designs);
		m_entitiesShareCriteria = new HashSet<>(entitiesShareCriteria);
		ProgressGroup progressGroup = new ProgressGroup(StringUtils.EMPTY_STRING);
		m_findProgress = progressGroup.createChild(1, 1,
				ResourceMgr.getString(FindAndShareCmd.class, "FindAndShareCmd.MainProgress.FindShareableObjects"));
		m_shareProgress = progressGroup.createChild(0, 1,
				ResourceMgr.getString(FindAndShareCmd.class, "FindAndShareCmd.MainProgress.ShareObjects"));
		m_shareIntoProgress = progressGroup.createChild(0, 1,
				ResourceMgr.getString(FindAndShareCmd.class, "FindAndShareCmd.MainProgress.ShareObjects"));
		m_findForConductorsProgress = progressGroup.createChild(1, 1,
				ResourceMgr.getString(FindAndShareCmd.class, "FindAndShareCmd.MainProgress.FindShareableConductors"));
		m_shareForConductorsProgress = progressGroup.createChild(0, 1,
				ResourceMgr.getString(FindAndShareCmd.class, "FindAndShareCmd.MainProgress.ShareConductors"));
		m_shareIntoForConductorsProgress = progressGroup.createChild(0, 1,
				ResourceMgr.getString(FindAndShareCmd.class, "FindAndShareCmd.MainProgress.ShareConductors"));
		m_progress = progressGroup;
	}

	@Override protected void doExecute()
	{
		Predicate<ShareableEntityTypeEnum> isConductorType =
				type -> type.equals(ShareableEntityTypeEnum.NET) || type.equals(ShareableEntityTypeEnum.WIRE);
		Set<IShareableObjectGroup> shareableObjectGroups =
				findShareableObjectGroups(getEntitiesShareCriteria(isConductorType.negate()), m_findProgress);
		share(shareableObjectGroups, m_reporter, m_shareProgress);
		shareInto(shareableObjectGroups, m_reporter, m_shareIntoProgress);
		shareableObjectGroups =
				findShareableObjectGroups(getEntitiesShareCriteria(isConductorType), m_findForConductorsProgress);
		share(shareableObjectGroups, m_reporter, m_shareForConductorsProgress);
		shareInto(shareableObjectGroups, m_reporter, m_shareIntoForConductorsProgress);
	}

	@Override
	protected void displayBatchShareStatusTab(@NotNull Collection<IBatchShareStatusMessage> reportedMessages)
	{
		HTMLTable htmlTable = HTMLTable.createTable();
		htmlTable.setTitle(ResourceMgr.getString(FindAndShareCmd.class,"BatchShareStatusMessageTableModel.Report.title"));
		htmlTable.setBorder(0);
		htmlTable.setStripColor("#D3D3D3");
		htmlTable.setStripped(true);
		htmlTable.setStyle("background-color: " + "#D3D3D3" + ";");
		HTMLTableRow htmlTableRow1 = new HTMLTableRow();
		htmlTableRow1.addColumn(HTMLHelper.bold(ResourceMgr
				.getString(FindAndShareCmd.class, "BatchShareStatusMessageTableModel.Column.Severity.title")));
		htmlTableRow1.addColumn(HTMLHelper.bold(ResourceMgr
				.getString(FindAndShareCmd.class, "BatchShareStatusMessageTableModel.Column.Message.title")));
		htmlTableRow1.addColumn(HTMLHelper.bold(ResourceMgr
				.getString(FindAndShareCmd.class, "BatchShareStatusMessageTableModel.Column.Design.title")));
		htmlTableRow1.addColumn(HTMLHelper.bold(ResourceMgr
				.getString(FindAndShareCmd.class, "BatchShareStatusMessageTableModel.Column.Object.title")));
		htmlTableRow1.setStyle("background-color: " + "#FFFFFF" + ";");
		htmlTable.addRows(htmlTableRow1);
		int rowNumber = 1;
		for (IBatchShareStatusMessage next : reportedMessages) {

			HTMLTableRow htmlTableRow = new HTMLTableRow();
			String imageURLForLevel =
					StringUtils.nonNull(HTMLHelper.getImageURLForLevel(next.getStatus().getSeverityIndex() - 1));
			htmlTableRow.addColumn(HTMLHelper.image(imageURLForLevel, 16, 16, 16, 0, "CENTER"));
			htmlTableRow.addColumn(next.getMessage());
			htmlTableRow.addColumn(next.getDesignName());
			htmlTableRow.addColumn(HTMLHelper.link(next.getObjectDetailText(),next.getObjectDetailLink()));
			if (rowNumber % 2 == 0) {
				htmlTableRow.setStyle("background-color: " + "#FFFFFF" + ";");
			}
			htmlTable.addRows(htmlTableRow);
			rowNumber++;
		}

		LogHelper.printMsg(htmlTable.getString());
	}

	@NotNull private Set<IEntityShareCriteria> getEntitiesShareCriteria(
			@NotNull Predicate<ShareableEntityTypeEnum> typeFilter)
	{
		return m_entitiesShareCriteria.stream()
				.filter(entityShareCriteria -> typeFilter.test(entityShareCriteria.getEntityType()))
				.collect(Collectors.toSet());
	}

	@Override protected boolean doShareUnplacedObjects()
	{
		return true;
	}

	@Override
	@NotNull public OptionalString getProgressTitle()
	{
		return OptionalString.of(ResourceMgr.getString(FindAndShareCmd.class, "FindAndShareCmd.Progress.title"));
	}

	@Override
	@NotNull public OptionalString getProgressHeader()
	{
		return OptionalString.of(ResourceMgr.getString(FindAndShareCmd.class, "FindAndShareCmd.Progress.header"));
	}

	@Override public boolean isStoppable()
	{
		return true;
	}

	@Override public boolean supportsChildProgressBars()
	{
		return true;
	}

	@NotNull public IProgress getProgress()
	{
		return m_progress;
	}

	@Override public void run()
	{
		execute();
	}
}