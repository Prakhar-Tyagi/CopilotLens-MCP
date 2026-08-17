package chs.caplets.shared;

import chs.caf.caplet.helpers.browser.BrowserFolder;
import chs.cof.COFTypeEnum;
import chs.cof.logical.IDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConductorIterator;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.ILogicObjectIterator;
import chs.cof.logical.cable.IModuleCodeInformationProvider;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IMulticoreIterator;
import chs.cof.logical.cable.IShieldBody;
import chs.common.IProperty;
import chs.common.IReadOnlyNamedObject;
import chs.common.IUID;
import chs.common.UIDObject;
import chs.common.ValueTypeEnum;
import chs.common.attr.IAttribute;
import chs.common.attr.IAttributeProvider;
import chs.common.attr.IAttributeTypes;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.AlphaNumComparator;
import chs.utilities.CommonUtils;
import chs.utilities.ListSet;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GroupAttributeConfigurator extends UIDObject implements IGroupAttributeAddendum, IReadOnlyNamedObject
{

	public static final SeperatorDetails[] SEPERATOR_DETAILS = new SeperatorDetails[0];

	private List<GroupAttributeNamePathModifier> attributeConfigPaths = new ArrayList<>();
	private static AlphaNumComparator<String> comparator = AlphaNumComparator.getCaseSensitiveComparator();
	//private IDesign design = null;

	private Map<IUID, IGroupAttributeConfiguratorNode> nodeForUID = new HashMap<>();

	@Override public Collection<IGroupAttributeAddEntry> getChildAttributesConfigured()
	{
		if (attributeConfigPaths.isEmpty()) {
			return Collections.emptyList();
		}
		return attributeConfigPaths.get(0).getChildAttributesConfigured();
	}

	@Override public boolean setChildAttributes(Collection<IGroupAttributeAddEntry> attributes)
	{
		if (!attributeConfigPaths.isEmpty()) {
			return attributeConfigPaths.get(0).setChildAttributes(attributes);
		}
		addAttributePath(attributes);
		return true;
	}

	@Override public String getName()
	{
		return ResourceMgr.getString(GroupAttributeConfigurator.class, "GroupAttributeConfigurator.addhierarchy.name");
	}

	public void reset()
	{
		nodeForUID.keySet().forEach(aUID -> UIDMgr.removeObject(aUID));
		nodeForUID = new HashMap<>();
		attributeConfigPaths.forEach(aPath -> aPath.reset());
	}

	protected Map<IUID, IGroupAttributeConfiguratorNode> getNodeForUID()
	{
		return nodeForUID;
	}

	private static class SeperatorDetails
	{

		private String valueBeforeSplit;
		private String valueBetweenSuccesiveSplits;
		private String preceedingSeperator;

		SeperatorDetails(@NotNull String valueBetweenSuccesiveSplits, @Nullable String valueBeforeSplit,
				@Nullable String lastSeperator)
		{
			this.valueBeforeSplit = valueBeforeSplit;
			this.valueBetweenSuccesiveSplits = valueBetweenSuccesiveSplits;
			preceedingSeperator = lastSeperator;
		}

		String getTreeDisplayValue()
		{
			if (StringUtils.isBlank(valueBetweenSuccesiveSplits)) {
				return ResourceMgr.getString(GroupAttributeConfigurator.class, "GroupAttributeConfigurator.blank.text");
			}
			return valueBetweenSuccesiveSplits;
		}

		String getAttributeValue()
		{
			if (StringUtils.isBlank(valueBeforeSplit)) {
				return "";
			}
			return valueBeforeSplit;
		}

		String getPreceedingSeperator()
		{
			return preceedingSeperator;
		}
	}

	private static SeperatorDetails[] splitAttributeBasedOnSeparators(@NotNull ILogicObject logicObject,
			@NotNull IGroupAttributeAddEntry attrOrPropEntry, @Nullable String seperatorsPattern)
	{
		AttrInfoHolder attribute = getAttribute(logicObject, attrOrPropEntry);
		IProperty property =
				attrOrPropEntry.isAttribute() ? null : logicObject.findPropertyByName(attrOrPropEntry.getName());

		String attributeDisplayValue =
				ResourceMgr.getString(GroupAttributeConfigurator.class, "GroupAttributeConfigurator.blank.text");
		SeperatorDetails[] splitAttributeValues;
		if (attribute != null || property != null) {

			String evaluatedAttributeValue = attribute != null ? attribute.getAsString() : property.getAsString();
			if (!StringUtils.isEmpty(evaluatedAttributeValue)) {
				attributeDisplayValue = evaluatedAttributeValue;
			}
			if (seperatorsPattern != null && !StringUtils.isEmpty(evaluatedAttributeValue) &&
					((attribute != null && ValueTypeEnum.TypeString.equals(attribute.getType())) ||
							(property != null && property.getType().equals(ValueTypeEnum.TypeString)))) {

				Pattern pattern = Pattern.compile(seperatorsPattern);
				Matcher matcher = pattern.matcher(evaluatedAttributeValue);
				List<SeperatorDetails> splitSeperators = new ArrayList<>();
				int index = 0;
				String preceedingSeperator = null;
				while (matcher.find()) {
					SeperatorDetails seperatorDetails =
							new SeperatorDetails(attributeDisplayValue.substring(index, matcher.start()),
									attributeDisplayValue.substring(0, matcher.start()), preceedingSeperator);
					preceedingSeperator = attributeDisplayValue.substring(matcher.start(), matcher.end());
					index = matcher.end();
					splitSeperators.add(seperatorDetails);
				}
				if (index < attributeDisplayValue.length()) {

					if (index == 0) { //avoin substring operation.
						splitSeperators.add(new SeperatorDetails(attributeDisplayValue,
								evaluatedAttributeValue, null));
					}
					else {
						splitSeperators.add(new SeperatorDetails(attributeDisplayValue.substring(index),
								evaluatedAttributeValue, preceedingSeperator));
					}
				}
				splitAttributeValues = splitSeperators.toArray(SEPERATOR_DETAILS);
			}
			else {
				splitAttributeValues =
						new SeperatorDetails[]{
								new SeperatorDetails(attributeDisplayValue, evaluatedAttributeValue, null)};
			}
		}
		else {
			splitAttributeValues = new SeperatorDetails[]{new SeperatorDetails(attributeDisplayValue, null, null)};
		}
		return splitAttributeValues;
	}

	@Nullable private static AttrInfoHolder getAttribute(@NotNull ILogicObject logicObject, @NotNull IGroupAttributeAddEntry attrOrPropEntry)
	{
		if (!attrOrPropEntry.isAttribute()) {
			return null;
		}
		IAttribute attribute = logicObject.getAttribute(attrOrPropEntry.getName());
		if (attribute != null) {
			return new AttrInfoHolder(attribute);
		}

		IModuleCodeInformationProvider informationProvider = CommonUtils.cast(logicObject, IModuleCodeInformationProvider.class);
		if (IAttributeTypes.MODULE_CODE.equals(attrOrPropEntry.getName()) && informationProvider!=null) {
			return new ModuleCodeInfoHolder(informationProvider);
		}
		return null;
	}

	@Nullable private static String createSeparatorPatter(Collection<String> seperators)
	{
		String seperatorsPattern;
		if (seperators == null || seperators.isEmpty()) {
			seperatorsPattern = null;
		}
		else {
			StringJoiner joiner = new StringJoiner("|", "", "");

			for (String aSeperator : seperators) {
				joiner.add(FactoryMgr.getRegExUtils().escapeSpecialCharacters(aSeperator));
			}
			seperatorsPattern = joiner.toString();
		}
		return seperatorsPattern;
	}

	private static void defaultSort(List<? extends IReadOnlyNamedObject> sortedValueNodes)
	{
		Collections.sort(sortedValueNodes, new Comparator<IReadOnlyNamedObject>()
		{
			@Override public int compare(IReadOnlyNamedObject o1,
					IReadOnlyNamedObject o2)
			{
				String blankValue = ResourceMgr
						.getString(GroupAttributeConfigurator.class, "GroupAttributeConfigurator.blank.text");
				if (blankValue.equals(o1.getName())) {
					return 1;
				}
				if (blankValue.equals(o2.getName())) {
					return -1;
				}
				return comparator.compare(o1.getName(), o2.getName());
			}
		});
	}

	public static class GroupAttributeConfiguratorAttrNameNode implements IGroupAttributeConfiguratorAttrNameNode
	{

		private GroupAttributeNamePathModifier pathModifier;

		protected GroupAttributeConfiguratorAttrNameNode child;
		@NotNull protected IGroupAttributeAddEntry attributeName;
		protected Map<String, GroupAttributeConfiguratorAttrValueNode> attributeValueNodes;
		protected IGroupAttributeConfiguratorNode parent;
		protected String seperatorsPattern;
		protected Supplier<Collection<ILogicObject>> logicObjectsProvider = null;

		GroupAttributeConfiguratorAttrNameNode(@NotNull GroupAttributeConfiguratorAttrNameNode otherChild)
		{

			attributeName = otherChild.attributeName;

			parent = otherChild.parent;

			if (otherChild.child != null) {
				child = new GroupAttributeConfiguratorAttrNameNode(otherChild.child);
			}
			pathModifier = otherChild.pathModifier;
			seperatorsPattern = otherChild.seperatorsPattern;
		}

		GroupAttributeConfiguratorAttrNameNode(@NotNull IGroupAttributeAddEntry attributeName,
				GroupAttributeNamePathModifier pathModifier)
		{
			this.attributeName = attributeName;
			this.pathModifier = pathModifier;
		}

		void setSeperatorsPattern(Collection<String> seperators)
		{
			seperatorsPattern = createSeparatorPatter(seperators);
		}

		@Override public IGroupAttributeConfiguratorNode getParent()
		{
			return parent;
		}

		public void setLogicObjectsProvider(
				Supplier<Collection<ILogicObject>> logicObjectsProvider)
		{
			this.logicObjectsProvider = logicObjectsProvider;
		}

		@Override public List<IUID> getChildNodes(
				Map<IUID, IGroupAttributeConfiguratorNode> nodes)
		{

			if (attributeValueNodes == null) {
				ILogicObjectIterator logicObjectIterator =
						FactoryMgr.getCableFactory().createLogicObjectIterator(logicObjectsProvider.get());
				attributeValueNodes = new LinkedHashMap<>();
				addLogicObjects(logicObjectIterator, nodes);
			}

			List<GroupAttributeConfiguratorAttrValueNode> sortedValueNodes =
					new ArrayList<>(attributeValueNodes.values());

			defaultSort(sortedValueNodes);
			List<IUID> childUIDs = new ArrayList<>();
			sortedValueNodes.forEach(aChild -> {
				nodes.put(aChild.getUID(), aChild);
				childUIDs.add(aChild.getUID());
			});

			return childUIDs;
		}

		@Nullable public String getAttributeName()
		{
			return attributeName.getName();
		}

		public boolean isAttribute()
		{
			return attributeName.isAttribute();
		}

		List<IGroupAttributeAddEntry> getAttributeTailPath()
		{
			int index = 0;
			String thisAttributeName = getAttributeName();
			for (IGroupAttributeAddEntry anAttributeName : pathModifier.getListOfAttributes()) {
				if (anAttributeName.getName().equals(thisAttributeName)) {
					break;
				}
				index++;
			}
			List<IGroupAttributeAddEntry> tailPart = new ArrayList<>(pathModifier.getListOfAttributes().size());
			for (; index < pathModifier.getListOfAttributes().size(); index++) {

				tailPart.add(pathModifier.getListOfAttributes().get(index));
			}
			return tailPart;
		}

		@Nullable public String getAttributeDisplayName()
		{
			return attributeName.getDisplayName();
		}

		@NotNull private ILogicObjectIterator addLogicObjects(ILogicObjectIterator logicObjectsInDesign,
				Map<IUID, IGroupAttributeConfiguratorNode> nodes)
		{
			Map<GroupAttributeConfiguratorAttrValueNode, Collection<ILogicObject>> logicObjectsInNode = new HashMap<>();
			while (logicObjectsInDesign.hasNext()) {
				ILogicObject aLogicObject = logicObjectsInDesign.next();
				if (aLogicObject instanceof IConductor && ((IConductor) aLogicObject).getMulticore() != null) {
					continue;
				}
				if (aLogicObject instanceof IMulticore && ((IMulticore) aLogicObject).getParent() != null) {
					continue;
				}
				GroupAttributeConfiguratorAttrValueNode parentNode = addAttributevalueChild(aLogicObject, nodes);
				Collection<ILogicObject> logicObjectsForNode =
						logicObjectsInNode.computeIfAbsent(parentNode, aNode -> new ArrayList<>());
				logicObjectsForNode.add(aLogicObject);
			}

			for (GroupAttributeConfiguratorAttrValueNode aChildAttributeValueNode : logicObjectsInNode.keySet()) {

				aChildAttributeValueNode.addLogicObjectWithValue(logicObjectsInNode.get(aChildAttributeValueNode));
				if (child != null) {
					aChildAttributeValueNode.addAttributeNameChild(child);
				}
			}

			return logicObjectsInDesign;
		}

		@Nullable GroupAttributeConfiguratorAttrValueNode addAttributevalueChild(@NotNull ILogicObject logicObject,
				Map<IUID, IGroupAttributeConfiguratorNode> nodes)
		{

			SeperatorDetails[] splitAttributeValues;
			if (GroupingByAttributesTree.ObjectTypeAttribute.equals(attributeName.getName())) {
				String type = COFTypeEnum.getDisplayableTypeName(logicObject);
				splitAttributeValues = new SeperatorDetails[]{new SeperatorDetails(type, type, null)};
			}
			else {
				splitAttributeValues = splitAttributeBasedOnSeparators(logicObject, attributeName, seperatorsPattern);
			}
			return addAttributeValueChildChain(splitAttributeValues, 0, nodes);
		}

		private GroupAttributeConfiguratorAttrValueNode addAttributeValueChildChain(SeperatorDetails[] seperateValues,
				int index,
				Map<IUID, IGroupAttributeConfiguratorNode> nodes)
		{
			GroupAttributeConfiguratorAttrValueNode attributeValueNode;
			SeperatorDetails aSplitValue = seperateValues[index];
			if (!attributeValueNodes.containsKey(seperateValues[index].getAttributeValue())) {

				attributeValueNode =
						new GroupAttributeConfiguratorAttrValueNode(aSplitValue, nodes, this);
				attributeValueNodes.put(aSplitValue.getAttributeValue(), attributeValueNode);
			}
			attributeValueNode = attributeValueNodes.get(aSplitValue.getAttributeValue());
			if (index + 1 < seperateValues.length) {
				return attributeValueNode.addSuffixAttributeValueChild(seperateValues, index + 1, nodes);
			}
			return attributeValueNode;
		}

		GroupAttributeConfiguratorAttrNameNode addAttributeNameChild(@NotNull IGroupAttributeAddEntry givenAttributeName,
				GroupAttributeNamePathModifier groupAttributeNamePathModifier)
		{

			child = new GroupAttributeConfiguratorAttrNameNode(givenAttributeName, groupAttributeNamePathModifier);
			child.parent = this;
			return child;
		}
	}

	public static class GroupAttributeConfiguratorAttrRootNode extends GroupAttributeConfiguratorAttrNameNode
	{

		GroupAttributeConfiguratorAttrRootNode(@NotNull IGroupAttributeAddEntry attributeName,
				GroupAttributeNamePathModifier pathModifier)
		{
			super(attributeName, pathModifier);
		}
	}

	private static class GroupAttributeNamePathModifier
	{

		private List<IGroupAttributeAddEntry> listOfAttributes = new ArrayList<>();
		private GroupAttributeConfiguratorAttrRootNode rootNode;

		GroupAttributeNamePathModifier(Collection<IGroupAttributeAddEntry> listOfAttributes)
		{
			this.listOfAttributes.addAll(listOfAttributes);
			buildTree();
		}

		void buildTree()
		{
			if (listOfAttributes.isEmpty()) {
				return;
			}
			IGroupAttributeAddEntry rootAttr = listOfAttributes.get(0);

			rootNode =
					new GroupAttributeConfiguratorAttrRootNode(rootAttr, this);
			rootNode.setSeperatorsPattern(rootAttr.seperator());

			GroupAttributeConfiguratorAttrNameNode previousNode = rootNode;
			for (int i = 1; i < listOfAttributes.size(); i++) {

				previousNode =
						previousNode.addAttributeNameChild(listOfAttributes.get(i), this);
				previousNode.setSeperatorsPattern(listOfAttributes.get(i).seperator());
			}
		}

		boolean setChildAttributes(Collection<IGroupAttributeAddEntry> attributes)
		{
			//make sure that the list of attributes are different, before reconstructing.
			listOfAttributes.clear();
			listOfAttributes.addAll(attributes);
			buildTree();
			return true;
		}

		public void reset()
		{
			rootNode = null;
		}

		Collection<IGroupAttributeAddEntry> getChildAttributesConfigured()
		{
			return Collections.unmodifiableCollection(listOfAttributes);
		}

		public GroupAttributeConfiguratorAttrRootNode getRootNode()
		{
			if (rootNode == null) {
				buildTree();
			}
			return rootNode;
		}

		protected List<IGroupAttributeAddEntry> getListOfAttributes()
		{
			return listOfAttributes;
		}
	}

	public static class GroupAttributeConfiguratorAttrValueNode extends BrowserFolder
			implements IGroupedAttributeModifier, IGroupAttributeConfiguratorNode
	{

		protected GroupAttributeConfiguratorAttrNameNode child;
		protected SeperatorDetails attributeNameOrValue;
		protected Collection<IUID> logicObjects;
		protected IGroupAttributeConfiguratorNode parent;
		protected Map<String, GroupAttributeConfiguratorAttrValueNode> seperatorSuffixes = new LinkedHashMap<>(1);

		GroupAttributeConfiguratorAttrValueNode(SeperatorDetails seperatorDetails,
				Map<IUID, IGroupAttributeConfiguratorNode> nodes, IGroupAttributeConfiguratorNode parent)

		{
			super(seperatorDetails.getTreeDisplayValue());
			this.parent = parent;
			UIDMgr.addObject(this);
			nodes.put(getUID(), this);
			attributeNameOrValue = seperatorDetails;
		}

		@Nullable @Override public IGroupAttributeConfiguratorNode getParent()
		{
			return parent;
		}

		@Override public List<IUID> getChildNodes(
				Map<IUID, IGroupAttributeConfiguratorNode> nodes)
		{

			List<GroupAttributeConfiguratorAttrValueNode> valueNodes =
					new ArrayList<>(seperatorSuffixes.values());
			defaultSort(valueNodes);

			List<IUID> childUIDs = new ArrayList<>();
			childUIDs.addAll(valueNodes.stream().map(aSeperator -> aSeperator.getUID())
					.collect(Collectors.toList()));
			if (child != null && logicObjects != null && !logicObjects.isEmpty()) {

				Collection<ILogicObject> logicObjectsInNode = logicObjects.stream().map(aUID -> UIDMgr.getObject(aUID))
						.filter(aUIDObject -> aUIDObject instanceof ILogicObject)
						.map(aLogicObject -> (ILogicObject) aLogicObject).collect(Collectors.toList());
				child.setLogicObjectsProvider(() -> {
					return logicObjectsInNode;
				});
				Collection<IUID> attributeChildren = child.getChildNodes(
						nodes);

				childUIDs.addAll(attributeChildren);
			}
			else {
				if (logicObjects != null) {
					List<IUID> sortedLogicObjects = new ArrayList<>(logicObjects);
					Collections.sort(sortedLogicObjects, new Comparator<IUID>()
					{
						@Override public int compare(IUID o1, IUID o2)
						{
							IReadOnlyNamedObject o1Name = UIDMgr.getObjectOfType(o1, IReadOnlyNamedObject.class);
							IReadOnlyNamedObject o2Name = UIDMgr.getObjectOfType(o2, IReadOnlyNamedObject.class);
							return o1Name == null || o2Name == null ? 0 :
									comparator.compare(o1Name.getName(), o2Name.getName());
						}
					});

					childUIDs.addAll(sortedLogicObjects);
				}
			}
			return childUIDs;
		}

		@Nullable public String getAttributeName()
		{
			IGroupAttributeConfiguratorNode other = parent;
			while (true) {
				if (other instanceof GroupAttributeConfiguratorAttrNameNode) {
					return other.getAttributeName();
				}
				if (other != null && other.getParent() != null) {
					other = other.getParent();
					continue;
				}
				return null;
			}
		}

		public boolean isAttribute()
		{
			IGroupAttributeConfiguratorNode other = parent;
			while (true) {
				if (other instanceof GroupAttributeConfiguratorAttrNameNode) {
					return other.isAttribute();
				}
				if (other != null && other.getParent() != null) {
					other = other.getParent();
					continue;
				}
				return true;
			}
		}

		@Nullable public String getAttributeDisplayName()
		{
			IGroupAttributeConfiguratorNode other = parent;
			while (true) {
				if (other instanceof GroupAttributeConfiguratorAttrNameNode) {
					return other.getAttributeDisplayName();
				}
				if (other != null && other.getParent() != null) {
					other = other.getParent();
					continue;
				}
				return null;
			}
		}

		GroupAttributeConfiguratorAttrNameNode addAttributeNameChild(
				@NotNull GroupAttributeConfiguratorAttrNameNode attributeNameNode)
		{
			child = new GroupAttributeConfiguratorAttrNameNode(attributeNameNode);
			return child;
		}

		protected void addLogicObjectWithValue(Collection<ILogicObject> givenLogicObjects)
		{

			logicObjects = new LinkedHashSet<>(givenLogicObjects.size());
			for (ILogicObject aLogicObject : givenLogicObjects) {

				logicObjects.add(aLogicObject.getUID());
			}
		}

		GroupAttributeConfiguratorAttrValueNode addSuffixAttributeValueChild(SeperatorDetails[] seperateValues,
				int index,
				Map<IUID, IGroupAttributeConfiguratorNode> nodes)
		{
			GroupAttributeConfiguratorAttrValueNode attributeValueNode;
			SeperatorDetails aSplitValueObject = seperateValues[index];
			if (!seperatorSuffixes.containsKey(aSplitValueObject.getAttributeValue())) {

				attributeValueNode =
						new GroupAttributeConfiguratorAttrValueNode(aSplitValueObject, nodes, this);
				seperatorSuffixes.put(aSplitValueObject.getAttributeValue(), attributeValueNode);
			}
			attributeValueNode = seperatorSuffixes.get(aSplitValueObject.getAttributeValue());
			if (index + 1 < seperateValues.length) {
				return attributeValueNode
						.addSuffixAttributeValueChild(seperateValues, index + 1, nodes);
			}
			return attributeValueNode;
		}

		@Override public List<String> getExpectedChildAttributeValue(@NotNull ILogicObject logicObject)
		{
			if (child != null) {
				List<String> toBeExpandedPaths = new ArrayList<>();
				List<IGroupAttributeAddEntry> attributeTail = child.getAttributeTailPath();
				for (IGroupAttributeAddEntry anEntry : attributeTail) {

					if (GroupingByAttributesTree.ObjectTypeAttribute.equals(anEntry.getName())) {
						String type = COFTypeEnum.getDisplayableTypeName(logicObject);

						toBeExpandedPaths.add(type);
						return toBeExpandedPaths;
					}
					IAttribute childAttribute = logicObject.getAttribute(anEntry.getName());
					if (childAttribute != null || !anEntry.isAttribute()) {
						String separatorPattern = createSeparatorPatter(anEntry.seperator());
						SeperatorDetails[] seperatorDetails =
								splitAttributeBasedOnSeparators(logicObject, anEntry, separatorPattern);

						for (SeperatorDetails aSeperatorDetail : seperatorDetails) {
							toBeExpandedPaths.add(aSeperatorDetail.getTreeDisplayValue());
						}
					}
				}
				return toBeExpandedPaths;
			}
			return Collections.emptyList();
		}

		@Override
		public boolean isAcceptable(IGroupedAttributeModifier transferingFolderNode)
		{
			if (transferingFolderNode instanceof GroupAttributeConfiguratorAttrValueNode) {
				if (((GroupAttributeConfiguratorAttrValueNode) transferingFolderNode).attributeNameOrValue
						.getPreceedingSeperator() != null) {
					return StringUtils
							.equals(getAttributeName(), ((IGroupAttributeConfiguratorNode) transferingFolderNode)
									.getAttributeName());
				}
				else {
					if (child != null) {
						return StringUtils.equals(child.getAttributeName(),
								((IGroupAttributeConfiguratorNode) transferingFolderNode).getAttributeName());
					}
				}
			}
			return false;
		}

		@Nullable public Pair<String, String> getAttributeNameValue()
		{
			String attributeName = getAttributeName();
			return (attributeName != null ?
					new Pair<String, String>(attributeName, attributeNameOrValue.getAttributeValue()) : null);
		}

		public Collection<IUID> recursiveGetAllChildObjects()
		{
			List<IUID> requiredUIDs = new ArrayList<>();
			if (logicObjects != null) {
				requiredUIDs.addAll(logicObjects);
			}
			for (GroupAttributeConfiguratorAttrValueNode uidsUnderSep : seperatorSuffixes.values()) {
				requiredUIDs.addAll(uidsUnderSep.recursiveGetAllChildObjects());
			}
			return requiredUIDs;
		}

		@Override
		public <T extends IAttributeProvider> Map<String, Collection<T>> getTrailingValueForAttributeBelowThisLevelInTree(
				Class<T> classType)
		{
			Map<String, Collection<T>> objectsWithAGivenValue = new LinkedHashMap<>();
			Collection<IUID> requiredUIDs = recursiveGetAllChildObjects();
			String attributeName = getAttributeName();
			for (IUID aUID : requiredUIDs) {
				T attributeProvider = UIDMgr.getObjectOfType(aUID, classType);
				if (attributeProvider != null && attributeName != null) {
					IAttribute attribute = attributeProvider.getAttribute(attributeName);
					String actualAttributeValue =
							(attribute != null && attribute.getAsString() != null ? attribute.getAsString() : "");
					String trailingAttibuteValue = "";
					if (actualAttributeValue.length() > attributeNameOrValue.getAttributeValue().length()) {
						trailingAttibuteValue =
								actualAttributeValue.substring(attributeNameOrValue.getAttributeValue().length());
					}
					String reqAttibuteValue =
							attributeNameOrValue.getPreceedingSeperator() +
									attributeNameOrValue.getTreeDisplayValue() + trailingAttibuteValue;
					Collection<T> attributeProviders =
							objectsWithAGivenValue.computeIfAbsent(reqAttibuteValue, it -> new ArrayList<>());
					attributeProviders.add(attributeProvider);
				}
			}

			return objectsWithAGivenValue;
		}
	}

	protected interface IGroupAttributeConfiguratorAttrNameNode extends IGroupAttributeConfiguratorNode
	{

		void setLogicObjectsProvider(
				Supplier<Collection<ILogicObject>> logicObjectsProvider);
	}

	protected interface IGroupAttributeConfiguratorNode
	{

		String getAttributeName();

		String getAttributeDisplayName();

		boolean isAttribute();

		IGroupAttributeConfiguratorNode getParent();

		List<IUID> getChildNodes(
				Map<IUID, IGroupAttributeConfiguratorNode> nodes);
	}

	void addAttributePath(Collection<IGroupAttributeAddEntry> givenAttributePath)
	{

		GroupAttributeNamePathModifier groupAttributeNamePathModifier =
				new GroupAttributeNamePathModifier(givenAttributePath);

		attributeConfigPaths.add(groupAttributeNamePathModifier);
	}

	Collection<IGroupAttributeConfiguratorAttrNameNode> getAttributeConfiguratorNodes()
	{

		Collection<IGroupAttributeConfiguratorAttrNameNode> rootNodes = new ArrayList<>();
		attributeConfigPaths.forEach(aPath -> rootNodes.add(aPath.getRootNode()));
		return rootNodes;
	}

	@Nullable public String getTooltipText(IUID uid)
	{
		IGroupAttributeConfiguratorNode node = nodeForUID.get(uid);
		if (node != null) {
			//for leaf logic objects there will be no child elements
			return node.getAttributeDisplayName();
		}
		return null;
	}

	List<IUID> getChildren(IUID uid)
	{
		IGroupAttributeConfiguratorNode node = nodeForUID.get(uid);
		if (node == null) {
			IMulticore multicoreNode = UIDMgr.getObjectOfType(uid, IMulticore.class);
			if (multicoreNode != null) {
				return getMulticoreChildren(multicoreNode);
			}
			//for leaf logic objects there will be no child elements
			return Collections.emptyList();
		}
		return node.getChildNodes(nodeForUID);
	}

	@NotNull private List<IUID> getMulticoreChildren(IMulticore mc)
	{
		List<IUID> vec = new ListSet<>();

		// any MC has a single child node for the connectivity shield body
		IShieldBody sb = mc.getShieldBody();
		if (sb != null) {
			vec.add(sb.getUID());
		}
		// if connectivity exists for a part of a MC, show that instead of the MC
		for (IMulticoreIterator it = mc.getMulticores(); it.hasNext(); ) {
			IMulticore child = it.getNext();
			vec.add(child.getUID());
			vec.remove(child.getInnercoreRef());
		}

		for (IConductorIterator it = mc.getConductorsIncludingShields(); it.hasNext(); ) {
			IConductor child = it.getNext();
			vec.add(child.getUID());
			vec.remove(child.getInnercoreRef());
		}
		return vec;
	}

	List<IUID> getChildrenOfRoot(IGroupAttributeConfiguratorAttrNameNode node, IDesign givenDesign)
	{

		if (node == null) {
			//for leaf logic objects there will be no child elements
			return Collections.emptyList();
		}

		if (node instanceof GroupAttributeConfiguratorAttrRootNode) {

			ILogicObjectIterator logicObjects =
					givenDesign.getConnectivity() != null ? givenDesign.getConnectivity().getObjects() : null;
			if (logicObjects == null) {
				return Collections.emptyList();
			}
			Collection<ILogicObject> logicObjectUIDs =
					logicObjects.stream().collect(Collectors.toList());

			node.setLogicObjectsProvider(() -> logicObjectUIDs);
			return node.getChildNodes(nodeForUID);
		}
		return Collections.emptyList();
	}

	public void destroy()
	{
		nodeForUID.keySet().forEach(aUID -> UIDMgr.removeObject(aUID));
		nodeForUID.clear();

		attributeConfigPaths.clear();
	}

	private static class AttrInfoHolder
	{

		@Nullable private IAttribute attribute;

		AttrInfoHolder(@Nullable IAttribute attribute)
		{
			this.attribute = attribute;
		}

		@Nullable protected String getAsString()
		{
			return attribute != null ? attribute.getAsString() : null;
		}

		@Nullable protected ValueTypeEnum getType()
		{
			return attribute != null ? attribute.getType() : null;
		}
	}

	private static class ModuleCodeInfoHolder extends AttrInfoHolder
	{

		private final IModuleCodeInformationProvider informationProvider;

		private ModuleCodeInfoHolder(@NotNull IModuleCodeInformationProvider informationProvider)
		{
			super(null);
			this.informationProvider = informationProvider;
		}

		@Nullable @Override protected String getAsString()
		{
			return informationProvider.getModuleCode();
		}

		@NotNull @Override protected ValueTypeEnum getType()
		{
			return ValueTypeEnum.TypeString;
		}
	}
}
