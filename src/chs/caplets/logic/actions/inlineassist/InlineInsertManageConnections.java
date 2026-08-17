	/*
	 * This material contains trade secrets or otherwise confidential information owned by
	 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
	 * Access to and use of this information is strictly limited as set forth in the Customer's
	 * applicable agreements with SISW.
	 *
	 * Copyright 2024 Siemens
	 */
	package chs.caplets.logic.actions.inlineassist;

	import chs.caplets.logic.actions.ConductorConnectionChanger;
	import chs.cof.logical.cable.IAbstractPin;
	import chs.cof.logical.shared.ISharedPinList;
	import chs.ctf.caf.utils.IPinProxy;
	import chs.ctf.caf.utils.PinProxy;
	import org.jetbrains.annotations.NotNull;

	import java.util.Map;
	import java.util.Objects;

	/**
	 * Manage connections of pins based on viable mapping provided through Place Inline Action.
	 */
	public class InlineInsertManageConnections
	{

		@NotNull private final ISharedPinList mSharedPinList;
		@NotNull private Map<IAbstractPin, IPinProxy> mConnectedToViablePinMap;

		public InlineInsertManageConnections(@NotNull ISharedPinList sharedPinList,
				@NotNull Map<IAbstractPin, IPinProxy> connectedToViablePinMap)
		{
			mSharedPinList = sharedPinList;
			mConnectedToViablePinMap = connectedToViablePinMap;
		}

		public void invoke()
		{
			final ConductorConnectionChanger conductorConnectionChanger =
					new ConductorConnectionChanger(mSharedPinList, new InlineInsertConnectionSavePredicate());
			mConnectedToViablePinMap.keySet().stream().forEach(
					pin -> conductorConnectionChanger.addConnection(Objects.requireNonNull(pin.getLogicDesign()),
							new PinProxy(pin), mConnectedToViablePinMap.get(pin)));
			conductorConnectionChanger.changeConnections();
		}
	}
