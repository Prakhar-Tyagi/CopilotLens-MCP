package chs.caplets.shared;

import chs.cof.symbol.IPSMStamp;

import java.awt.Frame;

public interface StampCreationParameterHolder
{

	IPSMStamp createStampBasedOnParameters();

	void collectParamsForCreation(Frame frame);

	UserActionFailureReason validateCreationParameters();

	String getName();

    boolean canceledAction();
}