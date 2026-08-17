package chs.caplets.logic.actions;

import chs.cof.logical.cable.IBlockDevice;
import chs.ctf.caf.utils.IPinProxy;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public interface IAddBlockPinActionModel
{

	@NotNull IBlockDevice getBlockDevice();

	Map<String, IPinProxy> getPinProxyMap();

	boolean isGetInputsForUsedPins();

	void setProcessUsedPins(boolean isProcessUsedPins);

	boolean isProcessUsedPins();

	boolean isValid(@NotNull List<IPinProxy> pinsToValidate);

	String getInvalidityReason();
}
