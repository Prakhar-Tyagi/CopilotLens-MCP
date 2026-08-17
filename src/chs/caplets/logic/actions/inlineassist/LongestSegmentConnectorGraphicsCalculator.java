/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2016-2023 Siemens
 */

package chs.caplets.logic.actions.inlineassist;

import chs.caf.CAFUtils;
import chs.caf.caplet.helpers.snapping.ModelUtils;
import chs.caplets.topology.inlineassist.GraphicalConductorComparator;
import chs.caplets.topology.inlineassist.IInlineAssistConductor;
import chs.caplets.topology.inlineassist.ILongestSegmentFinderClient;
import chs.cof.draw.IGfxContext;
import chs.cof.draw.ILine;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IConnected;
import chs.cof.drawplus.IDecorative;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IJoint;
import chs.cof.drawplus.IPropertiedGraphic;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IPhysicalConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.schem.IShieldBody;
import chs.cof.logical.shared.ISharedConductor;
import chs.common.IExtent;
import chs.common.ILocation;
import chs.common.INamedUIDObject;
import chs.common.Side;
import chs.services.dynamicgfx.IDynamicGfxMediator;
import chs.services.dynamicgfx.ISmartPoint;
import chs.services.dynamicgfx.SmartPointHelper;
import chs.utilities.CHSConstants;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.Environment;
import chs.utilities.Pair;
import chs.utility.gfx.SearchGfxContext;
import chs.utility.helpers.SchemConductorHelper;
import chs.utility.topology.utils.InlineAssistUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

public class LongestSegmentConnectorGraphicsCalculator implements IConnectorGraphicsCalculator
{
	/**
	 * Return value for function to work out whether or not to reverse the 'normal' graphical inline insertion which
	 * when defined as top-left to bottom-right puts the plug on the right for horizontal inlines and the bottom for
	 * vertical inlines.
	 */
	protected enum GraphicalInlineDirection
	{
		// No pin on this segment
		IGNORE,
		// Pin on this segment indicates normal direction
		NORMAL,
		// Pin on this segment indicates reversed direction
		REVERSED;

		/**
		 * Returns the reverse version of this enum value except if IGNORE when it returns IGNORE.
		 * <p>
		 *
		 * @return GraphicalInlineDirection
		 */
		@NotNull public GraphicalInlineDirection reversed()
		{
			if (this == IGNORE) {
				return IGNORE;
			}
			return this == NORMAL ? REVERSED : NORMAL;
		}
	}

	/**
	 * Indicates the orientation of the segments we are seeking to extend the extent over.
	 */
	private enum SeekOrientation
	{
		HORIZONTAL((point, delta) -> new Point(point.x + delta, point.y)),
		VERTICAL((point, delta) -> new Point(point.x, point.y + delta));

		@NotNull private final BiFunction<Point, Integer, Point> mTranslater;

		SeekOrientation(@NotNull final BiFunction<Point, Integer, Point> translater)
		{
			mTranslater = translater;
		}

		Point getAdjustedPoint(Point point, int delta)
		{
			return mTranslater.apply(point, delta);
		}
	}

	protected static Point zeroOffsetPoint = new Point(0, 0);
	@NotNull private final ModelUtils mModelUtils = new ModelUtils();
	private IBaseDiagram mDiagram;
	private Map<IPhysicalConductor, IInlineAssistConductor> mConductors;
	// Already split logical or shared wires
	private Set<INamedUIDObject> mSplitConductors;
	private int mMaxGridSpacingForNonMcCond;
	private int mMaxGridSpacingForMcCond;
	// These are the root multicores of the last conductors we split. They is only set if max grid spacing is exceeded,
	// we then set this variable and use a more lenient grid spacing, but encountering any conductor that does not have
	// the same root multicore causes extent extension to stop.
	@Nullable private IMulticore mFirstPreviousRootMulticore;
	@Nullable private IMulticore mSecondPreviousRootMulticore;

	public LongestSegmentConnectorGraphicsCalculator()
	{
		mMaxGridSpacingForNonMcCond = getGridSpacingValue("CAPITAL_RA_INLINE_SPACING", 4);
		mMaxGridSpacingForMcCond = getGridSpacingValue("CAPITAL_RA_INLINE_SPACING_MC", 8);
	}

	public Collection<NewConnectorData> getNewConnectorsData(@NotNull Collection<IInlineAssistConductor> conductors,
			@NotNull Collection<IConductor> targetConductors,
			@NotNull Collection<IgnoredConductorInformation> ignoredConductors)
	{
		mConductors = new HashMap<>();
		mSplitConductors = new HashSet<>();
		InlineAssistUtils.populateConductors(conductors, mConductors);
		final Set<NewConnectorData> result = new HashSet<>();
		final ILongestSegmentFinderClient client =
				(targetConds, usedConds, longestSegment, isVertical, ignoredConds) -> {
					collectConnector(targetConds, usedConds, longestSegment, isVertical, ignoredConds, result);
				};

		List<IConductor> sortedConductors =
				CollectionUtils.createSortedList(targetConductors.iterator(), new GraphicalConductorComparator());
		final LongestSegmentFinder longestSegmentFinder =
				new LongestSegmentFinder(client, conductors, sortedConductors, ignoredConductors);
		longestSegmentFinder.invoke();
		mConductors = null;
		mSplitConductors = null;
		return CollectionUtils.createSortedList(result.iterator(), new NewConnectorDataComparator());
	}

	private void collectConnector(@NotNull Collection<IConductor> targetConductors,
			@NotNull Collection<IConductor> usedConductors, @NotNull ISegment longestSegment, boolean isVertical,
			@NotNull Collection<IgnoredConductorInformation> ignoredConductors,
			@NotNull Collection<NewConnectorData> result)
	{
		IConductor startingConductor = longestSegment.getConductor();
		if (mSplitConductors.contains(getLogicalConductor(startingConductor))) {
			return;
		}
		boolean isReversed = isReversed(longestSegment);
		final Optional<InlineExtent> connectorExtent =
				getConnectorExtent(longestSegment, isVertical, usedConductors, targetConductors, isReversed);
		if (connectorExtent.isPresent()) {
			result.add(new NewConnectorData(connectorExtent.get(), getInlineDirection(isVertical, isReversed)));
		}
		else {
			final IgnoredConductorInformation information = new IgnoredConductorInformation(startingConductor);
			information.setIgnoredReason(IgnoredConductorInformation.Reason.OVERLAPPING_SEGMENTS);
			ignoredConductors.add(information);
		}
	}

	/**
	 * For this target segment do we need to reverse the jack/plug direction compared to the default that the logic
	 * actions do, which is jack on left for horizontal and jack on top for vertical, this is based on defining the
	 * inline top left to bottom right.
	 * <p>
	 *
	 * @param segment Segment to check direction of.
	 *
	 * @return true if we should reverse the logic default.
	 */
	private boolean isReversed(@NotNull ISegment segment)
	{
		final IConductor conductor = segment.getConductor();
		final chs.cof.logical.cable.IConductor logicalConductor = conductor.getConnectivity();
		final IInlineAssistConductor inlineAssistConductor = mConductors.get(logicalConductor);
		if (inlineAssistConductor == null) {
			throw new IllegalStateException("Graphical conductor must map to logical");
		}
		final Side direction = segment.getDirection();
		final Pair<IJoint, IJoint> joints = segment.getStartAndEnd();
		// We need to find which end of the graphical conductor connects to either the source jack or plug. We need
		// to define the end in terms of start-end joint of the provided segment.
		List<IConnected> startSegments = SchemConductorHelper.getOrderedSegments(segment, joints.getFirst(), conductor);
		List<IConnected> endSegments = SchemConductorHelper.getOrderedSegments(segment, joints.getSecond(), conductor);

		int numberOfAdditionalSegments = startSegments.size() + endSegments.size();
		final IAbstractPin plugSidePin = inlineAssistConductor.getPlugSidePin();
		final IAbstractPin jackSidePin = inlineAssistConductor.getJackSidePin();
		GraphicalInlineDirection inlineDirection;
		if (numberOfAdditionalSegments == 0) {
			// Checking for jack/plug pins on start/end of the segment
			inlineDirection = getDirectionOfSegment(plugSidePin, jackSidePin, segment);
		}
		else {
			// Checking for jack/plug pins on each end of the start/end segments that define the conductor
			IConnected startSegment = startSegments.isEmpty() ? segment : startSegments.get(startSegments.size() - 1);
			IConnected endSegment = endSegments.isEmpty() ? segment : endSegments.get(endSegments.size() - 1);
			inlineDirection = getDirectionOnAdditionalSegment(plugSidePin, jackSidePin, startSegment, endSegment);
		}
		if (inlineDirection == GraphicalInlineDirection.IGNORE) {
			throw new IllegalStateException("Must be able to determine plug-jack direction from graphical conductor: " +
					logicalConductor.getName());
		}
		boolean isReversed = inlineDirection == GraphicalInlineDirection.REVERSED;

		// Jack plug always face the same direction regardless of the position of the start-end joints, but the this
		// affects which direction we traversed to find the target pin so we have to reverse it if needed.
		if (direction.isLeft() || direction.isBottom()) {
			isReversed = !isReversed;
		}
		return isReversed;
	}

	/**
	 * For conductors with more than one segment check the startSegment and endSegment individually. There are four
	 * cases to check, we check all four to support design wide objects where we may not find the pin on the a
	 * particular graphical conductor instance.
	 * <p>
	 *
	 * @param plugSidePin Pin to connect to the plug
	 * @param jackSidePin Pin to connect to the jack
	 * @param startSegment Start segment of the conductor
	 * @param endSegment End segment of the conductor
	 *
	 * @return Direction of the jack/plug insertion
	 */
	@NotNull private GraphicalInlineDirection getDirectionOnAdditionalSegment(@NotNull IAbstractPin plugSidePin,
			@NotNull IAbstractPin jackSidePin, @NotNull IConnected startSegment, @NotNull IConnected endSegment)
	{
		GraphicalInlineDirection inlineDirection = GraphicalInlineDirection.IGNORE;
		if (getDirectionOfSegment(plugSidePin, startSegment) != GraphicalInlineDirection.IGNORE ||
				getDirectionOfSegment(jackSidePin, endSegment) != GraphicalInlineDirection.IGNORE) {
			inlineDirection = GraphicalInlineDirection.REVERSED;
		}
		else if (getDirectionOfSegment(jackSidePin, startSegment) != GraphicalInlineDirection.IGNORE ||
				getDirectionOfSegment(plugSidePin, endSegment) != GraphicalInlineDirection.IGNORE) {
			inlineDirection = GraphicalInlineDirection.NORMAL;
		}
		return inlineDirection;
	}

	/**
	 * For a single segment get the jack/plug direction. This is all that needs to be done for a single segment
	 * conductor.
	 * <p>
	 *
	 * @param plugSidePin Pin to connect to the plug
	 * @param jackSidePin Pin to connect to the jack
	 * @param segment The one and only segment on the graphical conductor
	 *
	 * @return Direction of the jack/plug insertion
	 */
	@NotNull
	protected GraphicalInlineDirection getDirectionOfSegment(@NotNull IAbstractPin plugSidePin,
			@NotNull IAbstractPin jackSidePin, @NotNull IConnected segment)
	{
		GraphicalInlineDirection inlineDirection = getDirectionOfSegment(plugSidePin, segment);
		if (inlineDirection == GraphicalInlineDirection.IGNORE) {
			inlineDirection = getDirectionOfSegment(jackSidePin, segment).reversed();
		}
		return inlineDirection;
	}

	/**
	 * For a single segment get the jack/plug direction for a single pin. For the single segment conductor case this can
	 * be used to get the actual direction if the pin is found. For the multiple segment case the return value is just
	 * an indicator to say whether or not the pin was found on the provided segment.
	 * <p>
	 *
	 * @param pin Pin to connect to the jack or plug
	 * @param segment A segment on the graphical conductor
	 *
	 * @return Direction of the jack/plug insertion
	 */
	@NotNull
	private GraphicalInlineDirection getDirectionOfSegment(@NotNull IAbstractPin pin, @NotNull IConnected segment)
	{
		final IBaseDiagram diagram = IBaseDiagram.Statics.getOwningDiagram(segment);
		if (diagram == null) {
			throw new IllegalStateException("Segment must exist on diagram");
		}
		final List<IDiagramObject> pinGraphics = CollectionUtils.createList(diagram.getRepresentations(pin.getUID()));
		final Pair<IJoint, IJoint> startAndEnd = segment.getStartAndEnd();
		if (isPinAssociated(startAndEnd.getFirst(), pinGraphics)) {
			return GraphicalInlineDirection.REVERSED;
		}
		if (isPinAssociated(startAndEnd.getSecond(), pinGraphics)) {
			return GraphicalInlineDirection.NORMAL;
		}
		return GraphicalInlineDirection.IGNORE;
	}

	private boolean isPinAssociated(@NotNull IJoint joint, @NotNull List<IDiagramObject> pins)
	{
		return pins.stream().anyMatch(pin -> joint.isAssociatedWith(pin));
	}

	protected InlineDirection getInlineDirection(boolean isVertical, boolean isReversed)
	{
		if (isVertical) {
			return isReversed ? InlineDirection.TOPDOWN : InlineDirection.DOWNTOP;
		}

		return isReversed ? InlineDirection.RIGHTLEFT : InlineDirection.LEFTRIGHT;
	}

	protected boolean isSegmentNotLongEnough(IConnected segment)
	{
		return segment.getLength() < CHSConstants.PIN_SPACING * 4;
	}

	protected int decideConnectorHalfSize(double segmentSize)
	{
		double pinSpacings = segmentSize / CHSConstants.PIN_SPACING;

		if (pinSpacings > 5) {
			return 2 * CHSConstants.PIN_SPACING;
		}

		return CHSConstants.PIN_SPACING;
	}

	protected void ensurePointSnappedToGrid(Point point)
	{
		point.x -= point.x % CHSConstants.PIN_SPACING;
		point.y -= point.y % CHSConstants.PIN_SPACING;
	}

	private Optional<InlineExtent> getConnectorExtent(@NotNull ISegment segment, boolean isVertical,
			Collection<IConductor> usedConductors, Collection<IConductor> targetConductors, boolean isReversed)
	{
		mDiagram = IBaseDiagram.Statics.getOwningDiagram(segment);
		if (mDiagram == null) {
			throw new IllegalStateException("Segment cannot find an owning diagram");
		}
		final IExtent extent = mDiagram.getExtent();
		// We have to have a context to allow search of graphical objects, this is a SearchGfxContext that will not
		// render any graphics
		final SearchGfxContext context = new SearchGfxContext(mDiagram, extent);

		ILine targetLine = segment;
		Point centerPoint = getCenterPoint(targetLine);
		ensurePointSnappedToGrid(centerPoint);

		// Reset state for the next iteration
		mFirstPreviousRootMulticore = null;
		mSecondPreviousRootMulticore = null;
		IConductor startingConductor = segment.getConductor();
		addSplitConductor(startingConductor);

		int halfSize = decideConnectorHalfSize(targetLine.getLength());
		Optional<ExtensionResult> extensionResult;
		if (isVertical) {
			extensionResult =
					seekVertical(context, startingConductor, halfSize, centerPoint, usedConductors, targetConductors,
							isReversed);
		}
		else {
			extensionResult =
					seekHorizontal(context, startingConductor, halfSize, centerPoint, usedConductors, targetConductors,
							isReversed);
		}
		if (!extensionResult.isPresent()) {
			return Optional.empty();
		}
		ExtensionResult actualResult = extensionResult.get();

		Point startPoint;
		Point endPoint;
		if (isVertical) {
			startPoint = new Point(centerPoint.x - actualResult.getLeftOffset() - CHSConstants.PIN_SPACING,
					centerPoint.y + actualResult.getTopOffset());
			endPoint = new Point(centerPoint.x + actualResult.getRightOffset() + CHSConstants.PIN_SPACING,
					centerPoint.y - actualResult.getBotOffset());
		}
		else {
			startPoint = new Point(centerPoint.x - actualResult.getLeftOffset(),
					centerPoint.y - actualResult.getBotOffset() - CHSConstants.PIN_SPACING);
			endPoint = new Point(centerPoint.x + actualResult.getRightOffset(),
					centerPoint.y + actualResult.getTopOffset() + CHSConstants.PIN_SPACING);
		}

		List<ISmartPoint> result = new ArrayList<>();
		result.add(new SmartPointHelper(startPoint, zeroOffsetPoint, true));
		result.add(new SmartPointHelper(centerPoint, zeroOffsetPoint, true));
		result.add(new SmartPointHelper(endPoint, zeroOffsetPoint, true));

		return Optional.of(new InlineExtent(result, actualResult.getPinPositions(), actualResult.getAddedConductors()));
	}

	// Refactor seekHorizontal and seekVertical to use same code parameterized with functions for the
	// different direction, see SeekOrientation as an example.
	@NotNull private Optional<ExtensionResult> seekHorizontal(IGfxContext context, IConductor startingConductor,
			int initialWidth,
			Point startPoint, Collection<IConductor> usedConductors, Collection<IConductor> targetConductors,
			boolean isReversed)
	{
		int leftOffset = initialWidth;
		int rightOffset = initialWidth;
		if (hasOverlappedSegments(context, startPoint, leftOffset, rightOffset, SeekOrientation.HORIZONTAL)) {
			return Optional.empty();
		}

		Point currentTopPoint = new Point(startPoint.x, startPoint.y + CHSConstants.PIN_SPACING);
		Point currentBotPoint = new Point(startPoint.x, startPoint.y - CHSConstants.PIN_SPACING);

		Collection<IConductor> addedConductors = new HashSet<>();
		addedConductors.add(startingConductor);

		int topChange = CHSConstants.PIN_SPACING;
		int botChange = CHSConstants.PIN_SPACING;
		List<Point> pinPositions = new ArrayList<>();

		IConductor topPreviousUsedConductor = startingConductor;
		IConductor bottomPreviousUsedConductor = startingConductor;
		int topOffset = 0;
		int botOffset = 0;
		while (topChange > 0 || botChange > 0) {
			if (topChange > 0) {
				Collection<IDynamicGfxMediator> objectsAtTop = getObjectsAtPoint(context, currentTopPoint);

				if (validObjects(objectsAtTop, usedConductors, targetConductors, isReversed,
						mFirstPreviousRootMulticore)) {
					ExtensionIncrement leftRightOffsets =
							getValidLeftRightOffsets(currentTopPoint, objectsAtTop);
					if (leftRightOffsets != null) {
						int leftOffsetCandidate = leftRightOffsets.getFirstExtension();
						int rightOffsetCandidate = leftRightOffsets.getSecondExtension();
						topPreviousUsedConductor = leftRightOffsets.getAddedConductor();

						//Check if we can keep the width of connector.
						leftOffset = leftOffsetCandidate < leftOffset ? leftOffsetCandidate : leftOffset;
						rightOffset = rightOffsetCandidate < rightOffset ? rightOffsetCandidate : rightOffset;
						if (hasOverlappedSegments(context, currentTopPoint, leftOffset, rightOffset,
								SeekOrientation.HORIZONTAL)) {
							topChange = 0;
						}
						else {
							//If the set of objects is valid an we have a suitable segment - remember current offset.
							topOffset = currentTopPoint.y - startPoint.y;
							pinPositions.add(new Point(currentTopPoint));
							leftRightOffsets.addConductor(usedConductors);
							addedConductors.add(topPreviousUsedConductor);
						}
					}
				}
				else {
					//If object set is not valid - stop moving this direction
					topChange = 0;
				}
			}

			if (botChange > 0) {
				Collection<IDynamicGfxMediator> objectsAtBot = getObjectsAtPoint(context, currentBotPoint);

				if (validObjects(objectsAtBot, usedConductors, targetConductors, isReversed,
						mSecondPreviousRootMulticore)) {
					ExtensionIncrement leftRightOffsets =
							getValidLeftRightOffsets(currentBotPoint, objectsAtBot);
					if (leftRightOffsets != null) {
						int leftOffsetCandidate = leftRightOffsets.getFirstExtension();
						int rightOffsetCandidate = leftRightOffsets.getSecondExtension();
						bottomPreviousUsedConductor = leftRightOffsets.getAddedConductor();

						//Check if we can keep the width of connector.
						leftOffset = leftOffsetCandidate < leftOffset ? leftOffsetCandidate : leftOffset;
						rightOffset = rightOffsetCandidate < rightOffset ? rightOffsetCandidate : rightOffset;
						if (hasOverlappedSegments(context, currentBotPoint, leftOffset, rightOffset,
								SeekOrientation.HORIZONTAL)) {
							botChange = 0;
						}
						else {
							//If the set of objects is valid an we have a suitable segment - remember current offset.
							botOffset = startPoint.y - currentBotPoint.y;
							pinPositions.add(new Point(currentBotPoint));
							leftRightOffsets.addConductor(usedConductors);
							addedConductors.add(bottomPreviousUsedConductor);
						}
					}
				}
				else {
					//If object set is not valid - stop moving this direction
					botChange = 0;
				}
			}

			currentTopPoint.y += topChange;
			currentBotPoint.y -= botChange;

			if (usedConductors.containsAll(targetConductors)) {
				topChange = 0;
				botChange = 0;
				continue;
			}

			//If the distance traveled from latest successful extension is greater that allowed one - stop moving
			//in that direction
			int maxAllowedConductorSpacing = getMaxAllowedConductorSpacing();
			final int topDelta = currentTopPoint.y - startPoint.y - topOffset;
			if (topDelta > maxAllowedConductorSpacing) {
				mFirstPreviousRootMulticore =
						switchToMulticoreSpacing(mFirstPreviousRootMulticore, topPreviousUsedConductor);
				if (mFirstPreviousRootMulticore == null) {
					topChange = 0;
				}
			}

			final int bottomDelta = startPoint.y - currentBotPoint.y - botOffset;
			if (bottomDelta > maxAllowedConductorSpacing) {
				mSecondPreviousRootMulticore =
						switchToMulticoreSpacing(mSecondPreviousRootMulticore, bottomPreviousUsedConductor);
				if (mSecondPreviousRootMulticore == null) {
					botChange = 0;
				}
			}
		}

		return Optional.of(new ExtensionResult(leftOffset, rightOffset, topOffset, botOffset, pinPositions, addedConductors));
	}

	private Optional<ExtensionResult> seekVertical(IGfxContext context, IConductor startingConductor, int initialHeight,
			Point startPoint, Collection<IConductor> usedConductors, Collection<IConductor> targetConductors,
			boolean isReversed)
	{
		int topOffset = initialHeight;
		int botOffset = initialHeight;
		if (hasOverlappedSegments(context, startPoint, topOffset, botOffset, SeekOrientation.VERTICAL)) {
			return Optional.empty();
		}

		Point currentLeftPoint = new Point(startPoint.x - CHSConstants.PIN_SPACING, startPoint.y);
		Point currentRightPoint = new Point(startPoint.x + CHSConstants.PIN_SPACING, startPoint.y);

		Collection<IConductor> addedConductors = new HashSet<>();
		addedConductors.add(startingConductor);

		int leftChange = CHSConstants.PIN_SPACING;
		int rightChange = CHSConstants.PIN_SPACING;
		List<Point> pinPositions = new ArrayList<>();

		IConductor leftPreviousUsedConductor = startingConductor;
		IConductor rightPreviousUsedConductor = startingConductor;
		int leftOffset = 0;
		int rightOffset = 0;
		while (leftChange > 0 || rightChange > 0) {
			if (leftChange > 0) {
				Collection<IDynamicGfxMediator> objectsAtLeft = getObjectsAtPoint(context, currentLeftPoint);

				if (validObjects(objectsAtLeft, usedConductors, targetConductors, isReversed,
						mFirstPreviousRootMulticore)) {
					ExtensionIncrement topBotOffsets =
							getValidTopBotOffsets(currentLeftPoint, objectsAtLeft);
					if (topBotOffsets != null) {
						int topOffsetCandidate = topBotOffsets.getFirstExtension();
						int downOffsetCandidate = topBotOffsets.getSecondExtension();
						leftPreviousUsedConductor = topBotOffsets.getAddedConductor();

						//Check if we can keep the height of connector.
						topOffset = topOffsetCandidate < topOffset ? topOffsetCandidate : topOffset;
						botOffset = downOffsetCandidate < botOffset ? downOffsetCandidate : botOffset;
						if (hasOverlappedSegments(context, currentLeftPoint, topOffset, botOffset,
								SeekOrientation.VERTICAL)) {
							leftChange = 0;
						}
						else {
							//If the set of objects is valid an we have a suitable segment - remember current offset.
							leftOffset = startPoint.x - currentLeftPoint.x;
							pinPositions.add(new Point(currentLeftPoint));
							topBotOffsets.addConductor(usedConductors);
							addedConductors.add(leftPreviousUsedConductor);
						}
					}
				}
				else {
					//If object set is not valid - stop moving this direction
					leftChange = 0;
				}
			}

			if (rightChange > 0) {
				Collection<IDynamicGfxMediator> objectsAtRight = getObjectsAtPoint(context, currentRightPoint);

				if (validObjects(objectsAtRight, usedConductors, targetConductors, isReversed,
						mSecondPreviousRootMulticore)) {
					ExtensionIncrement topBotOffsets =
							getValidTopBotOffsets(currentRightPoint, objectsAtRight);
					if (topBotOffsets != null) {
						int topOffsetCandidate = topBotOffsets.getFirstExtension();
						int botOffsetCandidate = topBotOffsets.getSecondExtension();
						rightPreviousUsedConductor = topBotOffsets.getAddedConductor();

						//Check if we can keep the height of connector.
						topOffset = topOffsetCandidate < topOffset ? topOffsetCandidate : topOffset;
						botOffset = botOffsetCandidate < botOffset ? botOffsetCandidate : botOffset;
						if (hasOverlappedSegments(context, currentRightPoint, topOffset, botOffset,
								SeekOrientation.VERTICAL)) {
							rightChange = 0;
						}
						else {
							//If the set of objects is valid an we have a suitable segment - remember current offset.
							rightOffset = currentRightPoint.x - startPoint.x;
							pinPositions.add(new Point(currentRightPoint));
							topBotOffsets.addConductor(usedConductors);
							addedConductors.add(rightPreviousUsedConductor);
						}
					}
				}
				else {
					//If object set is not valid - stop moving this direction
					rightChange = 0;
				}
			}

			currentLeftPoint.x -= leftChange;
			currentRightPoint.x += rightChange;

			if (usedConductors.containsAll(targetConductors)) {
				leftChange = 0;
				rightChange = 0;
				continue;
			}

			//If the distance traveled from latest successful extension is greater that allowed one - stop moving
			//in that direction

			int maxGridSpacing = getMaxAllowedConductorSpacing();
			final int leftDelta = startPoint.x - currentLeftPoint.x - leftOffset;
			if (leftDelta > maxGridSpacing) {
				mFirstPreviousRootMulticore =
						switchToMulticoreSpacing(mFirstPreviousRootMulticore, leftPreviousUsedConductor);
				if (mFirstPreviousRootMulticore == null) {
					leftChange = 0;
				}
			}

			final int rightDelta = currentRightPoint.x - startPoint.x - rightOffset;
			if (rightDelta > maxGridSpacing) {
				mSecondPreviousRootMulticore =
						switchToMulticoreSpacing(mSecondPreviousRootMulticore, rightPreviousUsedConductor);
				if (mSecondPreviousRootMulticore == null) {
					rightChange = 0;
				}
			}
		}

		return Optional.of(new ExtensionResult(leftOffset, rightOffset, topOffset, botOffset, pinPositions, addedConductors));
	}

	/**
	 * Are there more than one segments at points defined by 'point' and 'firstOffset' and 'secondOffset' those two
	 * points are the pin positions that would be used if we inserted the inline.
	 * <p>
	 * @param context Graphics context to use to query graphics at a point
	 * @param point 'middle' point of the inline
	 * @param firstOffset offset to the first edge of the inline from 'point'
	 * @param secondOffset offset to the second edge of the inline from 'point'
	 * @param dir Direction of the target segment
	 * @return true if there are overlapping segments
	 */
	private boolean hasOverlappedSegments(@NotNull IGfxContext context, @NotNull Point point, int firstOffset,
			int secondOffset, @NotNull SeekOrientation dir)
	{
		final Pair<Point, Point> pinPositions = getPinPositions(point, firstOffset, secondOffset, dir);
		return hasOverlappedSegments(context, pinPositions.getFirst()) ||
				hasOverlappedSegments(context, pinPositions.getSecond());
	}

	/**
	 * Are there more than one segments at 'point'
	 * <p>
	 * @param context Graphics context to use to query graphics at a point
	 * @param point point to query
	 * @return true if there are overlapping segments
	 */
	private boolean hasOverlappedSegments(@NotNull IGfxContext context, @NotNull Point point)
	{
		final Collection<IDynamicGfxMediator> objectsAtPoint = getObjectsAtPoint(context, point);
		if (objectsAtPoint == null) {
			return false;
		}
		return objectsAtPoint.stream().filter(obj -> obj instanceof ISegment).count() > 1;
	}

	/**
	 * Return the pin positions defined by 'point' and 'firstOffset' and 'secondOffset' that would be used if we
	 * inserted the inline.
	 * <p>
	 * @param point 'middle' point of the inline
	 * @param firstOffset offset to the first edge of the inline from 'point'
	 * @param secondOffset offset to the second edge of the inline from 'point'
	 * @param dir Direction of the target segment
	 * @return first and second pin positions
	 */
	@NotNull private Pair<Point, Point> getPinPositions(@NotNull Point point, int firstOffset, int secondOffset,
			@NotNull SeekOrientation dir)
	{
		return new Pair<Point, Point>(dir.getAdjustedPoint(point, -firstOffset),
				dir.getAdjustedPoint(point, secondOffset));
	}

	/**
	 * Method to check and potentially switch to multicore spacing, which is more generous than the default.
	 * <p>
	 *
	 * @param lastMulticore Multicore of the last conductor that we found by this method
	 * @param lastUsedConductor The last conductor that was split
	 *
	 * @return The multicore of the last conductor or null if we already had a last multicore, used to indicate that we
	 * should now stop increasing the extent
	 */
	@Nullable private IMulticore switchToMulticoreSpacing(@Nullable IMulticore lastMulticore,
			@Nullable IConductor lastUsedConductor)
	{
		if (lastMulticore != null) {
			// If we are already doing multicore we have not switched, this will allow us to stop extending the extent
			return null;
		}
		if (lastUsedConductor != null) {
			final IMulticore rootMulticore = lastUsedConductor.getConnectivity().getRootMulticore();
			if (rootMulticore != null) {
				return rootMulticore;
			}
		}
		return null;
	}

	@Nullable
	private Collection<IDynamicGfxMediator> getObjectsAtPoint(@NotNull IGfxContext context, @NotNull Point point)
	{
		// We are not using a grid based snapping radius so no need to pass in a view as the first param
		@SuppressWarnings("ConstantConditions")
		final int snapRadius = ModelUtils.getSnapRadius(null, false);
		return mModelUtils.getSnapCandidates(context, mDiagram, point, snapRadius,
				CAFUtils.getInstance().getCommonFactory().createExtent());
	}

	private boolean validObjects(@Nullable Collection<IDynamicGfxMediator> objects,
			Collection<IConductor> usedConductors, Collection<IConductor> targetConductors, boolean isReversed,
			@Nullable IMulticore previousRootMulticore)
	{
		if (objects == null || objects.isEmpty()) {
			return true;
		}

		for (IDynamicGfxMediator object : objects) {
			if (!isValidObject(object, usedConductors, targetConductors, isReversed, previousRootMulticore)) {
				return false;
			}
		}
		return true;
	}

	private boolean isValidObject(@NotNull IDynamicGfxMediator object, Collection<IConductor> usedConductors,
			Collection<IConductor> targetConductors, boolean isReversed, @Nullable IMulticore previousRootMulticore)
	{
		if (object instanceof ISegment) {
			ISegment segment = (ISegment) object;
			IConductor conductor = segment.getConductor();
			// Abort if we have already split this conductor or it is not one of the ones we should be splitting or
			// the direction of the plug jack is different to the starting conductor
			boolean invalid = (isInvalidConductor(conductor, usedConductors, targetConductors, previousRootMulticore) ||
					isReversed(segment) != isReversed);
			return !invalid;
		}

		if (object instanceof IShieldBody) {
			return true;
		}

		// Don't get blocked by any decorative or propertied graphic objects
		return object instanceof IDecorative || object instanceof IPropertiedGraphic;
	}

	private boolean isInvalidConductor(IConductor conductor, Collection<IConductor> usedConductors,
			Collection<IConductor> targetConductors, @Nullable IMulticore previousRootMulticore)
	{
		boolean isInvalid = usedConductors.contains(conductor) || !targetConductors.contains(conductor);
		if (isInvalid) {
			return true;
		}
		if (mSplitConductors.contains(getLogicalConductor(conductor))) {
			return true;
		}
		// If it's valid but we are in multicore grid spacing mode then abort if the conductor doesn't belong to the
		// target multicore
		if (previousRootMulticore == null) {
			return false;
		}
		return conductor.getConnectivity().getRootMulticore() != previousRootMulticore;
	}

	@Nullable
	private ISegment retrieveSegment(@Nullable Collection<IDynamicGfxMediator> objects)
	{
		if (objects == null) {
			return null;
		}
		for (IDynamicGfxMediator object : objects) {
			if (object instanceof ISegment) {
				return (ISegment) object;
			}
		}
		return null;
	}

	@Nullable
	private ExtensionIncrement getValidTopBotOffsets(Point point, @Nullable Collection<IDynamicGfxMediator> objects)
	{
		ISegment segment = retrieveSegment(objects);
		if (segment != null) {
			final IJoint start = segment.getStartJoint();
			final IJoint end = segment.getEndJoint();
			if (start == null || end == null) {
				return null;
			}
			int topY;
			int botY;
			if (start.getY() > end.getY()) {
				topY = start.getY();
				botY = end.getY();
			}
			else {
				topY = end.getY();
				botY = start.getY();
			}

			int topOffset = topY - point.y;
			int botOffset = point.y - botY;

			int doubleSpacing = 2 * CHSConstants.PIN_SPACING;
			if (topOffset < doubleSpacing || botOffset < doubleSpacing) {
				return null;
			}

			return new ExtensionIncrement(segment.getConductor(), topOffset, botOffset);
		}

		return null;
	}

	@Nullable
	private ExtensionIncrement getValidLeftRightOffsets(Point point, @Nullable Collection<IDynamicGfxMediator> objects)
	{

		ISegment segment = retrieveSegment(objects);
		if (segment != null) {
			final IJoint start = segment.getStartJoint();
			final IJoint end = segment.getEndJoint();
			if (start == null || end == null) {
				return null;
			}
			int leftX;
			int rightX;
			if (start.getX() < end.getX()) {
				leftX = start.getX();
				rightX = end.getX();
			}
			else {
				leftX = end.getX();
				rightX = start.getX();
			}

			int leftOffset = point.x - leftX;
			int rightOffset = rightX - point.x;

			int doubleSpacing = 2 * CHSConstants.PIN_SPACING;
			if (leftOffset < doubleSpacing || rightOffset < doubleSpacing) {
				return null;
			}

			leftOffset = decideConnectorHalfSize(leftOffset);
			rightOffset = decideConnectorHalfSize(rightOffset);

			return new ExtensionIncrement(segment.getConductor(), leftOffset, rightOffset);
		}

		return null;
	}

	private void addSplitConductor(@NotNull IConductor conductor)
	{
		mSplitConductors.add(getLogicalConductor(conductor));
	}

	@NotNull private INamedUIDObject getLogicalConductor(@NotNull IConductor conductor)
	{
		final chs.cof.logical.cable.IConductor connectivityCond = conductor.getConnectivity();
		final ISharedConductor sharedConductor = connectivityCond.getSharedConductor();
		return sharedConductor != null ? sharedConductor : connectivityCond;
	}

	private Point getCenterPoint(ILine line)
	{
		ILocation startPoint = line.getStartPoint();
		ILocation endPoint = line.getEndPoint();

		int xDiff = startPoint.getX() - endPoint.getX();
		int yDiff = startPoint.getY() - endPoint.getY();

		return new Point(startPoint.getX() - xDiff / 2, startPoint.getY() - yDiff / 2);
	}

	/**
	 * Returns the max diagram spacing allowed between conductors before we decide to create a separate graphical inline
	 * instance.
	 * <p>
	 *
	 * @return Maximum allows space between conductors on a single inline instance
	 */
	private int getMaxAllowedConductorSpacing()
	{
		if (mFirstPreviousRootMulticore != null || mSecondPreviousRootMulticore != null) {
			// Multicore wires spacing cannnot be less than regular spacing
			return Math.max(mMaxGridSpacingForNonMcCond, mMaxGridSpacingForMcCond) * CHSConstants.PIN_SPACING;
		}
		return mMaxGridSpacingForNonMcCond * CHSConstants.PIN_SPACING;
	}

	/**
	 * Gets a value for grid spacing from the provided default or environment variable.
	 * <p>
	 *
	 * @param envVariable Optional environment variable to check for, must be an integer greater than zero
	 * @param defaultValue The value to use if the env var is not set or is not valid
	 *
	 * @return The spacing in number of grids
	 */
	private int getGridSpacingValue(@NotNull String envVariable, int defaultValue)
	{
		int value = defaultValue;
		final String variableOrProperty = Environment.getVariableOrProperty(envVariable);
		if (variableOrProperty != null) {
			value = CommonUtils.parseCount(variableOrProperty);
			if (value <= 0) {
				value = defaultValue;
			}
		}
		return value;
	}

	private static class ExtensionResult
	{

		@NotNull private final List<Point> pinPositions;
		private int leftOffset;
		private int rightOffset;
		private int topOffset;
		private int botOffset;
		private Collection<IConductor> addedConductors;

		private ExtensionResult(int leftOffset, int rightOffset, int topOffset, int botOffset,
				@NotNull List<Point> pinPositions, Collection<IConductor> addedConductors)
		{
			this.leftOffset = leftOffset;
			this.rightOffset = rightOffset;
			this.topOffset = topOffset;
			this.botOffset = botOffset;
			this.pinPositions = pinPositions;
			this.addedConductors = addedConductors;
		}

		public int getLeftOffset()
		{
			return leftOffset;
		}

		public int getRightOffset()
		{
			return rightOffset;
		}

		public int getTopOffset()
		{
			return topOffset;
		}

		public int getBotOffset()
		{
			return botOffset;
		}

		@NotNull public List<Point> getPinPositions()
		{
			return pinPositions;
		}

		@NotNull public Collection<IConductor> getAddedConductors()
		{
			return addedConductors;
		}
	}

	/**
	 * The result of the getValidLeftRightOffsets and getValidTopBotOffsets methods that contains the result of the
	 * increase of extent to encompass another conductor.
	 */
	private class ExtensionIncrement
	{

		private IConductor mAddedConductor;
		private int mFirstExtension;
		private int mSecondExtension;

		private ExtensionIncrement(@NotNull IConductor conductor, int firstExtension, int secondExtension)
		{
			mAddedConductor = conductor;
			mFirstExtension = firstExtension;
			mSecondExtension = secondExtension;
		}

		@NotNull public IConductor getAddedConductor()
		{
			return mAddedConductor;
		}

		public int getFirstExtension()
		{
			return mFirstExtension;
		}

		public int getSecondExtension()
		{
			return mSecondExtension;
		}

		/**
		 * Add the conductor split by this increment to usedConductors and to the logical mSplitConductors.
		 * <p>
		 * @param usedConductors Collection to add graphical conductor to
		 */
		public void addConductor(@NotNull final Collection<IConductor> usedConductors)
		{
			final IConductor addedConductor = getAddedConductor();
			usedConductors.add(addedConductor);
			addSplitConductor(addedConductor);
		}
	}
}
