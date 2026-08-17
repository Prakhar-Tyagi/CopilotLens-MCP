/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.bridges;

import chs.bridges.adaptors.IAdaptorFormat;
import chs.bridges.adaptors.IAdaptorPluginMgr;
import chs.bridges.adaptors.net.ComputerConnection;
import chs.caf.IFIB;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import com.mentor.capital.javafx.interfaces.IRibbonConstants;
import com.mentor.capital.ui.IToggleAction;

import java.awt.event.ActionEvent;
import java.util.Observable;
import java.util.Observer;

/**
 * Base Action for Bridges-Cross-Highglight integration in Logic
 */
public abstract class BridgeLogicCrossHighlightAction extends BridgeLogicAppAction implements Observer, IToggleAction
{

	private IAdaptorPluginMgr _plugin;
	private String _actionTag;

	private boolean _selected = false;

	public BridgeLogicCrossHighlightAction(IAdaptorPluginMgr plugin, String actionTag,
			Integer mnemonic)
	{
		super((IFIB) plugin.getFIB());
		_plugin = plugin;
		_actionTag = "BridgeLogicCrossHighlightAction." + actionTag + ".";

		putValue(NAME, getActionNLSString("name.text"));
		putValue(SHORT_DESCRIPTION, getActionNLSString("shortDesc.text"));
		putValue(LONG_DESCRIPTION, getActionNLSString("longDesc.text"));
		putValue(MNEMONIC_KEY, mnemonic);
	}

	public void setSelected(boolean select)
	{
		String errorMsg = null;
		ComputerConnection ction = getConnection();
		if (ction == null) {
			errorMsg = getActionNLSString("Error.noConnection.text");
			MessageHelper.showErrorMessage(getMainWindow(), "", errorMsg);
			return;
		}

		try {
			if (select) {
				// Subscribe to connection
				ction.addObserver(this);
				beforeConnected();
				// Activate Connection
				ction.setActive(true, true);
				_selected = true;
			}
			else {
				beforeDisconnected();
				// De-activate Connection
				ction.setActive(false, true);
				_selected = false;
			}
		}
		catch (java.net.ConnectException connectException) {
			errorMsg = getActionNLSString("Error.noConnection.text");
		}
		catch (Exception e) {
			e.printStackTrace();
			errorMsg = e.getMessage();
			if (errorMsg == null || errorMsg.length() == 0) {
				errorMsg = "Program Error";
			}
		}

		if (errorMsg != null) {
			MessageHelper.showErrorMessage(getMainWindow(), "", errorMsg);
		}

		if (!_selected) {
			// Unsubscribe
			ction.deleteObserver(this);
		}
	}

	public void actionPerformed(ActionEvent ae)
	{
		setSelected(!_selected);
		firePropertyChange(IRibbonConstants.PROPERTY_TOGGLED, !_selected, _selected);
	}

	public void updateUI()
	{
	}

	/**
	 * @see java.util.Observer#update(Observable, Object)
	 */
	public void update(Observable iConnection, Object iArg)
	{
		// Processes connection states: just activated, just de-activated, message received
		boolean isError = false;
		String msg = null;

		try {
			ComputerConnection ction = (ComputerConnection) iConnection;

			if (iArg == null) {
				// Just (de-)activated
				if (ction.isActive()) {
					msg = getActionNLSString("connected.text");
					justConnected();
				}
				else {
					msg = getActionNLSString("disconnected.text");
					justDisconnected();
				}
			}
			else {
				if (iArg instanceof String) {
					messageReceived();
				}
				else if (iArg instanceof Integer) {
					if (((Integer) iArg).intValue() == ComputerConnection.EVENT_INTERRUPTED) {
						connectionInterrupted();
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			msg = e.getMessage();
			isError = true;
		}

		if (msg != null) {
			if (isError) {
				MessageHelper.showErrorMessage(getMainWindow(), "", msg);
			}
			else {
				MessageHelper.showInformationMessage(getMainWindow(), "", msg);
			}
		}

		if (isError) {
			setSelected(false);
		}
	}

	protected String getActionNLSString(String tag)
	{
		IAdaptorFormat format = getFormat();
		String fmtName = format == null ? "" : format.getNLSIdentifier();
		return ResourceMgr.getString(BridgeLogicCrossHighlightAction.class, _actionTag + tag, fmtName);
	}

	protected IAdaptorFormat getFormat()
	{
		return _plugin == null ? null : _plugin.getSupplierFormat();
	}

	protected ComputerConnection getConnection()
	{
		IAdaptorFormat format = getFormat();
		return format == null ? null : (ComputerConnection) format.getConnection();
	}

	protected void beforeConnected() throws Exception
	{
	}

	protected void justConnected() throws Exception
	{
	}

	protected void beforeDisconnected() throws Exception
	{
	}

	protected void justDisconnected() throws Exception
	{
	}

	protected void messageReceived() throws Exception
	{
	}

	protected void connectionInterrupted() throws Exception
	{
		String errorMsg = getActionNLSString("Error.connTerminated.text");
		throw new Exception(errorMsg);
	}

	@Override public boolean isOn()
	{
		return _selected;
	}
}
