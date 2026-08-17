/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor, alert, require*/
define("componentRouter", ["PackagesInSession", "fileDisplayHandler", "filters/attributeBasedFilter"],
        function (packagesInSession, fileDisplayHandler, attributeBasedFilter)
        {
            "use strict";
            var p = mentor.publisher;
            return {
                findProjectIdByName: function (packageName, effRange, projId, packageId) {
                    var allPackages,
                            matchingPackage;

                    allPackages = packagesInSession.get("packages");
                    var matchingSubpackage = allPackages.findSubPackageByParams({
                        packageName: packageName,
                        projId: projId,
                        effRange: effRange,
                        packageId: packageId
                    });
                    return matchingSubpackage;

                },
                findComponentByName: function (name, type, diagramName)
                {

                    return p.dataLoader.getObjectByName(name, type, diagramName);
                },
                findComponentById: function (id, type, diagramName)
                {
                    return p.dataLoader.getObjectById(id, type, diagramName);
                },
                getFirstArgumentBeforeQueryString: function (parameters)
                {
                    parameters = parameters || "";
                    if (parameters && parameters.split) {
                        return parameters.split("?")[0];
                    }
                },
                getAllObjectInstances: function (data)
                {
                    var objectXrefs = [], index, objects = data.systems;
                    for (index in objects) {
                        if (objects.hasOwnProperty(index)) {
                            objects[index].attributes.id = objects[index].attributes.systemId;
                            objectXrefs.push(objects[index].attributes);
                        }
                    }
                    data.systems = objectXrefs;
                    return data;

                },
                objectDataLoader: "",
                setObjectDataLoader: function (objDataLoader)
                {
                    this.objectDataLoader = objDataLoader;
                },
                findElementsByNameMatch: function (collection, itemToSearch)
                {
                    return attributeBasedFilter.filter(collection, itemToSearch) || [];
                },
                findElementsByExactNameMatch: function (collection, itemToSearch)
                {
                    return attributeBasedFilter.filterExact(collection, itemToSearch) || [];
                },
                showObjectPopover: function (itemToSearch, x, y, multipleObjectsData, xrefObj)
                {
                    display2DViewsAttributes(itemToSearch, x, y, null, multipleObjectsData);

                    setTimeout(function ()
                    {
                        if (xrefObj.allXrefs) {
                            var xrefsInOpenDiagram = [], openedDiagram = mentor.publisher.selectedSystem.get(
                                            "diagramId") ||
                                    (window.opener && window.opener.mentor ?
                                            window.opener.mentor.publisher.selectedSystem.get("diagramId") :
                                            "");

                            for (var xref in xrefObj.allXrefs) {
                                if (xrefObj.allXrefs[xref] && xrefObj.allXrefs[xref].diagramId === openedDiagram) {
                                    xrefsInOpenDiagram.push(xrefObj.allXrefs[xref].objectId);
                                }
                            }
                            for (var idToHighlight in xrefsInOpenDiagram) {
                                getWindowObj().crossHighlightHandler.highElementsInSVG(
                                        xrefsInOpenDiagram[idToHighlight]);
                            }
                        }
                    }, 200);

                },
                findElementInCollection: function (collection, itemToSearch, type)
                {
                    var matchedElements, selectedElement, objectType = "Component", multipleObjectsData, objDataLoader = this.objectDataLoader ||
                            p.objectDataLoader;
                    const viewArray = ['faultcode', 'diagnostic', mentor.publisher.contentType.JT_3D_MODEL, mentor.publisher.contentType.RA_3D_MODEL]
                    objectType = type || objectType;
                    if (collection && collection.partiallyLoaded) {
                        collection.loadAllObjects();
                    }
                    if (Utils.getUrlParameter('system') || Utils.getUrlParameter('harnesslayout') || viewArray.indexOf(Utils.getUrlParameter('view')) !== -1) {
                        matchedElements = this.findElementsByExactNameMatch(collection, itemToSearch);
                    } else {
                        matchedElements = this.findElementsByNameMatch(collection, itemToSearch);
                    }

                    if (matchedElements.length > 0) {
                        if (p.urlParams.internalLink && matchedElements.length > 1) {

                            var xrefs = objDataLoader.findXrefsByObjectId(matchedElements.map(function (ele)
                            {
                                return {
                                    systemId: ele.get("systemId"),
                                    objectId: ele.get("objectId")
                                }
                            }));

                            var filteredXrefs = xrefs;

                            filteredXrefs = filteredXrefs.map(function (xref)
                            {
                                var Model = Backbone.Model.extend(), model;
                                model = new Model();
                                model.set(xref, {silent: true});
                                return model;
                            });
                            multipleObjectsData = new Map();
                            multipleObjectsData.set(itemToSearch, filteredXrefs);
                            this.showObjectPopover(itemToSearch, p.urlParams.x, p.urlParams.y, multipleObjectsData,
                                    xrefs);

                            return false;
                        }
                        /*if(matchedElements.models) {
                         selectedElement = matchedElements.models[0];
                         } else {*/

                        //take the first system from matched systems
                        selectedElement = matchedElements[0];
                        //}
                    }
                    else {
                        alert(mentor.publisher.languageTranslator.localize("AlertObjTypeWithSearchItemNotFound").format(objectType, itemToSearch));
                    }
                    return selectedElement;
                },
                getConfig: function (parameters)
                {
                    var langAndConfig;
                    parameters = parameters || "";
                    if (parameters) {
                        langAndConfig = parameters.split("/");
                        return langAndConfig.length > 2 && langAndConfig[2];
                    }
                },
                getLanguage: function (parameters)
                {
                    var langAndConfig;
                    parameters = parameters || "";
                    if (parameters) {
                        langAndConfig = parameters.split("?");
                        return langAndConfig.length > 1 && langAndConfig[1];
                    }
                },
                showComponent: function (content)
                {
                    var resetViewerView, designObject, idValue;
                    if (p.urlParams.internalLink) {

                        p.eventDispatcher.dispatchEvent(p.events.OPEN_OBJECT_POPUP,
                                {
                                    id: content.objectId,
                                    x: p.urlParams.x,
                                    y: p.urlParams.y,
                                    systemId: content.systemId
                                });
                    }
                    else {
                        designObject = p.objectDataLoader.getDiagram(content);
                        resetViewerView = p.urlParams ? p.urlParams.reset : true;
                        if (designObject && designObject.type === p.contentType.HARNESS_LAYOUT_DIAGRAM &&
                                designObject.diagramId) {
                            require(["harnessLayouts", "models/selectedSystem"],
                                    function (harnessLayouts, selectedSystem)
                                    {
                                        selectedSystem.set("objectId", content.objectId, {silent: true});
                                        var itemToShow = harnessLayouts.getHarnessLayoutByDiagramId(
                                                designObject.diagramId);
                                        if (!itemToShow) {
                                            console.error('Diagram not found');
                                            return;
                                        }
                                        var type = harnessLayouts.getDesignType(itemToShow.design);

                                        fileDisplayHandler.display({
                                            layoutId: itemToShow.design.get("id"),
                                            listItemId: designObject.diagramId,
                                            group: "diagrams",
                                            reset: resetViewerView,
                                            doNotSaveAsHistory: true,
                                            type: type,
                                            mainText: itemToShow.diagram.mainText
                                        });
                                    });
                        }
                        else if (designObject && designObject.diagramId) {
                            idValue = designObject.systemId;
                            if (getWindowObj().diagramAsSystemsObjectFactoryImpl) {
                                idValue = designObject.diagramId;
                            }

                            fileDisplayHandler.display({
                                systemId: designObject.systemId,
                                id: idValue,
                                diagramId: designObject.diagramId,
                                reset: resetViewerView,
                                type: p.contentType.SYSTEM_SVG,
                                objectId: designObject.objectId
                            });

                            require(["SignalTracerModel"], function (signalTraceModel) {
                                signalTraceModel.updateData(designObject.systemId, designObject.objectId);
                            });
                        }
                        else {
                            alert(mentor.publisher.languageTranslator.localize("AlertObjNotExistInDia").format((p.urlParams.componentUID || p.urlParams.component)));
                        }
                    }

                },
                componentNotFound: function (name, type)
                {
                    alert(mentor.publisher.languageTranslator.localize("AlertObjByTypeNotExist").format(name, type));
                },
                checkifComponentTypeExists: function (name, type)
                {
                    //todo how to load object here
                    var objects = p.project.getObjects(type + "s");
                    if (!(objects && objects.length > 0)) {
                        this.componentNotFound(name, type);
                        return false;
                    }
                    return objects;
                },
                fetchComponentData: function (config, options)
                {
                    var componentLoaderLib = config.componentLoaderLib || require;
                    var componentLoader = componentLoaderLib("ComponentLoader");
                    var view = componentLoader.getComponentViewByName(options.componentType + "s");
                    if (view) {
                        var data = view.getData();
                        return data;
                    }
                },
                createComponentQuery: function (options)
                {
                    var attributes = {
                        name: "name",
                        value: options.componentName
                    };
                    if (options.uid) {
                        attributes = {
                            name: "uid",
                            value: options.uid
                        };
                    }
                    return attributes;
                },
                onComponentLoad: function (data, options)
                {
                    data = data || {};
                    var component = data.items;
                    if (component) {
                        component.systemId = component.systemUid;
                        this.showComponent(component);
                    }
                    else {
                        this.componentNotFound(options.componentName, options.componentType);
                    }
                },
                loadComponentDataToGetComponentUID: function (options, config)
                {

                    var that = this;
                    config = config || {};
                    var data = this.fetchComponentData(config, options);
                    if (data) {
                        options.uid = p.urlParams.componentUID;
                        var attributes = this.createComponentQuery(options);

                        data.fetchData
                        (
                                {
                                    method: "getObjectByAttribute",
                                    attributes: attributes,
                                    options: p.filter ? p.filter.vinOptions : ""
                                },
                                {
                                    success: function (data) {
                                        that.onComponentLoad(data, options);
                                    }
                                }
                        );
                    }
                },
                openComponent: function (options)
                {
                    // var component, that = this, selectedItem;   // not in use
                    options.componentType = options.componentType && options.componentType !== "undefined" && options.componentType.toLowerCase();
                    if (options.componentType) {
                        this.loadComponentDataToGetComponentUID(options);
                    } else {
                        const component = p.objectDataLoader.load("", options.componentName, p.project.getId());
                        const componentData = component && component.getCrossReferences && component.getCrossReferences();
                        if (componentData && componentData.listItems && componentData.listItems[0]) {
                            this.onComponentLoad({ items: componentData.listItems[0] });
                        }
                    }
                },
                loadProjectAndOpenComponent: function (options)
                {
                    var that = this;
                    if (options.projectId) {
                        p.router.loadProjectAndView({
                            projId: options.projId,
                            range: options.range,
                            projectId: options.projectId,
                            language: options.language,
                            config: options.config,
                            VIN: options.VIN,
                            query: options.query,
                            doNottriggerFirstSection: true,
                            success: function ()
                            {
                                if (options.config || options.VIN) {
                                    that.applyConfiguration(options, function () {
                                        that.openComponent(options);
                                    });
                                }
                                else {
                                    that.openComponent(options);
                                }
                            },
                            parameters: options.parameters
                        });
                    }
                    else {
                        //todo proper error message
                        alert(mentor.publisher.languageTranslator.localize("AlertProjCanNotBeLoaded").format(options.packageName));
                    }
                },
                findAndShowProjectData: function (packageName, type, parameters)
                {
                    var componentName;
                    componentName = decodeURIComponent(this.getFirstArgumentBeforeQueryString(parameters));
                    this.loadProjectData(packageName, componentName, parameters, type);
                },
                getParameterValue: function (parameters, name) {
                    var urlParams = p.urlParams || {};
                    var propValue = parameters[name] || urlParams[name];
                    return propValue && decodeURIComponent(propValue);
                },
                loadProjectData: function (packageName, componentName, parameters, type)
                {
                    var projectId, language, config, VIN;
                    packageName = decodeURIComponent(packageName);
                    var projId = this.getParameterValue(parameters, "projId");
                    var packageId = this.getParameterValue(parameters, "packageId");
                    var effRange = this.getParameterValue(parameters, "effRange");
                    var matchingSubPackage = this.findProjectIdByName(packageName, effRange, projId, packageId) || "";
                    if(matchingSubPackage) {
                        projectId = matchingSubPackage.id || "";
                        language = p.urlParams.language || "";
                        config = p.urlParams.config || "";
                        VIN = p.urlParams.VIN || "";
                        this.loadProjectAndOpenComponent({
                            projectId: projectId,
                            language: language,
                            componentName: componentName,
                            componentType: type,
                            config: config,
                            VIN: VIN,
                            query: p.urlParams.q || "",
                            packageName: packageName,
                            parameters: parameters,
                            projId: matchingSubPackage.projectId,
                            range: matchingSubPackage.effectivityRange,
                        });
                    } else {
                        //todo
                    }

                },
                applyConfiguration: function (theOptions, callback)
                {
                    var config = theOptions.config;
                    var vin = theOptions.VIN;
                    if (config) {
                        require(["ConfigurationsModel", "ConfigurationsCollection"],
                                function (ConfigurationsModel, ConfigurationsCollection)
                                {
                                    function onModelLoad()
                                    {
                                        var configObject = ConfigurationsCollection.getConfigurationByName(config);
                                        if (configObject && configObject.get("value")) {
                                            p.optionFilterPanel.VINFilterView();
                                            ConfigurationsModel.updateModel({
                                                currentTarget: {
                                                    getAttribute: function ()
                                                    {
                                                        return configObject.get("value");
                                                    }
                                                }
                                            }, "configurations");
                                            if (callback) {
                                                callback();
                                            }
                                        }
                                    }
                                    ConfigurationsModel.fetch({}, onModelLoad.bind(this));
                                });
                    }
                    else if (vin) {
                        setTimeout(function ()
                        {
                            var vinCallback;

                            if (callback) {
                                vinCallback = function () {
                                    mentor.publisher.eventDispatcher.removeEventListener(p.events.VIN_FILTER_APPLIED, vinCallback);
                                    callback();
                                };
                                mentor.publisher.eventDispatcher.attachEventListener(p.events.VIN_FILTER_APPLIED, vinCallback);
                            }

                            mentor.publisher.optionFilterPanel.setVINValue(vin);

                        }, 200);
                    }
                },
                isAlreadyEncoded:function(passedUrl){
                    let isEncoded = true;
                    if (typeof passedUrl === 'string' && !passedUrl.match(".*[\\ \"\\<\\>\\{\\}|\\\\^~\\[\\]].*")) {
                        isEncoded = false;
                    }
                    return isEncoded;

                },
                findAndShowComponentByType: function (packageName, componentName, parameters)
                {
                    var componentType = parameters.componentType;
                    if (!this.isAlreadyEncoded(componentName)) {
                        componentName = decodeURIComponent(componentName);
                    }
                    componentType =
                            componentType || decodeURIComponent(this.getFirstArgumentBeforeQueryString(parameters));
                    this.loadProjectData(packageName, componentName, parameters, componentType);
                },
                getComponentByNameAndType: function(componentName, componentType, systemUid, callback) {
                    const p = mentor.publisher;
                    let viewData = this.fetchComponentData({}, {componentType: componentType});
                    if (viewData) {
                        var attributes = [
                            {
                                name: "name",
                                value: componentName
                            }
                        ];
                        if (systemUid) {
                            attributes.push({
                                name: "systemUid",
                                value: systemUid
                            });
                        }
                        viewData.fetchData
                        (
                                {
                                    method: "getObjectsByAttribute",
                                    attributes: attributes,
                                    options: p.filter ? p.filter.vinOptions : ""
                                },
                                {
                                    success: function (data) {
                                        callback(data);
                                    }
                                }
                        );
                    }
                }
            };
        });