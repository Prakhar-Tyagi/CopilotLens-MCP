/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, mentor, require, $, alert, allowApplicationAccess*/
define("router", [
    'backbone', "currentPackage", "models/detailsPanelModel",
    "fileDisplayHandler", "EULA", "DesignObjectPopoverModel",
    "LocationViewObjectPopoverModel", "collections/informations", "LocationViews",
    "Harnesses", "harnessLayouts", "Diagnostics", "collections/faults",
    "underscore", "componentRouter", "PackagesInSession", "ComponentLoader", "preferences", "UserSession",
    "filehandlers/effectivitySetter", "ProjectsViewModel"
], function (Backbone, selectedPackage, detailModel,
        fileDisplayHandler, eula, designObjectModel,
        LocationViewObjectPopoverModel, informations, LocationViews,
        Harnesses, harnessLayouts, Diagnostics, faults,
        underscore, componentRouter, packagesInSession, ComponentLoader, preferences, UserSession,
        effectivitySetter, ProjectsViewModel) {
    "use strict";

    return Backbone.Router.extend({
        routes: {
            "package/*id": "loadPackage",
            ":packageName/component/:componentName/*parameters": "findAndShowComponentByType",
            ":packageName/diagram/:systemName/*parameters": "showDiagram",
            ":packageName/report/:systemName/*parameters": "showSystemReport",
            ":packageName/locationview/:componentName/*parameters": "showLocationByName",
            "harness/:packageName/*parameters": "showHarnessByName",
            "ground/:packageName/*parameters": "showGroundReportByName",
            "customView/:packageName/*parameters": "showCustomViewByType",
            "jtthreedview/:packageName/*parameters": "showJTThreeDViewName",
            "rathreedview/:packageName/*parameters": "showRAThreeDViewName",
            "objectlink/:packageName/:systemName/:type/:componentName/*parameters": "showObjectLevelPlugin",
            "plugin/:packageName/:systemName/:type/:componentName/*parameters": "showObjectLevelPlugin",
            "showpowertoground/:packageName/:systemName/:type/:componentName": "showPowerToGround",
            ":packageName/renderconnectivity/:componentName/*parameters": "renderSignalForAComponent",
            "showLocation/:name/:projectId/:projectName/:objectId": "showLocation",
            "showLocation/:name/*projectId": "showLocation",
            "locationview/:packageName/*parameters": "showProjectLocationByName",
            "renderSignal/:connectivityFileName/*projectId": "renderSignal",
            ":projectName/information/*parameters": "showInformationByName",
            "information/:name/*projectId": "showInformation",
            ":packageName/faceview/:connectorName/*parameter": "showFaceViewByName",
            ":packageName/threedview/:componentName/*parameter": "showObject3dByName",
            "faceview/:systemId/:connectorId/id:viewId/*projectId": "showFaceViewByViewName",
            "faceview/:systemId/:connectorId/*projectId": "showFaceView",
            "system/:id/:diagramId/:projectId/:projectName/:objectId": "showSystemDiagram",
            "system/:id/:diagramId/*projectId": "showSystemDiagram",
            "showFaultCode/:name/*projectId": "showFaultCode",
            "faultcode/:projectName/*parameters": "showFaultInformationByName",
            "showHarness/:name/*projectId": "showHarness",
            "report/:title/:systemId/:projectId/:projectName/*path": "showCustomReport",
            "globalReport/:title/:projectId/:projectName/*path": "showGlobalReport",
            "report/:systemId/:reportId/*projectId": "showReport",
            "report/:packageName/*parameters": "showGroundReportsByName",
            "customFile/:title/:projectId/:projectName/*path": "showCustomFile",
            "threeDXML/:title/:projectId/:projectName/:type/(:objectId)/*path": "showThreeDXML",
            "ra3DXML/:title/:projectId/:projectName/(:objectId)/*path": "showRA3DXML",
            'troubleshoot/:projectId/:projectName/(:activeCodes)/(:passiveCodes)': 'troubleshoot',
            'faultObjectTable/:projectId/:projectName/(:activeCodes)/(:passiveCodes)': 'faultObjectTable',
            "diagnostic/:packageName/*parameters": "showDiagnosticByName",
            "harnesslayoutdiagram/:layoutId/:diagramId/:projectId/:projectName(/)(:objectId)": "showHarnessLayoutDiagram",
            "olddesignrevision/:layoutId/:diagramId/:projectId/:projectName(/)(:objectId)": "showOldHarnessLayoutDiagram",
            "newdesignrevision/:layoutId/:diagramId/:projectId/:projectName(/)(:objectId)": "showNewHarnessLayoutDiagram",
            "harnesslayoutreport/:layoutId/:reportName/*projectId": "showHarnessLayoutReport",
            "document_views/:projectId/:projectName/effRange(:effRange)/projId(:projId)/objectId(:objectId)/curDoc(:selectedDocument)/:firstDocument(/:secondDocument)(/:thirdDocument)": "renderDocuments",
            "document/:type/:title/:projectId/:projectName/searchText(:searchText)/*path": "openViewByType",
            "*action": "defaultAction"
        },
        openViewByType: function (type, title, projectId, projectName, searchText, path) {
            var that = this;
            if (!mentor.publisher.project) {
                this.loadProject({
                    success: function () {
                        that.openViewByType(type, title, projectId, projectName, searchText, path);
                    }
                });
            }
            require(["fileDisplayHandler", "currentPackage"], function (fileDisplayHandler, currentPackage) {
                var document = {
                    type: type,
                    mainText: Utils.translate(title),
                    path: path
                }
                currentPackage.set("searchText", searchText, {silent: true});
                fileDisplayHandler.display(document);
            });
        },
        renderDocuments: function (projectId,
                projectName,
                effRange,
                parentProjId,
                objectId,
                selectedDocument,
                firstDocument,
                secondDocument,
                thirdDocument) {
            var that = this;

            if (projectId && projectName) {
                var projectPath = projectId + "/" + projectName;

                var project = mentor.publisher.project;
                if (!project || project.getId().replace(/\\/g, "/") !== projectPath) {
                    var options = {
                        projectId: projectPath,
                        projId: parentProjId,
                        range: effRange,
                        doNottriggerFirstSection: true,
                        success: function () {
                            that.renderDocuments(projectId,
                                    projectName,
                                    effRange,
                                    parentProjId,
                                    objectId,
                                    selectedDocument,
                                    firstDocument,
                                    secondDocument,
                                    thirdDocument);
                        }
                    };
                    this.loadProject(options);
                }
                if (this.documentRouter && this.documentRouter.render) {

                    this.documentRouter.render(objectId, selectedDocument, firstDocument, secondDocument,
                            thirdDocument);
                }
            }
        },
        setDocumentRouter: function (documentRouter) {
            this.documentRouter = documentRouter;
        },
        showProjectLocationByName: function (packageName, parameters) {
            this.showProjectArtifact(packageName, mentor.publisher.contentType.LOCATION_VIEWS, parameters);
        },
        showInformationByName: function (packageName, parameters) {
            this.showProjectArtifact(packageName, mentor.publisher.contentType.CUSTOM_VIEW, parameters);
        },
        showDiagnosticByName: function (packageName, parameters) {
            this.showProjectArtifact(packageName, mentor.publisher.contentType.DIAGNOSTIC, parameters);
        },
        showCustomViewByType: function (packageName, parameters) {
            this.showProjectArtifact(packageName, mentor.publisher.contentType.CUSTOM_VIEW, parameters);
        },
        showJTThreeDViewName: function (packageName, parameters) {
            this.showProjectArtifact(packageName, mentor.publisher.contentType.JT_3D, parameters);
        },
        showRAThreeDViewName: function (packageName, parameters) {
            this.showProjectArtifact(packageName, mentor.publisher.contentType.RA_3D, parameters);
        },
        showObjectLevelPlugin: function (packageName, systemName, type, componentName, parameters) {
            require(["routers/customPluginRouter"], function (router) {
                router.loadProjectData(packageName, componentName, parameters, type);
            });
        },
        showPowerToGround: function (packageName, systemName, type, componentName, parameters) {
            require(["routers/powerToGroundRouter"], function (router) {
                router.loadProjectData(packageName, componentName, "", type);
            });
        },
        showTroubleshoot: function (packageName, parameters) {
            require(["routers/troubleshootRouter"], function (router) {
                router.loadProjectData(packageName, "", parameters, mentor.publisher.contentType.TROUBLESHOOT);
            });
        },
        troubleshoot: function (projectId, projectName, activeCodes, passiveCodes) {
            var activeCodes = (activeCodes && activeCodes.split(",")) || [];
            var passiveCodes = (passiveCodes && passiveCodes.split(",")) || [];
            this.loadProject({
                projectId: projectId + "/" + projectName,
                doNottriggerFirstSection: true,
                success: function () {
                    require(["collections/faults"], function (faults) {
                        faults.fetch();
                        fileDisplayHandler.display({
                            type: mentor.publisher.contentType.TROUBLESHOOT,
                            passiveCodes: passiveCodes,
                            activeCodes: activeCodes,
                        });
                    });
                }
            });
        },
        faultObjectTable: function (projectId, projectName, activeCodes, passiveCodes) {
            var activeCodes = (activeCodes && activeCodes.split(",")) || [];
            var passiveCodes = (passiveCodes && passiveCodes.split(",")) || [];
            this.loadProject({
                projectId: projectId + "/" + projectName,
                doNottriggerFirstSection: true,
                success: function () {
                    require(["collections/faults"], function (faults) {
                        faults.fetch();
                        fileDisplayHandler.display({
                            type: mentor.publisher.contentType.FAULT_OBJECT_TABLE,
                            poppedOutFaultObjectTable: true,
                            passiveCodes: passiveCodes,
                            activeCodes: activeCodes,
                        });
                    });
                }
            });
        },
        renderSignalForAComponent: function (packageName, componentName, parameters) {
            require(["routers/objectConnectivityRouter"], function (objectConnectivityRouter) {
                objectConnectivityRouter.findAndShowComponentByType(packageName, componentName, parameters);
            });
        },
        showHarnessByName: function (packageName, parameters) {
            this.showProjectArtifact(packageName, mentor.publisher.contentType.HARNESS, parameters);
        },
        showGroundReportsByName: function (packageName, parameters) {
            this.showProjectArtifact(packageName, mentor.publisher.contentType.GLOBAL_GROUND_REPORT, parameters);
        },
        showGroundReportByName: function (packageName, parameters) {
            this.showProjectArtifact(packageName, mentor.publisher.contentType.CAPITAL_REPORT, parameters);
        },
        showFaultInformationByName: function (packageName, parameters) {
            this.showProjectArtifact(packageName, mentor.publisher.contentType.FAULT_CODE, parameters);
        },
        showProjectArtifact: function (packageName, type, parameters) {
            require(["routers/projectDataRouter"], function (projectDataRouter) {
                projectDataRouter.findAndShowProjectData(packageName, type,
                        parameters);
            });
        },
        showLocationByName: function (packageName, componentName, parameters) {
            require(["routers/locationViewRouter"], function (locationViewRouter) {
                locationViewRouter.findAndShowComponentByType(packageName, componentName, parameters);
            });
        },
        showSystemReport: function (packageName, systemName, parameters) {
            require(["routers/systemReportRouter"], function (systemReportRouter) {
                systemReportRouter.findAndShowComponentByType(packageName, systemName, parameters);
            });
        },

        showDiagram: function (packageName, systemName, parameters) {
            require(["routers/systemRouter"], function (systemRouter) {
                systemRouter.findAndShowComponentByType(packageName, systemName, parameters);
            });
        },
        getFirstArgumentBeforeQueryString: function (parameters) {
            parameters = parameters || "";
            if (parameters) {
                return parameters.split("/")[0];
            }
        },
        getConfig: function (parameters) {
            var langAndConfig;
            parameters = parameters || "";
            if (parameters) {
                langAndConfig = parameters.split("/");
                return langAndConfig.length > 2 && langAndConfig[2];
            }
        },
        getLanguage: function (parameters) {
            var langAndConfig;
            parameters = parameters || "";
            if (parameters) {
                langAndConfig = parameters.split("/");
                return langAndConfig.length > 1 && langAndConfig[1];
            }
        },
        findAndShowComponentByType: function (packageName, componentName, parameters) {
            componentRouter.findAndShowComponentByType(packageName, componentName, parameters);
        },
        renderSignal: function (connectivityFileName, projectId) {
            this.loadProject({
                projectId: projectId, doNottriggerFirstSection: true, success: function () {
                    mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.GROUND_PATH_TRACE,
                            {id: connectivityFileName});
                }
            });
        },
        renderSignalOnObject: function (objectId, systemId) {
            var faceviewData = mentor.publisher.project.loadObjectData(systemId,
                    objectId), content, Model = Backbone.Model.extend({});
            if (faceviewData) {
                var signalTraceFiles = faceviewData.getSignalTraceFiles();
                content = signalTraceFiles && (signalTraceFiles.signalTraceFile || signalTraceFiles.fullInstanceFile);
                if (content) {
                    mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.GROUND_PATH_TRACE,
                            {id: content});
                    crossHighlightHandler.initCrossHighlight(objectId);
                }
            }
        },
        showThreeDXML: function (title, projectId, projectName, type, objectId, path) {
            objectId = objectId || "";
            objectId = objectId.replace(/___/g, "/");
            this.loadProject({
                projectId: projectId + "/" +

                        projectName, doNottriggerFirstSection: true, success: function () {
                    var Model = Backbone.Model.extend({}), content = new Model();
                    content.set({mainText: title, reset: true, path: path, type: type, objectId: objectId});
                    content.type = type;
                    fileDisplayHandler.display(content);
                }
            });
        },
        showRA3DXML: function (title, projectId, projectName, objectId, path) {
            objectId = objectId || "";
            objectId = objectId.replace(/___/g, "/");
            this.loadProject({
                projectId: projectId + "/" +

                        projectName, doNottriggerFirstSection: true, success: function () {
                    var Model = Backbone.Model.extend({}), content = new Model();
                    content.set({
                        mainText: title,
                        reset: true,
                        path: path,
                        type: mentor.publisher.contentType.RA_3D,
                        objectId: objectId
                    });
                    content.type = mentor.publisher.contentType.RA_3D;
                    fileDisplayHandler.display(content);
                }
            });
        },
        showCustomFile: function (title, projectId, projectName, filePath) {
            this.loadProject({
                projectId: projectId + "/" +
                        projectName, doNottriggerFirstSection: true, success: function () {
                    var content = {
                        mainText: title,
                        reset: true,
                        path: filePath,
                        type: mentor.publisher.contentType.CUSTOM_VIEW
                    };
                    require(["internalLinkHandler"], function (internalLinkHandler) {
                        if (internalLinkHandler.isItaRelativeURL(content.path)) {
                            internalLinkHandler.displayHref(content.path);
                        }
                        else {
                            fileDisplayHandler.display(content);
                        }
                    })
                }
            });
        },
        showCustomReport: function (title, systemId, projectId, projectName, filePath) {
            this.loadProject({
                projectId: projectId + "/" +
                        projectName, doNottriggerFirstSection: true, success: function () {
                    fileDisplayHandler.display({
                        id: systemId,
                        path: filePath,
                        systemId: systemId,
                        reset: true,
                        title: title,
                        type: mentor.publisher.contentType.SYSTEM_REPORT
                    });
                }
            });
        },
        showGlobalReport: function (title, projectId, projectName, filePath) {
            this.loadProject({
                projectId: projectId + "/" +
                        projectName, doNottriggerFirstSection: true, success: function () {
                    fileDisplayHandler.display({
                        path: filePath,
                        reset: true,
                        title: title,
                        mainText: title,
                        type: mentor.publisher.contentType.CAPITAL_REPORT
                    });
                }
            });
        },

        showHarnessDesign: function (projectId, diagramId, layoutId, type, objectId) {
            this.loadProject({
                projectId: projectId,
                doNottriggerFirstSection: true,
                success: function () {
                    if (objectId) {
                        require(["models/selectedSystem"], function (selectedSystem) {
                            selectedSystem.set("objectId", objectId, {silent: true});
                        });
                    }
                    fileDisplayHandler.display({
                        id: diagramId,
                        layoutId: layoutId,
                        reset: true,
                        group: "diagrams",
                        type: type
                    });
                }
            });
        }, showHarnessLayoutDiagram: function (layoutId, diagramId, projectId, projectName, objectId) {
            var folder = projectId + "/" + projectName;

            this.showHarnessDesign(folder, diagramId, layoutId,
                    mentor.publisher.contentType.HARNESS_LAYOUT_DIAGRAM,
                    objectId);

        },
        showOldHarnessLayoutDiagram: function (layoutId, diagramId, projectId, projectName, objectId) {
            var folder = projectId + "/" + projectName;
            this.showHarnessDesign(folder, diagramId, layoutId, mentor.publisher.contentType.OLD_DESIGN_REVISION,
                    objectId);
        },
        showNewHarnessLayoutDiagram: function (layoutId, diagramId, projectId, projectName, objectId) {
            var folder = projectId + "/" + projectName;
            this.showHarnessDesign(folder, diagramId, layoutId, mentor.publisher.contentType.NEW_DESIGN_REVISION,
                    objectId);
        }
        , showHarnessLayoutReport: function (layoutId, report, projectId) {
            this.loadProject({
                projectId: projectId,
                doNottriggerFirstSection: true,
                success: function () {
                    fileDisplayHandler.display({
                        group: "reports",
                        id: report,
                        layoutId: layoutId,
                        reset: true,
                        type: mentor.publisher.contentType.HARNESS_LAYOUT_REPORT
                    });
                }
            });
        },

        showLinkedHarnessLayoutDiagram: function (params) {
            var project,
                    component;

            project = mentor.publisher.urlParams.project;
            component = mentor.publisher.urlParams.component;

            require(["routers/harnessLayoutDiagramRouter"], function (theRouter) {
                theRouter.findAndShowComponentByType(project, component, params);
            });
        },

        showLinkedHarnessLayoutReport: function (params) {
        },

        showFaceViewByName: function (packageName, connectorName, parameters) {
            require(["routers/objectFaceViewRouter"], function (objectFaceViewRouter) {
                objectFaceViewRouter.findAndShowComponentByType(packageName, connectorName, parameters);
            });
        },
        showObject3dByName: function (packageName, connectorName, parameters) {
            require(["routers/object3DViewRouter"], function (object3DViewRouter) {
                object3DViewRouter.findAndShowComponentByType(packageName, connectorName, parameters);
            });
        },
        openFaceView: function (connectorId, systemId, viewId, getValueToCompare) {
            fileDisplayHandler.display({
                objectId: connectorId,
                systemId: systemId,
                viewId: viewId,
                id: viewId,
                type: mentor.publisher.contentType.CONNECTOR_FACE_VIEW,
                getValueToCompare: getValueToCompare
            });
        },

        open3DView: function (objectId, systemId) {
            var objectData = mentor.publisher.project.loadObjectData(systemId,
                    objectId), content, Model = Backbone.Model.extend({}), object3DModel = new Model(), objectView;
            if (objectData) {
                content = objectData.get3DViews().listItems || [];
                objectView = this.getViewObjectForType(content, mentor.publisher.urlParams.viewName);
                if (objectView) {
                    object3DModel.set(objectView);
                    object3DModel.type = objectView.type;
                    fileDisplayHandler.display(object3DModel);
                }
            }
        },
        findObjectByName: function (array, objectName, getValueToCompare) {
            var objectFound;
            array = array || [];
            if (objectName) {
                for (var index in array) {
                    if (getValueToCompare && array.hasOwnProperty(index) && array[index] &&
                            getValueToCompare(array[index]) === objectName.toLowerCase()) {
                        objectFound = array[index];
                        break;
                    }
                    else if (array.hasOwnProperty(index) && array[index] &&
                            array[index].mainText.toLowerCase() === objectName.toLowerCase()) {
                        objectFound = array[index];
                        break;
                    }
                }
            }

            return objectFound;
        },
        getViewObjectForType: function (contentArray, viewName, getValueToCompare, showMessageOnFailure) {
            var objectView;
            if (contentArray.length > 0) {
                if (viewName) {
                    objectView = this.findObjectByName(contentArray, viewName, getValueToCompare);
                    if (!objectView) {
                        if (showMessageOnFailure) {
                            alert(mentor.publisher.languageTranslator.localize("AlertViewNotFound").format(viewName, mentor.publisher.urlParams.view));
                        }

                        objectView = contentArray[0];
                    }
                }
                else {
                    objectView = contentArray[0];
                }
            }
            else {
                alert(mentor.publisher.languageTranslator.localize("AlertViewNotExistForGivenObj").format(mentor.publisher.urlParams.view));
            }
            return objectView;
        },
        open2DView: function (objectId, systemId) {
            var objectData = mentor.publisher.project.loadObjectData(systemId,
                    objectId), content, Model = Backbone.Model.extend(
                    {}), object2DView = new Model(), view = mentor.publisher.urlParams.viewName ||
                    "";
            if (objectData) {
                content = objectData.get2dLocationViews().listItems || [];
                object2DView = this.getViewObjectForType(content, view, undefined, true);
                if (object2DView) {
                    mentor.publisher.selectedSystem.set("objectId", objectId, {silent: true});
                    object2DView.type = mentor.publisher.contentType.LOCATION_VIEWS;
                    if (this.isPDFContent(object2DView)) {
                        object2DView.type = mentor.publisher.contentType.CUSTOM_VIEW;
                    }
                    fileDisplayHandler.display(object2DView);

                }

            }
        },
        isPDFContent: function(content) {
            return content && content.path && getPluginType(content.path)
                    && getPluginType(content.path).indexOf("pdf") > 0;
        },

        showFaceViewByViewName: function (systemId, connectorId, viewName, projectId) {
            mentor.publisher.urlParams = mentor.publisher.urlParams || {};
            //mentor.publisher.urlParams.viewName = viewName;
            this.openFaceViewByViewName(systemId, connectorId, projectId, viewName);
        },
        openFaceViewByViewName: function (systemId, connectorId, projectId, viewId) {
            var that = this;
            this.loadProject({
                projectId: projectId, doNottriggerFirstSection: true, success: function () {
                    that.openFaceView(connectorId, systemId, viewId);
                }
            });
        }, showFaceView: function (systemId, connectorId, projectId) {
            this.openFaceViewByViewName(projectId, connectorId, systemId);
        },
        showInformation: function (name, projectId) {
            this.showContent(projectId, name);
        },

        showReport: function (systemId, reportId, projectId) {
            this.loadProject({
                projectId: projectId, doNottriggerFirstSection: true, success: function () {

                    fileDisplayHandler.display({
                        id: systemId,
                        reportId: reportId,
                        systemId: systemId,
                        reset: true,
                        type: mentor.publisher.contentType.SYSTEM_REPORT
                    });
                }
            });
        },

        show3DView: function (systemId, objectId, projectId) {
            this.loadProject({
                projectId: projectId, doNottriggerFirstSection: true, success: function () {
                    this.loadObject(systemId, objectId, function () {
                        fileDisplayHandler.display({
                            id: objectId,
                            systemId: systemId,
                            reset: true,
                            type: mentor.publisher.contentType.CUSTOM_VIEW
                        });
                    });
                }
            });
        },

        getProjectId: function (projectId, projectName) {
            if (projectName) {
                projectId = projectId + "/" + projectName;
            }
            return projectId;
        },

        showSystemDiagram: function (id, diagramId, projectId, projectName, objectId) {
            this.loadProject({
                projectId: this.getProjectId(projectId,
                        projectName), doNottriggerFirstSection: true, success: function () {
                    fileDisplayHandler.display({
                        id: id,
                        diagramId: diagramId,
                        reset: true,
                        type: mentor.publisher.contentType.SYSTEM_SVG,
                        objectId: objectId
                    });
                }
            });

        },

        loadPackage: function (id) {
            this.loadProjectAndView({projectId: id});
        },

        loadProjectAndView: function (options) {
            var router = this;
            router.options = options || router.options || {};
            router.options.success = router.options.success || function () {
            };
            if (router.options.projectId && selectedPackage.get("id") === router.options.projectId) {
                var urlParameters = router.options.parameters || {};
                var isInternalLink = urlParameters.internalLink;
                if (!isInternalLink) {
                    router.loadLanguageTranslationServices(options);
                    router.showViewerPage();
                }
                router.setPackageScreenVisible();
            }
            else {
                router.loadViewer(router.options);
            }
        },

        loadProject: function (options) {
            var that = this;
            var initialized = true;
            if (options && options.projectId) {
                initialized = effectivitySetter.initializeEffectivity(options);
            }
            if (initialized) {
                LoadMask.addLoadingRingOnPackageOpen();
                if (options) {
                    var onSuccess = options.success;
                    options.success = function () {
                        LoadMask.removeLoadingRingAfterPackageLoad();
                        onSuccess && onSuccess()
                    };
                }
                require(["URLQueryParameterHandler"], function (queryParameterHandler) {
                    var showDefaultPage = queryParameterHandler.handleQueryParameters();
                    if (showDefaultPage) {
                        that.loadProjectAndView(options);
                    }
                });
            }
        },

        loadObject: function (systemId, objectId, callBack) {
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.OPEN_OBJECT_POPUP,
                    {id: objectId, systemId: systemId, callBack: callBack});
        },
        showContent: function (projectId, id, type, objectId) {
            this.loadProject({
                projectId: projectId, doNottriggerFirstSection: true, success: function () {
                    var content = {id: id, reset: true, type: type, mainText: id};
                    if (objectId) {
                        content.reset = false;
                        mentor.publisher.selectedSystem.set("objectId", objectId, {silent: true});
                    }
                    content.type = content.type || mentor.publisher.contentType.CUSTOM_VIEW;
                    fileDisplayHandler.display(content);
                }
            });

        },
        showLocation: function (name, projectId, projectName, objectId) {

            this.showContent(this.getProjectId(projectId, projectName), name,
                    mentor.publisher.contentType.LOCATION_VIEWS, objectId);
        },
        showFaultCode: function (name, projectId) {
            this.showContent(projectId, name, mentor.publisher.contentType.FAULT_CODE);
        },
        showHarness: function (name, projectId) {
            this.showContent(projectId, name, mentor.publisher.contentType.HARNESS);
        },

        setLanguageFilter: function () {
            var p = mentor.publisher;
            var project = p.project;
            while (project.getProject && project.getProject() !== null) {
                project = project.getProject();
            }
            p.LanguageFilteredProject.setProject(project);
        }, loadPackageData: function (options) {

            var packageToBeShown, that = this;
            if (window.opener && window.opener.mentor) {
                mentor.publisher.dataLoader = window.opener.mentor.publisher.dataLoader;
                mentor.publisher.project = window.opener.mentor.publisher.project;
                mentor.publisher.colors = window.opener.mentor.publisher.colors;
                mentor.publisher.languageDataLoader = window.opener.mentor.publisher.languageDataLoader;
                mentor.publisher.languageTranslator = window.opener.mentor.publisher.languageTranslator;
                mentor.publisher.constants = window.opener.mentor.publisher.constants;
                mentor.publisher.locationViews = window.opener.mentor.publisher.locationViews;
                mentor.publisher.filter = window.opener.mentor.publisher.filter;
                mentor.publisher.config = window.opener.mentor.publisher.config;
                mentor.publisher.configurationsManager = window.opener.mentor.publisher.configurationsManager;
                packageToBeShown = {id: mentor.publisher.project.getId()};
                //event should not be genrated here as on popout window it is not required
                selectedPackage.set(packageToBeShown, {silent: true});
                //load only these three modules others are not required on popout window
                //informations.fetch();
                //LocationViews.fetch();
                //Harnesses.fetch();
                harnessLayouts.fetch();
                //Diagnostics.fetch();
            }
            else {
                var allPackages = packagesInSession.get("packages");

                var range = options.range || options.parameters && options.parameters.effRange;
                var projectId = options.projId || options.parameters && options.parameters.projId;
                packageToBeShown = allPackages.findPackageById(options.projectId, range, projectId);

                //console.log("time taken to fetch all packages" + (Utils.getTime() - startTime));
                if (packageToBeShown) {
                    var packageId = packageToBeShown.id.replace(/\\/g, "/");
                    effectivitySetter.initializeEffectivity({
                        projId: options.projId,
                        projectId: packageId,
                        range: options.range
                    });
                    var selectedSubPackage = allPackages.findSubPackageBy(packageId, options.range, options.projId);
                    UserSession.getActiveSession().set(UserSession.kSelectedPackageProperty, selectedSubPackage);
                    mentor.publisher.filter.vinOptions = '';
                    mentor.publisher.configurationsManager.reset();
                    // mentor.publisher.languageDataLoader.reset();
                    mentor.publisher.dataLoader.loadProject(packageToBeShown.id);
                    //console.log("time taken to load projects" + (Utils.getTime() - startTime));
                    this.setLanguageFilter();
                    mentor.publisher.project = mentor.publisher.LanguageFilteredProject;

                    that.loadLanguageTranslationServices(options);
                    //console.log("time taken to language tarnslation" + (Utils.getTime() - startTime));

                    mentor.publisher.detailLayoutManager.reset();

                }

                selectedPackage.set(packageToBeShown);
            }

            //console.log("time taken to load all views" + (Utils.getTime() - startTime));

            if (!options.doNottriggerFirstSection) {
                detailModel.fetch();

            }

        },

        defaultAction: function (action) {
            mentor.publisher.languageTranslator.loadResources(preferences.get("language"));
            var translator = mentor.publisher.languageTranslator;
            var errorMessage, clientType;
            var allPackages = packagesInSession.get("packages");
            if (allPackages.models.length === 0) {
                errorMessage = updateClientType(translator.localize("NoValidPacketAvailableMsg"), "");
                showError(errorMessage);
                return;
            }
            var urlParams = mentor.publisher.urlParams;
            if (!urlParams.hasOwnProperty('project') && urlParams.hasOwnProperty('effRange')) {
                var matchingSubPackages = allPackages.findSubpackagesByRange(urlParams.effRange,
                        allPackages.getAllSubPackages());
                if (matchingSubPackages && matchingSubPackages.length > 0) {
                    urlParams.project = matchingSubPackages[0].attributes.name;
                }
            }
            if (urlParams.hasOwnProperty('project')) {
                this.loadProject();
            }
            else {

                var activeSession = UserSession.getActiveSession();

                if (allPackages.containSubPackages()) {
                    if (allPackages.models[0].subPackages.models.length === 1) {
                        var subPackage = allPackages.models[0].subPackages.at(0);
                        activeSession.set(UserSession.kSelectedPackageProperty, subPackage);
                        this.loadProject({
                            projectId: subPackage.get('id'),
                            range: subPackage.get('effectivityRange'),
                            projId: subPackage.get('projectId')
                        });
                    }
                    else {
                        this.showHome();
                    }
                }
                else {
                    // If there is only one package, open the package directly.
                    if (allPackages.size() === 1) {
                        activeSession.set(UserSession.kSelectedPackageProperty, allPackages.at(0));
                        this.loadProject();
                    } else if (allPackages.size() > 1 && urlParams.projId) {
                        this.model = new ProjectsViewModel();
                        const projects = this.model.projects;
                        const projectIndex = projects.indexOf(projects.findWhere({"id": urlParams.projId}));
                        if (projectIndex !== -1) {
                            this.model.setCurrentIndex(projectIndex);
                        }
                        this.showHome();
                    }
                    else {
                        this.showHome();
                    }
                }
            }
        },

        loadLanguageTranslationServices: function (options) {
            options = options || {};

            var lang = options.language || mentor.publisher.urlParams.lang ||
                    mentor.publisher.urlParams.language;
            if (lang) {
                preferences.set("language", lang);
                // updating current package when language is coming through the url
                setTimeout(() => {
                    selectedPackage.set("language", lang);
                }, 100);
            }
            if (Utils.isPopoutWindow()) {
                mentor.publisher.languageTranslator = window.opener.mentor.publisher.languageTranslator;
            }
            else {
                mentor.publisher.languageTranslator.initialize(preferences.get("language"));
            }
        },

        loadComponents: function (componentsLocation, that) {

            ComponentLoader.loadComponents(componentsLocation, {
                beforeComponentsStartLoading: function () {
                    if (!that.options.dontLoadPackageData) {
                        that.loadPackageData(that.options);
                    }
                },
                preRender: function () {

                },

                postRender: function () {
                    if (selectedPackage.hasChanged("id")) {
                        selectedPackage.trigger("change:id");
                    }

                    if (that.options.config || that.options.VIN) {
                        componentRouter.applyConfiguration(that.options);
                    }
                    // urlParams support for searchFilter
                    if (that.options.query) {
                        that.setSearchValue(that.options.query);
                    }
                    // urlParams support for show/hide navigation
                    if (that.options.navPanel) {
                        const navPanel = that.options.navPanel;
                        require(["views/navigationPanelView"], function (navigationPanelView) {
                            if(navPanel === "show") {
                                navigationPanelView.showPanel();
                            } else if(navPanel === "hide") {
                                navigationPanelView.hidePanel();
                            }
                        });
                    }
                    // urlParams support for introduction page
                    if(mentor.publisher.urlParams.view === 'information' && mentor.publisher.urlParams.viewName) {
                        const language = that.options.language || preferences.get("language");
                        const allLanguage = mentor.publisher.project.getInformation();
                        // checking without language suffix variant
                        let id = mentor.publisher.urlParams.viewName;
                        let selectedLang = allLanguage.filter(function(item) {
                            return item.getId() == id;
                        });
                        // checking with language suffix variant
                        if (selectedLang.length === 0) {
                            id = id + '_' + language;
                            selectedLang = allLanguage.filter(function(item) {
                                return item.getId() == id;
                            });
                        }
                        if (selectedLang.length > 0) {
                            fileDisplayHandler.display({
                                id: selectedLang[0].id,
                                reset: true,
                                type: mentor.publisher.contentType.CUSTOM_VIEW
                            });
                        } else {
                            // We can change the message
                            alert(mentor.publisher.languageTranslator.localize("AlertViewNotFound").format(mentor.publisher.urlParams.viewName, "available item"));
                        }
                        that.options.success = "";
                    }
                    that.setPackageScreenVisible();
                    that.showViewerPage();
                    resizeViewer();
                }
            });
        },
        setSearchValue: function (searchText)
        {
            setTimeout(() => {
                $("#filterText").removeClass("placeHolderText");
                $("#filterText").attr("title", "");
                $("#filterText").val(searchText);
                selectedPackage.set("searchText", searchText);
            }, 100);
        },
        loadViewer: function (options) {
            var that = this;
            that.options = options || that.options || {};
            that.options.success = that.options.success || function () {
            };

            require(["titleupdater"], function (titleUpdater) {
                titleUpdater.startUpdateTitleForPackageSelectionScreen();
            });

            that.ensureEULAAcceptance({
                completion: function () {
                    var componentsLocation = $("body").attr("data-config");
                    mentor.publisher.dataLoader.loadServerConfig(function() {
                        that.loadLanguageTranslationServices();
                        that.loadComponents(componentsLocation, that);
                    });
                }
            });
        },

        showViewerPage: function () {
            $("#home-screen").hide();
            $("#applicationArea").addClass("screen").show();
            resizeViewer();
            selectedPackage.trigger("ShowViewerEvent");
        },

        setPackageScreenVisible: function () {
            if (this.options && this.options.success) {
                this.options.success();
            }

            require(["titleupdater"], function (titleUpdater) {
                titleUpdater.startUpdatingTitleForPackageScreen();
            });
        },

        /**
         * @return {boolean}
         */
        EULAVerified: function () {
            var windorObj = window;
            if (window.opener && window.opener.mentor) {
                windorObj = window.opener;
            }

            return windorObj.mentor.publisher.eulaVarified;

        },

        checkEULA: function () {
            var eula_acceptance = Utils.readCookie("eula_rev_210520") || this.EULAVerified() || "";
            return !!eula_acceptance;
        },

        showEULA: function (options) {
            eula.once("AcceptedEULA", function () {
                if (options.completion) {
                    options.completion();
                }
            });
            eula.render();

            $(".screen").hide();
            $("#eualContainer").show();
        },

        ensureEULAAcceptance: function (options) {
            if (!allowApplicationAccess) {
                this.showWarning("IncorrectLicense");
                return;
            }

            if (this.checkEULA() && options.completion) {
                options.completion();
            }
            else {
                this.showEULA(options);
            }
        },

        showHome: function () {
            var that = this;

            mentor.publisher.languageTranslator.loadResources(preferences.get("language"));

            that.ensureEULAAcceptance({
                completion: function () {
                    ComponentLoader.loadComponents("home-screen.xml", {
                        postRender: function () {
                            $(".screen").hide();
                            $("#home-screen").show();
                        }
                    });

                    require(["titleupdater"], function (titleUpdater) {
                        titleUpdater.startUpdateTitleForPackageSelectionScreen();
                    });
                }
            });
        },

        showWarning: function (messageKeyRoot) {
            require(["text!templates/warningTemplate.html"],
                    function (template) {
                        var warningMsg;

                        if (mentor.publisher.languageTranslator.isLoaded()) {
                            warningMsg = mentor.publisher.languageTranslator.localize(messageKeyRoot + "Message");
                        }
                        else {
                            warningMsg = "Unlicensed access to package content. " +
                                    "Package can only be opened by invoking {appExe}";
                        }
                        var renderedTemplate = underscore.template(template)({
                            message: warningMsg
                        });

                        $("body").html(renderedTemplate);
                    }
            );
        }
    });
});