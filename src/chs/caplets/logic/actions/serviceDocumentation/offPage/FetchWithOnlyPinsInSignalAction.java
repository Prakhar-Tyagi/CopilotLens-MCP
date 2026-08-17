/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.serviceDocumentation.offPage;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.common.IDesignDescriptor;
import chs.images.CHSImageLoader;
import chs.publisher.offPage.ISelectionForFetch;
import chs.publisher.offPage.ISignalContentToBeCopiedProvider;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;

public class FetchWithOnlyPinsInSignalAction extends FetchOffPageConnectivityAction
{

	public FetchWithOnlyPinsInSignalAction(ICapletController controller)
	{
		super(controller);
	}

	@NotNull protected ISignalContentToBeCopiedProvider.MULTITERM_TRACE_STRATEGY getMultitermStrategy()
	{
		return ISignalContentToBeCopiedProvider.MULTITERM_TRACE_STRATEGY.STOP_AT_FIRST;
	}

	@NotNull protected ISignalContentToBeCopiedProvider.ASSOCIATED_OBJECTS_STRATEGY getAssociatedObjectsStrategy()
	{
		return ISignalContentToBeCopiedProvider.ASSOCIATED_OBJECTS_STRATEGY.DO_NOT_BRING;
	}

	@NotNull
	protected FetchOffPageObjectsCmd createCommand(ISignalContentToBeCopiedProvider contentProvider, IProject project,
			ISchemDiagram activeDiagram,
			@NotNull ILogicDesign activeDesignContainer, ISelectionForFetch selection,
			List<IDesignDescriptor> scope)
	{
		return new FetchOffPageObjectsCmd(project, scope, activeDiagram, activeDesignContainer,
				selection, contentProvider, false);
	}

	public String getActionUIClass()
	{
		return UIWithOnlyPinsInSignal.class.getName();
	}

	@ApplicationSpecification(
			includeIn = {Application.SvcDoc})
	public static class UIWithOnlyPinsInSignal extends FetchOffPageConnectivityAction.UI
	{

		public UIWithOnlyPinsInSignal(ICaplet caplet)
		{
			super(caplet);
			setEnabled(true);
		}

		public void setupUI()
		{
			Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/fetch-offpage-references-small.png");
			String name = ResourceMgr
					.getString(FetchWithOnlyPinsInSignalAction.class,
							"FetchWithOnlyPinsInSignalAction.name.action.text");
			Integer mnemonic = (int) ResourceMgr
					.getMnemonic(FetchWithOnlyPinsInSignalAction.class,
							"FetchWithOnlyPinsInSignalAction.mnemonic.text");
			putValue(NAME, name);
			KeyStroke accel = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.ALT_DOWN_MASK);
			putValue(ACCELERATOR_KEY, accel);
			putValue(MNEMONIC_KEY, mnemonic);
			putValue(SHORT_DESCRIPTION, ResourceMgr.getString(FetchWithOnlyPinsInSignalAction.class,
					"FetchWithOnlyPinsInSignalAction.shortDesc.action.text"));
			putValue(LONG_DESCRIPTION,
					ResourceMgr.getString(FetchWithOnlyPinsInSignalAction.class,
							"FetchWithOnlyPinsInSignalAction.longDesc.action.text"));
			putValue(SMALL_ICON, icon);
		}

		@Override public Icon getInactiveIcon()
		{
			return CHSImageLoader.loadImageIcon("chs/images/app/fetch-offpage-references-disabled-small.png");
		}

		public String getActionClass()
		{
			return FetchWithOnlyPinsInSignalAction.class.getName();
		}
	}
}
