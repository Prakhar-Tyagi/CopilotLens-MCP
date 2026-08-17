package chs.caplets.logic.actions;

import chs.cof.logical.ConvertPinTypeLogEnum;
import chs.cof.logical.cable.IDevicePin;
import chs.utility.PinTypeConversionChecker;

import java.util.List;

/**
 * Created with IntelliJ IDEA. User: nagamani Date: 14/2/14
 */
public class StudPinToNormalPinTypeConverter extends AbstractPinTypeConverter
{

	@Override protected boolean isStudAfterConversion()
	{
		return false;
	}

	@Override public ConvertPinTypeLogEnum isEnabled()
	{
		ConvertPinTypeLogEnum returnStauts = super.isEnabled();
		if (returnStauts != ConvertPinTypeLogEnum.NO_ERROR) {
			return returnStauts;
		}
		PinTypeConversionChecker checker = new PinTypeConversionChecker();
		boolean bValid = checker.executeDesignLevelCheckForPinConversion(m_selectedPins);
		if (!bValid) {
			for (IDevicePin devicePin : m_selectedPins) {
				List<ConvertPinTypeLogEnum> log = checker.getPinConversionStatus(devicePin);
				if (!log.isEmpty()) {
					return log.get(0);
				}
			}
		}
		return ConvertPinTypeLogEnum.NO_ERROR;
	}
}
