/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.utils;

import chs.caf.CAFUtils;
import chs.caplets.logic.actions.shared.IHyperLinkStatusMessage;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.shared.ISharedObject;
import chs.common.IDesignContainer;
import chs.common.IUID;
import chs.services.ui.HyperlinkUtils;
import chs.system.UIDMgr;
import chs.utilities.CommonUtils;
import chs.utilities.Pair;
import chs.utilities.StringUtils;
import com.mentor.capital.javafx.table.ColumnInformation;
import com.mentor.capital.javafx.table.TableDataStorage;
import com.mentor.capital.javafx.table.TableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Base table model for StatusMessageTableWindow
 */
public abstract class StatusMessageTableModel<T> extends TableModel<T>
{

	@NotNull private String m_tablePrefID;

	protected StatusMessageTableModel(@NotNull String tablePrefID, @NotNull TableDataStorage<T> dataStorage,
			@Nullable Function<String, ColumnInformation<T>> columnsCreator)
	{
		super(dataStorage, columnsCreator);
		m_tablePrefID = tablePrefID;
	}

	@NotNull public String getTablePrefID()
	{
		return m_tablePrefID;
	}

	@NotNull public abstract List<ColumnInformation<T>> getColumns();

	@Nullable protected HyperlinkInfo getObjectHyperlinkInfo(@NotNull IHyperLinkStatusMessage statusMessage)
	{
		String objectDetailText = statusMessage.getObjectDetailText();
		final String objectDetailLink = statusMessage.getObjectDetailLink();
		HyperlinkInfo objectHyperlinkInfo = null;
		if (StringUtils.isNotBlank(objectDetailText)) {
			if (statusMessage.isSharedObjectLink()) {
				Pair<IDesignContainer, IUID> designAndObject =
						HyperlinkUtils.getInstance().getDesignAndObjectUID(objectDetailLink);
				ISharedObject sharedObject = UIDMgr.getObjectOfType(designAndObject.getSecond(), ISharedObject.class);
				if (sharedObject != null) {
					CAFUtils instance = CAFUtils.getInstance();
					List<ILogicDesign> designsToLookFor =
							new ArrayList<>(instance.getOpenedDesigns(ILogicDesign.class));
					ILogicDesign logicDesign =
							CommonUtils.cast(instance.getActiveDesignContainer(), ILogicDesign.class);
					if (logicDesign != null) {
						designsToLookFor.remove(logicDesign);
						designsToLookFor.add(0, logicDesign);
					}
					for (ILogicDesign openedDesign : designsToLookFor) {
						IConnectivity connectivity = openedDesign.getLoadedConnectivity();
						ILogicObject logicObjectForShared =
								connectivity != null ? connectivity.findLogicObjectForShared(sharedObject) : null;
						if (logicObjectForShared != null) {
							String hyperlink = IHyperLinkStatusMessage
									.getHyperlink(openedDesign.getUID(), logicObjectForShared.getUID());
							objectHyperlinkInfo = new HyperlinkInfo(objectDetailText, hyperlink);
							break;
						}
					}
				}
			}
			if (objectHyperlinkInfo == null) {
				objectHyperlinkInfo = new HyperlinkInfo(objectDetailText, objectDetailLink);
			}
		}
		return objectHyperlinkInfo;
	}
}