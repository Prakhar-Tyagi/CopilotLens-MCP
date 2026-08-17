package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.cof.logical.cable.IBlockDevice;
import chs.cof.logical.cable.ICableFactory;
import chs.cof.logical.cable.IConnectivity;
import chs.common.IUID;

public class CreateFunctionBlockAction extends CreateBlockDeviceAction

{

	public CreateFunctionBlockAction(ICapletController controller)
	{
		super(controller);
	}

	protected IBlockDevice createBlock(ICableFactory cblFactory, IUID uid)
	{
		return cblFactory.createFunctionBlock(uid);
	}

	protected void addIntoConnectivity(IBlockDevice device, IConnectivity connectivity)
	{
		IConnectivity conn = connectivity;
		assert conn != null;
		conn.addBlockDevice(device);
	}

	public String getActionUIClass()
	{
		return CreateFunctionBlockActionUI.class.getName();
	}
}
