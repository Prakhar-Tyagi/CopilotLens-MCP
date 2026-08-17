/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import javax.swing.ListModel;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Jan 26, 2005 Time: 7:30:39 PM
 */
public interface IModifiableListModel extends ListModel
{

	boolean add(Object obj);

	boolean remove(Object obj);
}
