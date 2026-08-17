/*
 * Copyright 2010 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.selection.SelectSet;
import chs.cof.logical.schem.ISchemDiagram;
import chs.view.route.IModifiableCostStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Container;
import java.awt.Frame;

public interface ICAFUtilityProvider
{

	@Nullable ICapletController getController(@NotNull ISchemDiagram diagram);

	@Nullable ICapletModel getModel(@NotNull ISchemDiagram diagram);

	@Nullable Container getContainer();

	@Nullable Frame getDialogFrame();

	@Nullable ICapletModel getActiveCapletModel();

	@Nullable SelectSet getPreSelections();

	@Nullable IModifiableCostStrategy getCostStrategy();
}
