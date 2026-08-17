/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025-2026 Siemens
 */

package chs.caplets.logic.actions.inlineassist;

import chs.caf.CAFUtils;
import chs.caf.caplet.helpers.snapping.ModelUtils;
import chs.cof.draw.IGfxContext;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.services.dynamicgfx.IDynamicGfxMediator;
import chs.utility.gfx.SearchGfxContext;
import chs.utility.topology.inlineconn.InlineShieldTerminationInfo;
import chs.utility.topology.utils.IInlineAssistFailureCollector;
import chs.utility.topology.utils.subsystem.messaging.MessagingServices;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Calculate the positions of shield pins in a schematic diagram.
 * It determines the appropriate positions based on the inline extent, shield conductor directions, pin orientation,
 * and jack side alignment.
 */
public class ShieldPinPositionCalculator
{

	@NotNull private final ShieldConductorsProvider mShieldConductorsProvider;
	@NotNull private final InlineShieldTerminationInfo mShieldTerminationInfo;
	private final ShieldTerminationPinTypeMapper mShieldTerminationPinTypeMapper;

	public ShieldPinPositionCalculator(@NotNull InlineShieldTerminationInfo shieldTerminationTypeInfo)
	{
		mShieldTerminationInfo = shieldTerminationTypeInfo;
		mShieldConductorsProvider = createShieldConductorsProvider();
		mShieldTerminationPinTypeMapper = new ShieldTerminationPinTypeMapper();
	}

	@NotNull private ShieldConductorsProvider createShieldConductorsProvider()
	{
		return new ShieldConductorsProvider();
	}

	/**
	 * Invokes the shield pin position calculation for the given connector data and schematic diagram.
	 * It processes the inline extent, determines the relevant shield conductors, and updates their termination
	 * positions and also updates the inline extent.
	 * <p>
	 * Note- This method will update Inline extent of newConnectorData boundaries based on the shield pin positions.
	 *
	 * @param newConnectorData The new connector data containing the inline extent and other relevant information.
	 * @param diagram          The schematic diagram in which the shield pins are to be placed.
	 */
	public void updateInlineExtent(@NotNull NewConnectorData newConnectorData, @NotNull ISchemDiagram diagram)
	{
		InlineExtent extent = newConnectorData.getExtent();

		Collection<IConductor> schemConductors = extent.getAddedConductors();
		List<IShieldConductor> shieldsToTerminateOnDiagram = getShieldsToTerminateOnDiagram(diagram, schemConductors);
		if (shieldsToTerminateOnDiagram.isEmpty()) {
			return;
		}

		Map<IShieldConductor, Integer> shieldDirections =
				createShieldPlacementDirectionProvider()
						.getDirections(shieldsToTerminateOnDiagram, schemConductors, newConnectorData.isVertical());
		for (IShieldConductor shield : shieldsToTerminateOnDiagram) {
			shieldDirections.computeIfAbsent(shield,
					shieldConductor -> ShieldPlacementDirectionProvider.SHIELD_DIR_DOWN);
		}
		boolean isPinVertical = !newConnectorData.isVertical();
		boolean isJackSideLeftBottom = !newConnectorData.getDirection().isReversedPinSide();
		updateShieldTerminationInfo(extent, shieldDirections, isPinVertical, isJackSideLeftBottom, diagram);
	}

	@NotNull private List<IShieldConductor> getShieldsToTerminateOnDiagram(@NotNull ISchemDiagram diagram,
			@NotNull Collection<IConductor> schemConductors)
	{
		List<IShieldConductor> shields = mShieldConductorsProvider.getShieldsToProcess(diagram, schemConductors);
		Collection<IShieldConductor> shieldsToTerminate = mShieldTerminationInfo.getShieldsToTerminate();
		shields.retainAll(shieldsToTerminate);
		return shields;
	}

	@NotNull private ShieldPlacementDirectionProvider createShieldPlacementDirectionProvider()
	{
		return new ShieldPlacementDirectionProvider();
	}

	/**
	 * adds entry in mShieldPinPositions for shield conductors and identified pin-positions based on their required directions,
	 * pin orientation, and the jack side alignment.
	 * Also updates this Inline extent boundaries based on the shield pin positions.
	 *
	 * @param shieldDirections     A map of shield conductors and their directions (e.g., up or down).
	 * @param isPinVertical        Whether the pins are oriented vertically.
	 * @param isJackSideLeftBottom Whether the jack side is on the left/bottom.
	 * @param diagram              The diagram in which the shields are placed.
	 */
	private void updateShieldTerminationInfo(@NotNull InlineExtent inlineExtent,
			@NotNull Map<IShieldConductor, Integer> shieldDirections,
			boolean isPinVertical, boolean isJackSideLeftBottom,
			@NotNull ISchemDiagram diagram)
	{
		Point firstPoint = inlineExtent.getFirstPoint();
		Point secondPoint = inlineExtent.getSecondPoint();
		BoundingBox boundingBox = new BoundingBox(firstPoint, secondPoint);
		// Context created for checking overlapping segments while searching for pin positions
		IGfxContext context = new SearchGfxContext(diagram, diagram.getExtent());

		boolean failedTerminatingShield = false;
		// Iterate over each shield conductor and its placement direction.
		for (Map.Entry<IShieldConductor, Integer> entry : shieldDirections.entrySet()) {
			IShieldConductor shield = entry.getKey();
			boolean growingBottom = entry.getValue() == ShieldPlacementDirectionProvider.SHIELD_DIR_DOWN;
			boolean validPointFound = false;
			boolean triedBothDirections = false;

			ShieldPositionData placement = new ShieldPositionData();
			ShieldTerminationPinType jackType = mShieldTerminationPinTypeMapper.getShieldTerminationPinType(
					mShieldTerminationInfo.getJackTerminationPin(shield));
			ShieldTerminationPinType plugType = mShieldTerminationPinTypeMapper.getShieldTerminationPinType(
					mShieldTerminationInfo.getPlugTerminationPin(shield));
			boolean needsMating = jackType.canConnectWith(plugType);

			// Loop until a valid point is found or both directions have been exhausted.
			while (!validPointFound) {
				// Update the pin position based on its orientation and growth direction.
				Point jackPoint = calculatePosition(isPinVertical, growingBottom, isJackSideLeftBottom, boundingBox);

				if (needsMating) {
					Point plugPoint = getMatePoint(jackPoint, boundingBox, isPinVertical, isJackSideLeftBottom);

					if (isPointValid(jackPoint, diagram, context) && isPointValid(plugPoint, diagram, context)) {
						addShieldPlacementInfoToExtent(placement, shield, jackPoint, plugPoint, inlineExtent,
								isPinVertical, boundingBox, growingBottom);
						validPointFound = true;
					}
				}
				else {
					if (isPointValid(jackPoint, diagram, context)) {
						expandBoundingBox(isPinVertical, boundingBox, growingBottom);
						Point plugPoint =
								calculatePosition(isPinVertical, growingBottom, !isJackSideLeftBottom, boundingBox);

						if (isPointValid(plugPoint, diagram, context)) {
							addShieldPlacementInfoToExtent(placement, shield, jackPoint, plugPoint, inlineExtent,
									isPinVertical, boundingBox, growingBottom);
							validPointFound = true;
						}
					}
				}

				if (!validPointFound) {
					// If both directions have been tried, break the loop
					if (triedBothDirections) {
						failedTerminatingShield = true;
						break;
					}
					growingBottom = !growingBottom; // Try the other direction
					triedBothDirections = true;
				}
			}
		}

		if (failedTerminatingShield) {
			MessagingServices.getService(IInlineAssistFailureCollector.class)
					.addMessage("InlineAssistFailureCollector.InsufficientSpace");
		}

		//update the extent boundaries based on the shield pin positions
		firstPoint.setLocation(boundingBox.getMinX(), boundingBox.getMinY());
		secondPoint.setLocation(boundingBox.getMaxX(), boundingBox.getMaxY());
	}

	private void addShieldPlacementInfoToExtent(
			@NotNull ShieldPositionData placement,
			@NotNull IShieldConductor shield,
			@NotNull Point jackPoint,
			@NotNull Point plugPoint,
			@NotNull InlineExtent inlineExtent,
			boolean isPinVertical,
			@NotNull BoundingBox boundingBox,
			boolean growingBottom)
	{
		addTerminationsToPlacement(placement, shield, jackPoint, plugPoint);
		inlineExtent.addShieldTerminationPosition(shield, placement);
		expandBoundingBox(isPinVertical, boundingBox, growingBottom);
	}

	private void addTerminationsToPlacement(@NotNull ShieldPositionData placement,
			@NotNull IShieldConductor shield,
			@NotNull Point jackPoint,
			@NotNull Point plugPoint)
	{
		ShieldTerminationInfo jackTermination = new ShieldTerminationInfo(
				jackPoint,
				mShieldTerminationInfo.getJackTerminationType(shield),
				mShieldTerminationInfo.getJackTerminationPin(shield));
		placement.addTermination(jackTermination);

		ShieldTerminationInfo plugTermination = new ShieldTerminationInfo(
				plugPoint,
				mShieldTerminationInfo.getPlugTerminationType(shield),
				mShieldTerminationInfo.getPlugTerminationPin(shield));
		placement.addTermination(plugTermination);
	}

	@NotNull
	private Point calculatePosition(boolean isPinVertical, boolean growingBottom,
			boolean isJackSideLeftBottom, @NotNull BoundingBox boundingBox)
	{
		Point point = new Point();
		if (isPinVertical) {
			updateVerticalPinPosition(point, growingBottom, isJackSideLeftBottom, boundingBox);
		}
		else {
			updateHorizontalPinPosition(point, growingBottom, isJackSideLeftBottom, boundingBox);
		}
		return point;
	}

	// Expand the bounding box based on the orientation and direction
	private static void expandBoundingBox(boolean isPinVertical, BoundingBox boundingBox, boolean growingBottom)
	{
		if (isPinVertical) {
			boundingBox.expandVertical(growingBottom);
		}
		else {
			boundingBox.expandHorizontal(growingBottom);
		}
	}

	private boolean isPointValid(@NotNull Point shieldTermPinPoint, @NotNull ISchemDiagram diagram,
			@NotNull IGfxContext context)
	{
		return checkValidity(shieldTermPinPoint, diagram, context);
	}

	private boolean checkValidity(@NotNull Point shieldTermPinPoint, @NotNull ISchemDiagram diagram,
			@NotNull IGfxContext context)
	{
		Collection<IDynamicGfxMediator> objectsAtPoint = getObjectsAtPoint(shieldTermPinPoint, diagram, context);
		if (objectsAtPoint == null) {
			return true;
		}
		return objectsAtPoint.stream()
				.noneMatch(ISegment.class::isInstance);
	}

	@Nullable
	private Collection<IDynamicGfxMediator> getObjectsAtPoint(@NotNull Point point,
			@NotNull ISchemDiagram diagram,
			@NotNull IGfxContext context)
	{
		final int snapRadius = ModelUtils.getSnapRadius(null, false);
		return new ModelUtils().getSnapCandidates(context, diagram, point, snapRadius,
				CAFUtils.getInstance().getCommonFactory().createExtent());
	}

	private void updateVerticalPinPosition(@NotNull Point shieldTermPinPoint, boolean growingBottom,
			boolean isJackSideLeftBottom, @NotNull BoundingBox boundingBox)
	{
		int xCord = isJackSideLeftBottom ? boundingBox.getMinX() : boundingBox.getMaxX();
		int yCord = growingBottom ? boundingBox.getMinY() : boundingBox.getMaxY();
		shieldTermPinPoint.setLocation(xCord, yCord);
	}

	private void updateHorizontalPinPosition(@NotNull Point shieldTermPinPoint, boolean growingLeft,
			boolean isJackSideLeftBottom, @NotNull BoundingBox boundingBox)
	{
		int yCord = isJackSideLeftBottom ? boundingBox.getMinY() : boundingBox.getMaxY();
		int xCord = growingLeft ? boundingBox.getMinX() : boundingBox.getMaxX();
		shieldTermPinPoint.setLocation(xCord, yCord);
	}

	@NotNull
	private Point getMatePoint(@NotNull Point shieldTermPinPoint, @NotNull BoundingBox boundingBox,
			boolean isPinVertical, boolean isJackSideLeftBottom)
	{
		if (isPinVertical) {
			int xCord = isJackSideLeftBottom ? boundingBox.getMaxX() : boundingBox.getMinX();
			return new Point(xCord, shieldTermPinPoint.y);
		}
		int yCord = isJackSideLeftBottom ? boundingBox.getMaxY() : boundingBox.getMinY();
		return new Point(shieldTermPinPoint.x, yCord);
	}
}
