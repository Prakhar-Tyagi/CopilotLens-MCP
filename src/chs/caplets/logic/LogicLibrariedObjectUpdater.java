/*
 * Copyright 2002-2015 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic;

import chs.caf.cafmain.actions.CAFCommandHelper;
import chs.caf.caplet.cmd.BaseLibrariedObjectUpdater;
import chs.caf.caplet.helpers.MulticoreLibraryHelper;
import chs.caplets.logic.commands.AssociateLibraryPartCommand;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.library.ILibrariedObject;
import chs.cof.library.ILibrariedObjectUpdater;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAssembly;
import chs.cof.logical.cable.IAssemblyIterator;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.ILogicOtherComponent;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IOverbraid;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemOtherComponent;
import chs.cof.logical.schem.IShareableDiagramObjectWithMultipleRepresentation;
import chs.cof.logical.schem.IShieldBody;
import chs.cof.parts.ILibraryBaseObject;
import chs.cof.parts.ILibraryInnerCore;
import chs.cof.parts.ILibraryMultiWireCore;
import chs.cof.parts.ILibraryMulticore;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.ILibrarySingleWireCore;
import chs.cof.parts.partselector.ILibraryInnercoreComparator;
import chs.cof.project.IProject;
import chs.cof.symbol.ISymboledObject;
import chs.cofUtils.cmd.CommandContext;
import chs.common.IBOMObject;
import chs.common.ICHSIterator;
import chs.common.IPreferenceMgr;
import chs.common.IProjectPreferenceMgr;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.PreferenceContext;
import chs.subsystem.structure.StructureMatchService;
import chs.system.UIDMgr;
import chs.utilities.CommonUtils;
import chs.utility.helpers.LibraryAssignmentHelper;
import chs.utility.helpers.LibraryHelper;
import chs.utility.helpers.LibraryPartProjectUsageValidityReporter;
import chs.utility.helpers.ReferenceHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class LogicLibrariedObjectUpdater extends BaseLibrariedObjectUpdater
{

	public LogicLibrariedObjectUpdater()
	{
		setPreserveIncludeOnBOM(false);
	}

	protected boolean isStrictMatch(@NotNull IMulticore rootMc)
	{
		IProject project = rootMc.getProject();
		IProjectPreferenceMgr preferenceMgr = project != null ? project.getPreferences() : null;
		if (preferenceMgr != null) {
			PreferenceContext preferenceContext = PreferenceContext.determineContext(rootMc.getDesignContainer());
			String matchCriteria = preferenceMgr.getMulticoreStructureMatchCriteria(preferenceContext);
			return ILibraryInnercoreComparator.InnercoreCompareEnum.EXACT.match(matchCriteria);
		}
		return super.isStrictMatch(rootMc);
	}

	/**
	 * @see ILibrariedObjectUpdater#getObjectsToUpdate(IBaseDiagram)
	 */
	@NotNull public Set<IUID> getObjectsToUpdate(@NotNull final IBaseDiagram diagram)
	{
		Set<IUID> objects = new LinkedHashSet<IUID>();
		List<IRepresentedObject> diagramObjects = new ArrayList<IRepresentedObject>();
		diagram.getAllObjects(IRepresentedObject.class, diagramObjects);
		for (IRepresentedObject diagramObject : diagramObjects) {

			if (diagramObject != null) {
				//dts0101117092
				//As there is no schem representation of Assembly, BULP does not work for Assembly
				//so explicitly adding the Assemblies as well to the list as below :
				//check if the object is part of Assembly -> then add Assembly to the list
				IUIDObject uidObject = diagramObject.getRawConnectivity();
				if (uidObject instanceof ILogicObject) {
					ILogicObject sourceObject = (ILogicObject) uidObject;
					if (sourceObject.getAssembly() != null) {
						objects.add(sourceObject.getAssembly().getUID());
					}
				}

				uidObject = CommonUtils.cast(diagramObject, IUIDObject.class);
				if (uidObject != null) {
					//dts0100794823
					//As there is no schem representation of Backshell, BULP does not work for BackShell
					//so explicitly adding the BackShell as well to the list as below :
					//check if the object is connector && having backshell -> then add to the list
					boolean bAddDiagramObject = true;
					IUID relatedObjectUIDToAdd = null;
					if (uidObject instanceof IRepresentedObject) {
						IConnector connector =
								CommonUtils.cast(ReferenceHelper.reduceToLogicObject(uidObject), IConnector.class);
						if (connector != null) {
							IBackshell backshell = connector.getBackshell();
							if (backshell != null) {
								relatedObjectUIDToAdd = backshell.getUID();
							}
						}

						if (relatedObjectUIDToAdd == null) {
							IShieldConductor shieldConductor = CommonUtils
									.cast(ReferenceHelper.reduceToLogicObject(uidObject), IShieldConductor.class);
							if (shieldConductor != null) {
								relatedObjectUIDToAdd = shieldConductor.getUID();
								bAddDiagramObject = false; // No need to add diagram object
							}
						}

						if (relatedObjectUIDToAdd == null) {
							chs.cof.logical.cable.IShieldBody shieldBody =
									CommonUtils.cast(ReferenceHelper.reduceToLogicObject(uidObject),
											chs.cof.logical.cable.IShieldBody.class);
							if (shieldBody != null) {
								IShieldConductor shieldConductor = shieldBody.getShieldConductor();
								if (shieldConductor != null) {
									relatedObjectUIDToAdd = shieldConductor.getUID();
								}
							}
						}
					}
					if (bAddDiagramObject) {
						objects.add(uidObject.getUID());
					}
					if (relatedObjectUIDToAdd != null) {
						objects.add(relatedObjectUIDToAdd);
					}
				}
			}
		}

		// Library part update should be performed on objects present in the multiple represented objects
		List<IShareableDiagramObjectWithMultipleRepresentation> multiRepObjects =
				new ArrayList<IShareableDiagramObjectWithMultipleRepresentation>();
		diagram.getAllObjects(IShareableDiagramObjectWithMultipleRepresentation.class, multiRepObjects);
		for (IShareableDiagramObjectWithMultipleRepresentation diagramObject : multiRepObjects) {
			Set<IUID> connUIDs = diagramObject.getConnectivityUIDs();
			for (IUID uidObjectUID : connUIDs) {
				ILogicObject connectivityObject = UIDMgr.getObjectOfType(uidObjectUID, ILogicObject.class);
				// Library part part update for pin is taken care by its pin list, so this is not required for pin
				if (connectivityObject != null) {
					objects.add(connectivityObject.getUID());
				}
			}
		}

		//dts0101117092
		//As there is no schem representation of Assembly, BULP does not work for Assembly
		//so explicitly adding all the Assemblies of the design to the list.
		ILogicDesign design = ((ISchemDiagram) diagram).getDesign();
		if (design != null) {
			ICHSIterator<ISchemDiagram> diagramItr = design.getDiagrams(true);
			if (diagram.equals(diagramItr.next())) {
				//Add assemblies only once - for the first diagram in the design
				if ((design.getConnectivity() != null)) {
					IAssemblyIterator assItr = design.getConnectivity().getAssemblies();
					for (IAssembly assembly : assItr) {
						objects.add(assembly.getUID());
					}
				}
			}
		}

		if (design instanceof ILayoutLogicDesign) {
			objects.removeIf(uid -> {
				IUIDObject object = uid.getObject();
				return !(object instanceof ILogicOtherComponent || object instanceof ISchemOtherComponent);
			});
		}

		return objects;
	}

	/**
	 * @see ILibrariedObjectUpdater#update(IBaseDiagram, IUIDObject)
	 */
	public boolean update(@NotNull final IBaseDiagram diagram, @NotNull final IUIDObject object)
	{
		boolean modifiedObject = false;
		ISchemDiagram schemDiagram = CommonUtils.cast(diagram, ISchemDiagram.class);
		//771618
		if (schemDiagram != null) {
			// Wire/Shield may have TerminalPartSpecEnd1 and TerminalPartSpecEnd2 even if part is not assigned on wire
			// TerminalMaterialSpecEnd1 and TerminalMaterialSpecEnd2 should be updated
			assignWireEnds(object);

			// Silently update MC Inner Cores and continue
			if (updateMulticoreInnerCore(object, schemDiagram)) {
				return true;
			}

			CAFCommandHelper commandHelper = new CAFCommandHelper();
			ILibrariedObject librariedObject = getLibrariedObject(object);
			String partNumber = null;
			String partRevision = null;

			ILibraryObject libraryObject = null;
			if (librariedObject != null) {
				// remember the part number in case it gets set to null because the library part can't be found,
				// apparently that can happen in some of our code and we don't want it, if it does happen
				// we put the part number back again in finishAssignment below
				partNumber = librariedObject.getPartNumber();
				partRevision = librariedObject.getPartRevision();
				libraryObject = setLibraryObjectContext(diagram.getDesignContainer(), librariedObject);
				if (librariedObject instanceof IOverbraid) {
					CommandContext.setObject(object);
				}
			}
			if (libraryObject != null) {
				boolean incBom =
						!(librariedObject instanceof IBOMObject) || ((IBOMObject) librariedObject).isIncludeOnBOM();
				// Assume we modified the object if we found the Library Part - just means we might save unnecessarily
				modifiedObject = true;
				IUIDObject objectToModify = object;
				boolean hadLibraryPartRef = hasLibraryPartRef(object);
				IProject project = diagram.getProject();
				if (!hadLibraryPartRef) {
					LibraryPartProjectUsageValidityReporter validityReporter = new LibraryPartProjectUsageValidityReporter();
					if (!LibraryHelper.isPartUsableForProject(project, libraryObject, validityReporter)) {
						return false;
					}
				}
				if (ReferenceHelper.reduceToLibrariedLogicObject(object) != librariedObject) {
					objectToModify = CommonUtils.cast(librariedObject, IUIDObject.class);
					if (objectToModify == null) {
						return false;
					}
				}

				if (librariedObject instanceof IBOMObject) {
					incBom = ((IBOMObject) librariedObject).isIncludeOnBOM();
				}

				if (!assignMulticoreOrOverbraid(hadLibraryPartRef, objectToModify, libraryObject)) {
					AssociateLibraryPartCommand cmd =
							new AssociateLibraryPartCommand(commandHelper, schemDiagram, objectToModify, libraryObject)
							{
								@Override protected void syncModularLibraryPart(ILogicObject logObj)
								{
									// Do nothing
								}

								@Override
								protected boolean associateLibraryPart(ILogicObject logObj)
								{
									if (!(logObj instanceof IConnector && ((IConnector) logObj).isModularParent())) {
										super.associateLibraryPart(logObj);
									}
									return true;
								}
							};
					cmd.setIsSkeletonDesignChecker(mIsSkeletonDesign);
					cmd.setPreserveIncludeOnBOM(isPreserveIncludeOnBOM());
					IPreferenceMgr prefs = project.getPreferences();
					boolean symbolUpdatePreference = false;
					if (prefs instanceof IProjectPreferenceMgr) {
						IProjectPreferenceMgr projectPreferenceMgr = (IProjectPreferenceMgr) prefs;
						symbolUpdatePreference = projectPreferenceMgr.getUpdateSymbolOnPartUpdate();
					}
					if (symbolUpdatePreference) {
						if (object instanceof IRepresentedObject) {
							IUIDObject conn = ((IRepresentedObject) object).getRawConnectivity();
							if (conn instanceof ISymboledObject &&
									!((ISymboledObject) conn).getSymbolReferences().isEmpty() &&
									!cmd.canUpdateSymbol()) {
								CommandContext.setMessage("symbolNotUpdated");
							}
						}
					}

					cmd.setSilent(true);
					cmd.setAllowAutoMapAllByName(true);
					if (cmd.prepare()) {
						boolean ok = cmd.execute();
						if (ok) {
							finishAssignment(libraryObject, hadLibraryPartRef, librariedObject, partNumber, incBom,
									partRevision);
						}
					}
				}
				if (librariedObject instanceof IBOMObject && isPreserveIncludeOnBOM()) {
					((IBOMObject) librariedObject).setIncludeOnBOM(incBom);
				}
			}
		}
		return modifiedObject;
	}

	@Nullable private ILibrariedObject getLibrariedObject(@NotNull IUIDObject object)
	{
		return ReferenceHelper.relaxedReduceToLibrariedObject(object);
	}

	/**
	 * Potentially update Multicore Inner core - returns true if 'object' was updated.
	 *
	 * @param object Multicore Inner Core to update (or some other object type)
	 *
	 * @return boolean true if 'object' was updated
	 */
	protected boolean updateMulticoreInnerCore(@NotNull IUIDObject object, @Nullable ISchemDiagram diagram)
	{
		ILogicObject logicObject = ReferenceHelper.reduceToLogicObject(object);
		if (logicObject != null) {
			//We want the libraried object corresponding to the innerMC, not the parent MC
			if (logicObject instanceof chs.cof.logical.cable.IShieldBody) {
				logicObject = ((chs.cof.logical.cable.IShieldBody) logicObject).getMulticore();
			}
			ILibrariedObject librariedObject = ReferenceHelper.reduceToLibrariedObject(logicObject);
			if (librariedObject != null) {
				// Have to check the loaded object rather than calling getInnercoreRef() as getInnercoreLibraryObject()
				// checks the actual relationship with the MC in Library
				ILibraryInnerCore libraryInnerCore = logicObject.getInnercoreLibraryObject();
				if (libraryInnerCore != null) {
					boolean isShield = isShield(librariedObject);
					ILibrarySingleWireCore.ConductorRoleEnum conductorRole = getConductorRole(libraryInnerCore);
					if ((isShield ^ (conductorRole == ILibrarySingleWireCore.ConductorRoleEnum.Shield))) {
						CommandContext.setMessage("libInnercoreTypeMismatch");
						return false;
					}
					else {
						String oldIndicatorType = null;
						if (libraryInnerCore instanceof ILibraryMultiWireCore) {
							IMulticore multicore = CommonUtils.cast(librariedObject, IMulticore.class);
							if (multicore == null) {
								CommandContext.setMessage("libPartIncorrectType");
								return false;
							}
							oldIndicatorType = multicore.getIndicatorType();
						}
						LibraryAssignmentHelper.setInnercoreAttributes(librariedObject, libraryInnerCore);
						if (libraryInnerCore instanceof ILibraryMultiWireCore) {
							//if there is change in indicator type we will ensure the reCreateGraphics is true.
							//otherwise reCreateGraphics will be false and would setup the hookups only.
							if (mLibrariedObjectsUnderUpdate.registered((IUIDObject) librariedObject)) {
								MulticoreLibraryHelper.redrawSchemIndicators((IMulticore) librariedObject, diagram,
										true);
							}
							else {
								String newIndicatorType = ((IMulticore) librariedObject).getIndicatorType();
								boolean reCreateGraphics = !newIndicatorType.equalsIgnoreCase(oldIndicatorType);
								if (reCreateGraphics) {
									mLibrariedObjectsUnderUpdate.register((IUIDObject) librariedObject);
								}
								MulticoreLibraryHelper.redrawSchemIndicators((IMulticore) librariedObject, diagram,
										reCreateGraphics);
							}
						}
						return true;
					}
				}
			}
		}
		return false;
	}

	private ILibrarySingleWireCore.ConductorRoleEnum getConductorRole(ILibraryInnerCore libraryInnerCore)
	{
		ILibrarySingleWireCore.ConductorRoleEnum conductorRole =
				ILibrarySingleWireCore.ConductorRoleEnum.Blank;
		if (libraryInnerCore instanceof ILibrarySingleWireCore) {
			ILibrarySingleWireCore librarySingleWireCore = (ILibrarySingleWireCore) libraryInnerCore;
			conductorRole = librarySingleWireCore.getConductorRole();
		}
		return conductorRole;
	}

	private boolean isShield(ILibrariedObject librariedObject)
	{
		boolean isShield = false;
		if (librariedObject instanceof IShieldConductor) {
			isShield = true;
		}
		else {
			if (librariedObject instanceof IWireConductor) {
				isShield = ((IWireConductor) librariedObject).isShieldWire();
			}
		}
		return isShield;
	}

	@Override public IUIDObject getPseudoLibrariedObject(@NotNull IUIDObject uidObject)
	{
		if (uidObject instanceof IShieldBody) {
			ILogicObject cableShieldBody = ReferenceHelper.reduceToLogicObject(uidObject);
			if (cableShieldBody instanceof chs.cof.logical.cable.IShieldBody) {
				IMulticore mc = ((chs.cof.logical.cable.IShieldBody) cableShieldBody).getMulticore();
				if (mc.isPartAssigned() && mc.getInnercoreLibraryObject() instanceof ILibraryMultiWireCore) {
					//This is a innerMC
					return mc.getInnercoreLibraryObject();
				}
			}
		}
		return null;
	}

	@Override public void removePartNumMessageIfFootprintMismatch()
	{
		//
	}

	@Override protected boolean mismatchForAlreadyAssignedPart(IMulticore rootMc)
	{
		ILibraryBaseObject libraryObject = rootMc.getLibraryObject();
		ILibraryMulticore libMc = CommonUtils.cast(libraryObject, ILibraryMulticore.class);
		if (libMc != null) {
			boolean exactMatch = isStrictMatch(rootMc);
			StructureMatchService matchService = StructureMatchService.getInstance();
			boolean isStructureMatching = exactMatch ? matchService.validateExactMatch(rootMc, libMc) :
					matchService.validateContainmentMatch(rootMc, libMc);
			if (!isStructureMatching) {
				CommandContext.setMessage("libMulticoreStructureMismatch");
				return true;
			}
		}
		return false;
	}
}
