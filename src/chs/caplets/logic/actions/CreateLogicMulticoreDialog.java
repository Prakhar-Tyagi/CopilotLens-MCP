/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2004-2023 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.helpers.DualDisplayModel;
import chs.caf.caplet.helpers.MCProxy;
import chs.caplets.actions.AbstractCreateMulticoreDialog;
import chs.caplets.logic.actions.ui.LogicMulticoreEditPanel;
import chs.cofUtils.logical.concurrency.LogicConcurrentEditReporter;
import chs.common.IUID;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import chs.utilities.ui.messaging.IMessageContent;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: lstamper
 * Date: Jun 23, 2004
 * Time: 9:57:03 AM
 */

/**
 * Dialog for displaying the multicores & nets, ready for adding the indicators
 */
public class CreateLogicMulticoreDialog extends AbstractCreateMulticoreDialog
{

	@NotNull private CreateMulticoreContext m_context;
	@NotNull private Collection<IUID> m_alreadyLockedInThisSession;

	public CreateLogicMulticoreDialog(@Nullable Frame frame, @Nullable String title,
			@NotNull CreateMulticoreContext context, @NotNull Set<IUID> selectedUIDS)
	{
		this(frame, title, context,
				new LogicMulticoreEditPanel(context.getProxyRoot(), context.getEditType(), false,
						context.getEditScope(), selectedUIDS));
	}

	public CreateLogicMulticoreDialog(@Nullable Frame frame, @Nullable String title,
			@NotNull CreateMulticoreContext context, @NotNull LogicMulticoreEditPanel editPanel)
	{
		super(frame, title, editPanel);
		m_context = context;
		jbInit();
		pack();
	}

	protected void jbInit()
	{
		JPanel main = new JPanel();
		main.setLayout(new BorderLayout(5, 5));
		main.add(getPanel(), BorderLayout.CENTER);
		getOkButton().setEnabled(false);

		getContentPane().add(main);
	}

	@Override @NotNull public LogicMulticoreEditPanel getPanel()
	{
		return (LogicMulticoreEditPanel) super.getPanel();
	}

	public void setAlreadyLockedInThisSession(@NotNull Collection<IUID> alreadyLockedInThisSession)
	{
		m_alreadyLockedInThisSession = alreadyLockedInThisSession;
	}

	@Override protected boolean vetoOKAction()
	{
		boolean vetoed = super.vetoOKAction();

		if (vetoed){
			return true;
		}

		String errMessage = getPanel().WillAnyDesignHaveMultipleRevisionsOfSharedMC();
		if (errMessage != null) {
			MessageHelper.showErrorMessage(this, getTitle(), errMessage);
			return true;
		}

		if (m_context.isSharedEditScope()) {
			String header = ResourceMgr
					.getString(CreateMulticoreAction.class, "CreateMulticoreAction.sharedConfirmation.heading");
			String saveOption = ResourceMgr
					.getString(CreateMulticoreAction.class, "CreateMulticoreAction.sharedConfirmation.SaveButton");
			String cancelOption = ResourceMgr
					.getString(CreateMulticoreAction.class, "CreateMulticoreAction.sharedConfirmation.CancelButton");

			if (!MessageHelper.showSessionOptionDialog(null, getConfirmSaveMessage(), getTitle(), header, null,
					new String[]{saveOption, cancelOption}, saveOption, "CreateMulticoreAction",
					ResourceMgr.getString(CreateMulticoreAction.class,
							"CreateMulticoreAction.sharedConfirmation.actionText"))) {
				return true;
			}
		}

		Set<MCProxy> removedMCs = getRemovedMulticores();
		Set<MCProxy> addedMCs = getAddedMulticores();
		Set<MCProxy> modifiedMCs = getModifiedMulticores();
		Set<MCProxy> editedMCs = new HashSet<MCProxy>();
		editedMCs.addAll(addedMCs);
		editedMCs.addAll(modifiedMCs);
		editedMCs.addAll(removedMCs);
		if (!CreateMulticoreActionHelper.canProceedWithEditMulticore(
				m_context.getDesign(), editedMCs, m_alreadyLockedInThisSession,
				m_context.getEditType(),
				new LogicConcurrentEditReporter())) {
			//cannot process the edit in multiuser mode
			m_context.buildProxyTree();
			//  m_root of panel should be set to the new one as it is used for various operations
			getPanel().setRoot(m_context.getProxyRoot());
			DualDisplayModel ddModel = getPanel().getNewDualDisplayModel(m_context.getProxyRoot());

			getMulticoreTree().setModel(ddModel);
			getAvailableNetOrMulticoresList().setModel(ddModel.getListModel());
			getMulticoreTree().updateUI();
			getAvailableNetOrMulticoresList().updateUI();
			invalidate();

			// reset everything
			getRemovedMulticores().clear();
			getAddedMulticores().clear();
			getAddedIndicators().clear();
			getModifiedMulticores().clear();
			return true;
		}
		return false;
	}

	@NotNull private String getConfirmSaveMessage()
	{
		String editType = m_context.isMulticoreEditType() ? "MC" : "OB";

		return ResourceMgr.getString(CreateMulticoreAction.class,
				"CreateMulticoreAction.confirm.saveDesigns" + editType + ".message");
	}

	@Override @NotNull protected IMessageContent getConfirmEmptyMessage()
	{
		String editType = m_context.isMulticoreEditType() ? "Multicore" : "Overbraid";

		ResourceBasedMessageContent message = new ResourceBasedMessageContent(CreateMulticoreAction.class,
				"CreateMulticoreAction.confirm.empty" + editType);
		message.setImplicationsParameters(getInvalidMulticoresString());
		return message;
	}
}