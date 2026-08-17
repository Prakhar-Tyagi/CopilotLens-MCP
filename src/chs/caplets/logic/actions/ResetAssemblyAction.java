/*
 * Copyright 2011-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.AssemblyUIUtils;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.cof.logical.IDesign;
import chs.cof.logical.cable.IAssembly;
import chs.cof.project.IProject;
import chs.common.IUID;
import chs.subsystem.structure.StructureMatchService;
import chs.utilities.AppInfo;
import chs.utilities.ResourceMgr;
import chs.utility.AssemblyUtils;
import chs.utility.helpers.LibraryHelper;
import chs.utility.logic.ILogicModel;

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: brangan Date: May 25, 2011 Time: 2:12:06 PM To change this template use File |
 * Settings | File Templates.
 */
public class ResetAssemblyAction extends ControllerActionRT
{

	private IDesign m_design;

	private String m_warnTitle = ResourceMgr.getString(ResetAssemblyActionUI.class,
			"ResetAssemblyAction.logic.warning.title");
	private String m_warnMsg1 = ResourceMgr.getString(ResetAssemblyActionUI.class,
			"ResetAssemblyAction.logic.warning.message1");
	private String m_warnMsg2 = ResourceMgr.getString(ResetAssemblyActionUI.class,
			"ResetAssemblyAction.logic.warning.message2");

	private String m_warnHeading = ResourceMgr.getString(ResetAssemblyActionUI.class,
			"ResetAssemblyAction.logic.warning.heading");
	private String m_outputMsg = ResourceMgr.getString(ResetAssemblyActionUI.class,
			"ResetAssemblyAction.logic.output.message");

	public ResetAssemblyAction(ICapletController controller)
	{
		super(controller);
	}

	public String getActionUIClass()
	{
		return ResetAssemblyActionUI.class.getName();
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		ICapletModel capletModel = getController().getCapletModel();
		m_design = ((ILogicModel) capletModel).getDesign();
		return IActionEnum.eCompleted;
	}

	/**
	 * Theory: if none selected selectset = all assembly objects warning dialog - all the assembly will be affected by this
	 * action.
	 * <p/>
	 * for each assembly: . get all children . if it is child (cavityseal, terminal, backshell, backshellplug, bs-seal,
	 * addicomp, cavicomp) . remove it from assembly // manually added - nonparented child in assembly will also be removed
	 * in this way.
	 * <p/>
	 * . if it is parent: get the definition from AssemblyActionHelper . newAssemblyChildren = get all its children. . get
	 * the pref setting for the children . for each child: . if pref = yes . add the child_uid to assembly . else . remove
	 * it without checking whether it is there in assembly.
	 */
	protected boolean onTerminate(boolean successful)
	{
		if (successful) {
			String appTitle = AppInfo.getFullApplicationName(AppInfo.App.LOGIC_DESIGNER);

			List<IAssembly> assemblies = AssemblyUIUtils.getOperands(getController(),
					m_design.getConnectivity(), m_warnHeading, m_warnTitle,
					m_warnMsg1 + ' ' + appTitle + ' ' + m_warnMsg2);

			if (!assemblies.isEmpty()) {
				updateAssemblyContents(assemblies, m_design.getProject());
				CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(m_outputMsg);
			}
			m_design = null;
		}

		return successful;
	}

	protected void updateAssemblyContents(List<IAssembly> assemblies, IProject project)
	{
		Set<IAssembly> updatedAssemblies = AssemblyUtils.updateAssembliesAsPerPrefereces(assemblies,
				project, false, true,
				Collections.<IUID>emptyList(), null);
		for (IAssembly assembly : updatedAssemblies) {
			if (assembly.getLibraryObject() != null) {
				boolean cotsAssembly = assembly.isCOTSAssembly();
				if (cotsAssembly || !StructureMatchService.getInstance()
						.validateExactMatch(assembly, assembly.getLibraryObject())) {
					LibraryHelper.assignLibraryPartWithDefaults(assembly, null);
				}
			}
		}
	}
}
