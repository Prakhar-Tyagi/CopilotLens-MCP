package chs.caplets.logic.LockobjectsDisplay;

import chs.caf.caplet.helpers.browser.LockedTreeNodeDimmer;
import chs.capitalmanager.appserver.IUserSession;
import chs.capitalmanager.appserver.UserSessionException;
import chs.common.IUID;
import chs.system.FactoryMgr;
import chs.utilities.StringUtils;
import com.mentor.capital.xml.DOMParserService;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

class FindLockedLogicObjects implements Runnable
{

	private IUID designUID;

	private Consumer<Map<IUID, LockedTreeNodeDimmer.LockDetail>> logicObjectsLockedListener;

	FindLockedLogicObjects(IUID designToWorkOnUID, Consumer<Map<IUID, LockedTreeNodeDimmer.LockDetail>> listener)
	{
		designUID = designToWorkOnUID;

		logicObjectsLockedListener = listener;
	}

	@Override public void run()
	{
		//get the logic objects to work on.
		Map<IUID, LockedTreeNodeDimmer.LockDetail> lockedObjects;
		try {
			lockedObjects = getLogicObjectsLockedInOtherSessions(designUID.getString());
			logicObjectsLockedListener.accept(lockedObjects);
		}
		catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		catch (UserSessionException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (SAXException e) {
			e.printStackTrace();
		}
	}

	private Map<IUID, LockedTreeNodeDimmer.LockDetail> getLogicObjectsLockedInOtherSessions(String givenDesignUID)
			throws ParserConfigurationException, UserSessionException, IOException, SAXException
	{

		Map<IUID, LockedTreeNodeDimmer.LockDetail> objectsLockedInOtherSession =
				new HashMap<IUID, LockedTreeNodeDimmer.LockDetail>();
		IUserSession userSession = FactoryMgr.getCHSSystem().getUserSession();
		if (userSession == null) {
			return Collections.emptyMap();
		}
		String lockedLogicObjectsXML = userSession.getLockedLogicObjectsOfADesign(givenDesignUID);
		if (StringUtils.isEmpty(lockedLogicObjectsXML)) {
			return Collections.emptyMap();
		}

		DocumentBuilderFactory factory = DOMParserService.INSTANCE.newDocumentBuilderFactoryXXEAndExternalTDDisabled();
		factory.setValidating(false);
		Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(lockedLogicObjectsXML)));

		if (doc != null) {
			NodeList logicObjectLockInfo = doc.getElementsByTagName("lockobjectdetail");
			for (int i = 0; i < logicObjectLockInfo.getLength(); i++) {
				Node node = logicObjectLockInfo.item(i);
				NamedNodeMap attributes = node.getAttributes();
				String lockedLogicObjectUID = attributes.getNamedItem("id").getNodeValue();
				String sessionId = attributes.getNamedItem("usersession_id").getNodeValue();
				String userName = attributes.getNamedItem("username").getNodeValue();
				String timeStamp = attributes.getNamedItem("timestamp").getNodeValue();
				if (!userSession.getUserSessionID().equals(sessionId)) {
					IUID lockObjectUID = FactoryMgr.getCommonFactory().constructUID(lockedLogicObjectUID);
					objectsLockedInOtherSession
							.put(lockObjectUID, new LockedTreeNodeDimmer.LockDetail(userName, timeStamp));
				}
			}
		}

		return objectsLockedInOtherSession;
	}
}
