package chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect;

import chs.common.IUIDObject;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface ISchemObjectsToConnect
{

	/**
	 * @param schemObjectsConnector the connector which is helpful for connecting two objects
	 *
	 * @return true if the connect operation is successful
	 */
	boolean connect(ISchemObjectsConnector schemObjectsConnector);

	/**
	 * Some objects might already have been identified to connect as part of the current connect, these can be ignored
	 * for future connects
	 *
	 * @return the objects which can be ignored for future connect
	 */
	Set<? extends IUIDObject> getObjectsToIgnore();

	/**
	 * objects like floating conductors/highways can be deleted
	 *
	 * @return the objects which can be deleted
	 */
	Set<? extends IUIDObject> getObjectsToDelete();

	/**
	 * add the identified conductors for auto route
	 */
	void addForAutoRoute();

	@NotNull
	Set<? extends IUIDObject> getLeftOverObjects();
}
