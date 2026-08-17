/*
* Copyright 2017 Mentor Graphics Corporation
* All Rights Reserved
*
* THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
* INFORMATION WHICH IS THE PROPERTY OF MENTOR
* GRAPHICS CORPORATION OR ITS LICENSORS AND IS
* SUBJECT TO LICENSE TERMS.
*/

package chs.caplets.logic.helpers;

import chs.cof.logical.cable.IAssembly;
import chs.cof.logical.cable.IGeneralHighway;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.ISingleLine;
import chs.cof.logical.schem.ISchemStackPin;
import chs.common.IUID;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author pbhawsar on 22-05-2017
 */
public interface ILogicObjectChildrenProvider
{

	@NotNull List<IUID> getChildren(@NotNull IPinList pinList);

	@NotNull List<IUID> getChildren(@NotNull IGeneralHighway highway);

	@NotNull List<IUID> getChildren(@NotNull IAssembly assembly);

	@NotNull List<IUID> getChildren(@NotNull IMulticore multicore);

	@NotNull List<IUID> getChildren(@NotNull ISchemStackPin schemStackPin);
}
