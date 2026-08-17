/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

/*global objectFactoryImpl, ActiveXObject, $, Msg, mentor, Utils, RenderConnectivityHandler, Constants, createOptionExpressions, window, navigator*/
mentor.publisher.cache = (function () {
    "use strict";
    var cachedObjects = {};
    return {

        storeObjectInCache: function (items) {
            var index, id;
            items = items || [];
            for (index = 0; index < items.length; index = index + 1) {
                id = items[index].idAttribute ||
                        (typeof items[index].getId === "function" ? items[index].getId() : (items[index].id || ""));
                if (id) {
                    cachedObjects[id] = items[index];
                }

            }
        },
        cachedObjects: cachedObjects
    };
}());

var xmlDataLoader = function (objectFactory) {
    "use strict";
    var cache = [], locationViews, xmlIdsDataLoaded, diagrams = {}, configurationDataLoaded, loadDiagrams,
            p = mentor.publisher,
            parseXMLDOMFor, vehicleConfigurationLoaded, signalDataLoadedForHighlight;

    vehicleConfigurationLoaded = function (data, textStatus, XMLHttpRequest, callback) {
        // displayHint('Vehicle Configuration Loaded', textStatus);
        var vehicleConfigDOM = $(data), vehicleConfigData = {}, i = 0, dataObject = {};

        function getAttributeValue(nodeAttributes, attributeName)
        {
            return nodeAttributes.getNamedItem(attributeName) && nodeAttributes.getNamedItem(attributeName).nodeValue
        }

        $('configurations', vehicleConfigDOM).each(function () {
            var childCount = this.childNodes.length;

            for (i = 0; i < childCount; i = i + 1) {
                var thisNode = this.childNodes[i];
                if (thisNode.nodeName !== "#text") {
                    var tagName = thisNode.tagName;
                    dataObject = {};
                    dataObject.type = tagName;

                    if (!vehicleConfigData[tagName]) {
                        vehicleConfigData[tagName] = [];
                    }
                    var attributes = thisNode.attributes;
                    if (tagName === mentor.publisher.constants.TypeConfigurtaion) {
                        dataObject.value = getAttributeValue(attributes, 'options');
                        dataObject.name = getAttributeValue(attributes, 'name');
                        dataObject[mentor.publisher.constants.customToolTipArrayLength] = 1;
                        dataObject[mentor.publisher.constants.customToolTip + '-0'] =
                                dataObject.name + "==" + dataObject.value;
                    }
                    else if (tagName === mentor.publisher.constants.TypeOption) {
                        dataObject.name = getAttributeValue(attributes, 'name');
                        var optionDescription = getAttributeValue(attributes, 'desc').trim();
                        if (optionDescription) {
                            dataObject.name += ' - ' + optionDescription;
                        }
                        dataObject.value = getAttributeValue(attributes, 'name');
                        dataObject.isVariant = getAttributeValue(attributes, 'variant');
                        dataObject.exclusiveOptions = getAttributeValue(attributes, 'exclusiveOptions');
                        dataObject.inclusiveOptions = getAttributeValue(attributes, 'inclusiveOptions')
                        dataObject.checked = false;
                        dataObject.disabled = false;
                    }
                    vehicleConfigData[tagName].push(dataObject);
                }
            }
        });

        require(["LocalConfigurations"], function (localConfiguration) {
            var configs = localConfiguration.getConfigs();
            var typeConfigurtaionKey = mentor.publisher.constants.TypeConfigurtaion;

            if (!vehicleConfigData[typeConfigurtaionKey] && configs && configs.length > 0) {
                vehicleConfigData[typeConfigurtaionKey] = [];
            }

            configs.forEach(function (item) {
                vehicleConfigData[typeConfigurtaionKey].push(item);
            });
            callback({textValue: textStatus, dataArray: vehicleConfigData});
        });
    };

    configurationDataLoaded = function (data, textStatus, XMLHttpRequest, callback) {
        var configData = $(data), configArray, i, configValue, configName;
        configArray = [];
        $('config', configData).each(function () {
            var childCount = this.childNodes.length;
            for (i = 0; i < childCount; i = i + 1) {
                if (this.childNodes[i].nodeName !== "#text") {
                    configValue = $(this.childNodes[i]).text() || $(this.childNodes[i]).attr("value");
                    configName = this.childNodes[i].tagName;
                    configArray[configName] = configValue;
                }
            }
        });

        callback({textValue: textStatus, dataArray: configArray});
    };

    loadDiagrams = function (type) {
        if (type === p.contentType.LOCATION_VIEWS) {
            p.locationViews.load();
            diagrams[type] = p.locationViews.getLocationViews();
        }
        return diagrams[type];
    };

    parseXMLDOMFor = function (elementTagName, indexXMLDOM, root, loadAllObjects) {
        root = root || "project";
        return objectFactory.createObjectsByType(elementTagName, $(root, indexXMLDOM.data), loadAllObjects);
    };

    /**
     * This is a private method
     */
    xmlIdsDataLoaded = function (data, textStatus, XMLHttpRequest, callback) {
        var locationObj, locationData, locationDataArray;

        locationData = $(data);
        locationDataArray = [];

        $('objects', locationData).each(function () {
            locationObj = {};
            locationObj.id = $("id", this).text();
            locationDataArray.push(locationObj);
        });
        callback({textValue: textStatus, dataArray: locationDataArray});
    };

    /**
     * This is a private method
     */
    signalDataLoadedForHighlight = function (data, textStatus, XMLHttpRequest, callback) {
        var objArray, signalData, signalDataArray;
        if (Utils.is_msie()) {
            data = mentor.publisher.xmlLoader.convertStringXMLToDOM(data);
        }

        signalData = $(data);
        signalDataArray = [];

        $('objects', signalData).each(function () {
            objArray = [];
            $(this).find("object").each(function () {
                objArray.push($(this).attr('id'));
            });
            signalDataArray.objArray = objArray;
        });

        callback(
                {
                    textValue: textStatus,
                    dataArray: signalDataArray
                }
        );
    };

    return {
        findSystemByName: function (systemName, diagramName) {
            var projectId = mentor.publisher.project.getId(), indexDOM, diagrams = [];
            indexDOM = this.loadIndexXML(projectId);
            if (indexDOM.data) {
                if (diagramName) {
                    $("system[name='" + systemName + "'] diagram[name='" + diagramName + "']",
                            indexDOM.data).each(function () {
                        diagrams.push({systemId: $(this).parent().attr("id"), diagramId: $(this).attr("id")})
                    });
                }
                else {
                    $("system[name='" + systemName + "']", indexDOM.data).each(function () {
                        var firstDiagram = $("diagram", this).first(), firstReport;
                        firstReport = $("report", this).first();
                        diagrams.push({
                            systemId: $(this).attr("id"),
                            diagramId: $(firstDiagram).attr("id"),
                            firstReport: $(firstReport).attr('name')
                        });
                    });
                }
                return diagrams.length > 0 && diagrams[0];
            }

        },
        getObjectByName: function (name, type, diagramName) {
            if (type === "systems") {
                return this.findSystemByName(name, diagramName);
            }
            return mentor.publisher.objectDataLoader.findObjectByName(name, type);
        },
        getObjectById: function (id, type, diagramName) {
            return mentor.publisher.objectDataLoader.findObjectByName("", type, id)
        },
        loadIndexXML: function (projectId) {
            var indexXML = mentor.publisher.pathResolver.getProjectXML(projectId);
            return mentor.publisher.xmlLoader.loadXMLByAjax(indexXML, false, true);
        },

        getProject: function (projectId) {
            var indexXMLDOM, id = projectId, lastLoadedObject = {}, storeObjectInCache, globalObjectCache = {};
            this.invalidateCache();

            indexXMLDOM = this.loadIndexXML(projectId);

            return {
                getSystems: function () {
                    if (globalObjectCache.systems) {
                        return globalObjectCache.systems;
                    }
                    globalObjectCache.systems = parseXMLDOMFor("system", indexXMLDOM);
                    p.cache.storeObjectInCache(globalObjectCache.systems);
                    return globalObjectCache.systems;
                },
                getObjectById: function (id) {
                    var element = p.cache.cachedObjects[id];
                    return element;
                },

                getReports: function (type) {
                    var customGeneratorWithSameType = [], index;

                    if (globalObjectCache[type]) {
                        return globalObjectCache[type];
                    }
                    globalObjectCache[type] = parseXMLDOMFor(type, indexXMLDOM);
                    /**
                     * is there any custom data with same type, if yes add its item also
                     * @type {*}
                     */
                    customGeneratorWithSameType = this.getData(type) || [];
                    if (customGeneratorWithSameType.length > 0) {
                        for (index in customGeneratorWithSameType) {
                            if (customGeneratorWithSameType.hasOwnProperty(index) &&
                                    customGeneratorWithSameType[index].mainText.indexOf("-meta.xml") < 0) {
                                globalObjectCache[type].push(customGeneratorWithSameType[index]);
                            }
                        }
                    }
                    p.cache.storeObjectInCache(globalObjectCache[type]);
                    return globalObjectCache[type];
                },
                getDiagrams: function (type) {
                    var diagramsLoaded = diagrams[type];
                    if (!diagramsLoaded) {
                        diagramsLoaded = loadDiagrams(type);
                        p.cache.storeObjectInCache(diagramsLoaded);
                    }
                    return diagramsLoaded;
                },
                getObjects: function (type, loadAllObjects) {
                    if (loadAllObjects) {
                        //reload all objects
                        globalObjectCache[type] = undefined;
                    }
                    globalObjectCache[type] =
                            globalObjectCache[type] ||
                            mentor.publisher.objectDataLoader.loadObjects(type, loadAllObjects);
                    return globalObjectCache[type];
                },
                getInformation: function () {
                    //todo add these tag names in a constant
                    return this.getReports('introduction-page');
                },
                getCache: function () {
                    return p.cache.cachedObjects;
                },
                getId: function () {
                    return id;
                },
                //loads design object data
                loadObjectData: function (systemId, objectUid) {
                    var object = lastLoadedObject && lastLoadedObject.getId &&
                            ((lastLoadedObject.getId() === objectUid) ? lastLoadedObject : "");
                    systemId = systemId;
                    if (!object) {
                        object = mentor.publisher.objectDataLoader.load(systemId, objectUid, this.getId());
                        lastLoadedObject = object;
                    }
                    return object;
                },
                getData: function (type, systemId, diagramId) {
                    //globalObjectCache[type] =
                    //globalObjectCache[type] || mentor.publisher.customGeneratorDataLoader.load(type, this.getId());
                    //return globalObjectCache[type];
                    //todo caching is temporarily disabled for this panel
                    //todo becase the name can be same as that of any other panel name  (like systems)
                    //todo could we store the type as 'custom' + type?
                    return mentor.publisher.customGeneratorDataLoader.load(type, this.getId(), systemId, diagramId);
                },

                createListGroups: function () {
                    var listGroups = [], length, index, navigationPanelObjects = [], navigationPanelObject;
                    return p.dataLoader.getNavigationPanelOrder(this.getId());
                },
                getFirstSection: function () {
                    var allSections = this.createListGroups() || [], index;
                    if (allSections.length > 0) {
                        for (index in allSections) {
                            if (allSections.hasOwnProperty(index) && allSections[index] &&
                                    allSections[index].listItems && allSections[index].listItems().length > 0) {
                                return allSections[index];
                            }
                        }
                        return allSections[0];
                    }
                },
                get: function (id) {
                    var items = this.getObjectById(id);
                    if (!items) {
                        items = this.getData(id);
                    }
                    return items;
                },
                getByType: function (type) {
                    return p.dataLoader.getNavigationPanelObjectMap()[type].listItems() || [];
                },
                getCustomData: function () {
                    var navigationPanelItems = p.dataLoader.getNavigationPanels(this.getId()), customPanels = [], index;
                    for (index in navigationPanelItems) {
                        if (navigationPanelItems.hasOwnProperty(index) &&
                                navigationPanelItems[index].name === "custompanel") {
                            customPanels.push(navigationPanelItems[index]);
                        }
                    }
                    return customPanels;
                }

            };
        },

        getNavigationPanelObjectMap: function () {
            var map = {};
            map.introductionPage = {
                listItems: function () {
                    return mentor.publisher.LanguageFilteredProject.filterInformationPages(
                            mentor.publisher.project.getInformation());
                },
                title: 'introduction-page'
            };

            map.system = {
                listItems: function () {
                    var sortedArray = mentor.publisher.project.getSystems() || [];
                    sortedArray = sortedArray.sort(Utils.sort);
                    return sortedArray;
                },
                title: 'system',
                type: mentor.publisher.contentType.SYSTEM_SVG
            };

            map.LocationViews = {
                listItems: function () {
                    return mentor.publisher.project.getDiagrams(p.contentType.LOCATION_VIEWS) || [];
                },
                title: 'locationViews',
                type: mentor.publisher.contentType.LOCATION_VIEWS
            };

            map.JT = {
                listItems: function () {
                    return mentor.publisher.project.getData(p.contentType.JT_3D_MODEL) || [];
                },
                title: mentor.publisher.contentType.JT_3D,
                type: mentor.publisher.contentType.JT_3D
            };

            map.RA = {
                listItems: function () {
                    return mentor.publisher.project.getData(p.contentType.RA_3D_MODEL) || [];
                },
                title: mentor.publisher.contentType.RA_3D,
                type: mentor.publisher.contentType.RA_3D
            };

            map.faultcode = {
                listItems: function () {
                    return mentor.publisher.project.getReports(p.contentType.FAULT_CODE);
                },
                title: 'faultcode',
                type: mentor.publisher.contentType.FAULT_CODE
            };

            map.commonFaultCodes = {
                listItems: function () {
                    return [{
                        getName: function () {
                            return mentor.publisher.languageTranslator.localize('commonFaultCode.MainTitle');
                        },
                        getId: function () {
                            return "Common Fault Codes";
                            ;
                        },
                        showToolTipAlways: true,
                        mainText: mentor.publisher.languageTranslator.localize('commonFaultCode.MainTitle'),
                        id: "Common Fault Codes",
                        category: mentor.publisher.documentCategory.TROUBLESHOOT,
                    }];
                },
                title: 'commonFaultCodes',
                type: mentor.publisher.contentType.COMMON_FAULT_CODE
            }

            map.diagnostics = {
                listItems: function () {
                    var result = [];

                    var url = mentor.publisher.project.getId() + "/diagnostics.json";
                    var saneUrl = Utils.prepareFilePath(url);
                    $.ajax({
                        url: saneUrl,
                        async: false,
                        success: function (data, textStatus, xhr) {
                            result = data;
                        },
                        dataType: "json",
                        mimeType: "application/json"
                    });

                    result.forEach(function (diagnostic) {
                        diagnostic.type = "diagnostic";
                    });

                    return result;
                },
                title: 'diagnostics',
                type: mentor.publisher.contentType.DIAGNOSTIC
            };

            map.harness = {
                listItems: function () {
                    return mentor.publisher.project.getReports(p.contentType.HARNESS);
                },
                type: mentor.publisher.contentType.HARNESS, title: 'harness'
            };

            map.wires = {
                listItems: function () {
                    return [];
                },
                title: 'WiresTitle'
            };

            map.nets = {
                listItems: function () {
                    return [];
                },
                title: 'NetsTitle'
            };

            map.connectors = {
                listItems: function () {
                    return [];
                },
                title: 'ConnectorTitle'
            };

            map.multicores = {
                listItems: function () {
                    return [];
                },
                title: 'MulticoresTitle'
            };

            map.devices = {
                listItems: function () {
                    return [];
                },
                title: 'DevicesTitle'
            };

            map.splices = {
                listItems: function () {
                    return [];
                },
                title: 'SplicesTitle'
            };

            map.inlines = {
                listItems: function () {
                    return [];
                },
                title: 'InlinesTitle'
            };

            map.grounds = {
                listItems: function () {
                    return [];
                },
                title: 'GroundsTitle'
            };

            map.capitalreport = {
                listItems: function () {
                    let result = [];
                    const url = mentor.publisher.project.getId() + "/grounds.json";
                    var saneUrl = Utils.prepareFilePath(url);
                    $.ajax({
                        url: saneUrl,
                        async: false,
                        success: function (data, textStatus, xhr) {
                            result = data;
                        },
                        dataType: "json",
                        mimeType: "application/json"
                    });

                    return result;
                },
                title: 'GroundsTitle'
            };

            map.globalreports = {
                listItems: function () {
                    return mentor.publisher.project.getObjects(p.contentType.GLOBAL_GROUND_REPORT);
                }, /*eventHandler: p.globalObjectEventHandler,*/
                title: p.contentType.GLOBAL_GROUND_REPORT,
                paginated: true,
                type: mentor.publisher.contentType.CAPITAL_REPORT
            };

            map.harnesslayout = {
                title: "harness-layouts",
                listItems: function () {
                    var harnessLayouts = require("harnessLayouts"), items = [];
                    if (harnessLayouts.models) {
                        for (var i = 0; i < harnessLayouts.models.length; i++) {
                            items.push(harnessLayouts.models[i].getContent());
                        }
                    }
                    return items;
                }
            };

            return map;
        },
        getNavigationPanelObject: function (name, type) {
            if (name === 'introduction-page') {
                name = 'introductionPage';
            }
            var navigationPanelObject = this.getNavigationPanelObjectMap()[name];
            //for custom panels
            if (!navigationPanelObject) {
                navigationPanelObject = {
                    listItems: function () {
                        return mentor.publisher.project.getData(type);
                    }, /*eventHandler: p.customGeneratorDataEventHandler(),*/
                    title: type
                };
            }
            return navigationPanelObject;
        },
        getNavigationPanelOrder: function (projectRoot) {
            //todo load the tooltip attributes which should be shown in panel
            var navConfigFilePath = projectRoot + '/' +
                            "Resources/Config/NavigationPanel.xml", navPanelXMLDOM, navigationSubPanels = [],
                    navigationPanelObjects = [], loader = this, navPanelMap = {};
            navPanelXMLDOM = mentor.publisher.xmlLoader.loadXMLByAjax(navConfigFilePath, false, false);
            if (navPanelXMLDOM.data) {
                $('NavigationPanel', navPanelXMLDOM.data).each(function () {
                    var childCount = this.childNodes.length, i, subPanel, navPanelObject, attributes, properties;
                    for (i = 0; i < childCount; i = i + 1) {
                        subPanel = {};
                        attributes = [];
                        properties = [];
                        if (this.childNodes[i].nodeName !== "#text") {
                            subPanel.name = this.childNodes[i].tagName;
                            subPanel.type = $(this.childNodes[i]).attr('type');
                            //don't load RA type if browser is IE11
                            if (!subPanel.type ||
                                    !(subPanel.type === 'Rapid Author 3D Catalog Model' && Utils.is_msie())) {
                                navPanelObject = loader.getNavigationPanelObject(subPanel.name, subPanel.type);
                                if (!navPanelMap[navPanelObject.title]) {
                                    navigationPanelObjects.push(loader.getNavigationPanelObject(subPanel.name,
                                            subPanel.type));
                                    navPanelMap[navPanelObject.title] = true;
                                    navigationSubPanels.push(subPanel);
                                }
                            }
                        }
                    }
                });

            }
            return navigationPanelObjects;
        },
        getNavigationPanels: function (projectRoot) {
            //todo load the tooltip attributes which should be shown in panel
            var navConfigFilePath = projectRoot + '/' +
                    "Resources/Config/NavigationPanel.xml", navPanelXMLDOM, navigationSubPanels = [], loader = this;
            navPanelXMLDOM = mentor.publisher.xmlLoader.loadXMLByAjax(navConfigFilePath, false, false);
            if (navPanelXMLDOM.data) {
                $('NavigationPanel', navPanelXMLDOM.data).each(function () {
                    var childCount = this.childNodes.length, i, subPanel, attributes, properties;
                    for (i = 0; i < childCount; i = i + 1) {
                        subPanel = {};
                        attributes = [];
                        properties = [];
                        if (this.childNodes[i].nodeName !== "#text") {
                            subPanel.name = this.childNodes[i].tagName;
                            subPanel.type = $(this.childNodes[i]).attr('type');
                            var genKey = $(this.childNodes[i]).attr('genKey');
                            if(genKey) {
                                subPanel.genKey = genKey;
                            }
                            // navigationPanelObjects.push(loader.getNavigationPanelObject(subPanel.name,
                            // subPanel.type));
                            navigationSubPanels.push(subPanel);
                        }
                    }
                });

            }
            return navigationSubPanels;
        },
        getRelatedDataOrder: function (projectRoot, designRoot, diagramId) {
            var relatedDataOrderFilePath = mentor.publisher.pathResolver.getRelatedDataOrderFilePath(projectRoot,
                    designRoot, diagramId), relatedDataPanelXMLDOM, navigationSubPanels = [];
            relatedDataPanelXMLDOM = mentor.publisher.xmlLoader.loadXMLByAjax(relatedDataOrderFilePath, false, false);
            if (relatedDataPanelXMLDOM.data) {
                $('RelatedDataOrder', relatedDataPanelXMLDOM.data).each(function () {
                    var childCount = this.childNodes.length, i, type;
                    for (i = 0; i < childCount; i = i + 1) {
                        if (this.childNodes[i].nodeName !== "#text") {
                            type = $(this.childNodes[i]).attr('type');
                            navigationSubPanels.push(type);
                        }
                    }
                });
            }
            return navigationSubPanels;
        },
        getWindowTitleConfigData: function () {
            var arrtributeNames = [], configUrl = (Utils.prepareFilePath(p.project.getId() +
                            '/Resources/Config/config.xml')), delimiter, autoFitSVGOnWindowResize, showPathForFaceViews,
                    showPathFor2dViews, result;
            if (!cache[configUrl]) {
                result = {};
                $.ajax({
                    url: configUrl,
                    success: function (data, textStatus, XMLHttpRequest) {
                        delimiter = $("splitPanelView delimiter", data).attr("value");
                        autoFitSVGOnWindowResize = $("splitPanelView autoFitSVGOnWindowResize", data).attr("value");
                        showPathForFaceViews = $("splitPanelView showPathForFaceViews", data).attr("value");
                        showPathFor2dViews = $("splitPanelView showPathFor2dViews", data).attr("value");
                        $("splitPanelView windowTitleAttributes attributeName", data).each(function () {
                            arrtributeNames.push($(this).attr("value"));
                        });
                    },
                    error: function (XMLHttpRequest, textStatus, errorThrown) {
                        delimiter = "";
                        arrtributeNames.push("name");
                    }, dataType: (Utils.is_msie()) ? "text" : "xml", async: false
                });
                result = {
                    attributeNames: arrtributeNames,
                    delimiter: delimiter,
                    autoFitSVGOnWindowResize: autoFitSVGOnWindowResize && autoFitSVGOnWindowResize == "true",
                    showPathForFaceViews: showPathForFaceViews,
                    showPathFor2dViews: showPathFor2dViews
                };
                cache[configUrl] = result;
            }

            return cache[configUrl];
        },
        getDesignObjects: function (systemId, objectType, xmlNodeName) {

            var filePath = p.pathResolver.getSystemObjectDataFilePath(p.project.getId(), systemId,
                    objectType), vinOptions, result;
            xmlNodeName = xmlNodeName || "object";
            result = p.xmlLoader.loadXMLByAjax(filePath, false, false);
            if (result.data) {
                return parseXMLDOMFor(xmlNodeName, result, objectType);
            }
        },
        getFaceViewSymbol: function (symbol, systemId, projectId) {
            return p.pathResolver.getFaceViewSymbol(symbol, systemId, projectId);
        },
        getCavityTable: function (table, systemId, projectId) {
            return p.pathResolver.getCavityTable(table, systemId, projectId);
        },
        getCustomPopoverSectionOrder: function () {
            if (!this.customPopoverSectionOrder) {
                this.customPopoverSectionOrder = this.readAndParseObjectPopupConfig(function (objectDOMEntry) {
                    var popElementArray = [];
                    var popupElements = $(objectDOMEntry).children();
                    var len = popupElements.length;
                    for (var j = 0; j < len; j = j + 1) {
                        var popElement = popupElements[j];
                        var xmlItem = popElement.tagName;
                        if (xmlItem.toLowerCase() === "customdata") {
                            popElementArray.push($(popElement).attr('title'));
                        }
                    }
                    return popElementArray;
                });
            }
            return this.customPopoverSectionOrder;
        },
        getPopoverOrder: function () {
            if (!this.popoverOrder) {
                var cache = {};
                this.popoverOrder = this.readAndParseObjectPopupConfig(function (objectDOMEntry) {
                    var popElementArray = [];
                    var popupElements = $(objectDOMEntry).children();
                    var len = popupElements.length;
                    for (var j = 0; j < len; j = j + 1) {
                        var popElement = popupElements[j];
                        //To eliminate duplicate
                        if (!cache[popElement.tagName]) {
                            popElementArray.push(popElement.tagName);
                            cache[objectDOMEntry.tagName + popElement.tagName] = true;
                        }
                    }
                    return popElementArray;
                });
            }
            return this.popoverOrder;
        },
        getObjectPropertyToUseForTitle: function (objectType) {
            if (!this.popoverObjectTitleProp) {
                this.popoverObjectTitleProp = this.readAndParseObjectPopupConfig(function (objectDOMEntry) {
                    return $(objectDOMEntry).attr("title") || "";
                });
            }
            return this.popoverObjectTitleProp[objectType] || "";
        },
        readAndParseObjectPopupConfig: function (parserCallback) {
            if (!parserCallback) {
                return;
            }
            var configFilePath = p.pathResolver.getPopoverConfigFilePath(
                            p.project.getId()), popOverDOM, i, configs, length, config, popupElements, j, len, popElement,
                    popElementArray, configMap = {};
            popOverDOM = p.xmlLoader.loadXMLByAjax(configFilePath, false, true);
            if (popOverDOM.data) {
                configs = $("Popup>*", popOverDOM.data);
                length = configs.length;
                for (i = 0; i < length; i = i + 1) {
                    config = configs[i];
                    configMap[config.tagName] = parserCallback(config);
                }
                return configMap;
            }
        },
        getSignalObjects: function (signalName, systemId) {

            var systemPath = p.pathResolver.getSystemPath(systemId ||
                    p.selectedSystem.get("systemId"),
                    p.project.getId()), signalFilePath, xmlDOM, uids = [], uidDomArray, length, i, uidDom, id;
            signalFilePath = p.pathResolver.getSignalPath(systemPath, signalName);
            xmlDOM = p.xmlLoader.loadXMLByAjax(signalFilePath, false, false);
            if (xmlDOM.data) {
                uidDomArray = $("object", xmlDOM.data);
                length = uidDomArray.length;
                for (i = 0; i < length; i = i + 1) {
                    uidDom = uidDomArray[i];
                    id = $(uidDom).attr('id') || "";
                    if (id) {
                        //////console.log("high " + id);
                        uids.push(id);
                    }

                }
                return uids;
            }

        },
        getSignalDataForHighlightInRenderedSVG: function (signalName, callback) {

            var filePath = (Utils.prepareFilePath(p.project.getId() + '/GlobalSignals/' + signalName + '.xml'));
            $.ajax(
                    {
                        url: filePath,
                        success: function (data, textStatus, XMLHttpRequest) {
                            signalDataLoadedForHighlight(data, textStatus, XMLHttpRequest, callback);
                        },
                        error: function (XMLHttpRequest, textStatus, errorThrown) {
                            callback();

                        },
                        dataType: (Utils.is_msie()) ? "text" : "xml"
                    }
            );
        },
        parseXMLForTag: function (elementTagName, indexXMLDOM, root, loadAllObjects) {
            return parseXMLDOMFor(elementTagName, indexXMLDOM, root, loadAllObjects);
        },
        loadPackages: function () {
            var filePath = p.pathResolver.getPackagesFilePath(), xmlDOM, packageNodes, i, len, packages = [];
            xmlDOM = p.xmlLoader.loadGlobalFile(filePath, false, false);
            if (xmlDOM.data) {
                packageNodes = $("package", xmlDOM.data);
                len = packageNodes.length;
                for (i = 0; i < len; i = i + 1) {
                    packages.push({
                        name: $(packageNodes[i]).attr('name'),
                        id: $(packageNodes[i]).attr('id'),
                        mainText: $(packageNodes[i]).attr('name')
                    });
                }

            }
            else {
                xmlDOM.data = null;
            }
            return packages;
        },
        get3dXmlId: function (currentProject, currentFolder, fileName, callback) {
            var filePath, xmlDOM;
            filePath =
                    (currentProject + '/' + currentFolder + '/O/' + fileName +
                            '.xml');
            xmlDOM = p.xmlLoader.loadXMLByAjax(filePath, false, false);
            if (xmlDOM.data) {
                xmlIdsDataLoaded(xmlDOM.data, "", "", callback);
            }
        },
        loadConfigurationData: function (currentProject, callback) {
            var url = (currentProject + '/Resources/Config/config.xml'), xmlDom;
            var self=this;
            xmlDom = p.xmlLoader.loadXMLByAjax(url, false, false);
            if (xmlDom.data) {
                configurationDataLoaded(xmlDom.data, "", "", callback);
            }
        },
        loadFaultCodeById: function (projectId, faultCodeId) {
            var indexXML = projectId + "/index.xml", xmlDom, objects = [], object;
            xmlDom = mentor.publisher.xmlLoader.loadXMLByAjax(indexXML, false, true);
            if (xmlDom.data) {
                $("faultcode[id='" + faultCodeId + "'] >objectData", xmlDom.data).each(function () {
                    var objectExpression, designOpExp, name = $(this).attr('name');
                    $("data", this).each(function () {
                        object = {};
                        object.objectName = name;
                        object.objectId = $(this).attr('uid');
                        object.path = $(this).attr('path');
                        object.systemId = $(this).attr('designId');
                        designOpExp = $(this).attr('designOptions');
                        objectExpression = $(this).attr('optionExpression');
                        object.designOptions = designOpExp;
                        object.objectOptions = objectExpression;
                        object.designName = $(this).attr('designName');
                        object.diagramName = $(this).attr('diagramName');
                        object.diagramId = $(this).attr('diagramId');
                        object.optionExpression = createOptionExpressions(designOpExp, objectExpression, "&&");
                        objects.push(object);

                    });
                });
            }
            return objects;

        },
        loadVehicleConfigObject: function (currentProject, callback) {
            var url = currentProject + '/vehicleconfig.xml', xmlDom;
            xmlDom = p.xmlLoader.loadXMLByAjax(url, false, false);
            if (xmlDom.data) {
                vehicleConfigurationLoaded(xmlDom.data, "", "", callback);
            }
        },
        loadOptionFilterInfo: function (projectId) {
            var optionsFile = p.pathResolver.getOptionsFile(projectId), xmlDOM;
            xmlDOM = p.xmlLoader.loadXMLByAjax(optionsFile, false, false);
            if (xmlDOM.data) {
                return {
                    vin: $("filterType>vin", xmlDOM.data).text(),
                    config: $("filterType>config", xmlDOM.data).text()
                };
            }
            else {
                return {};
            }
        },
        getProjectPreferences: function (projectId) {
            var path = p.pathResolver.getProjectPreferencesFilePath(projectId), content;
            content = p.xmlLoader.loadFile(path, false, true, "json") || {}
            content.data = content.data || {};
            content.data.hookupConnectOntoMulticore = content.data.hookupConnectOntoMulticore || false;
            content.data.hookupConnectOntoMulticore = content.data.hookupConnectOntoMulticore || false;
            content.data.hookupConnectOntoOverbraid = content.data.hookupConnectOntoOverbraid || false;
            return content.data;
        },
        invalidateCache: function () {
            diagrams = {};
        }

    };

};

mentor.publisher.pathResolver = (function () {
    "use strict";

    return {
        getPackagesFilePath: function () {
            return "unzipped/data/packages.xml";
        },
        getVersionFilePath: function (projectId) {
            return projectId + "/version.json";
        },
        getProjectPreferencesFilePath: function (projectId) {
            return projectId + "/projectpreferences.json";
        },
        getProjectXML: function (projectId) {
            return projectId + "/index.xml";
        },
        getSystemPath: function (systemid, projectId) {
            var shortenedDesignUID = mentor.publisher.shortenedUIDMap.getShortenedDesignUID(systemid, projectId);
            return projectId + "/" + shortenedDesignUID + "/";
        },
        getDiagramPath: function (diagramid, systemid, projectId) {
            return this.getSystemPath(systemid, projectId) + diagramid + "/";
        },
        getObjectUIDMapPath: function (systemId, projectId) {
            if (mentor.publisher.objectDataLoader.areUIDsStoredInBuckets(projectId)) {
                return projectId + "/O/" + "uidMap.xml";
            }
            else {
                return this.getSystemPath(systemId, projectId) + "O/" + "uidMap.xml"
            }
        },
        getDesignUIDMapPath: function (projectId) {
            return projectId + "/" + "uidMap.xml";
        },
        getObjectPath: function (systemId, objectId, projectId, uidBucket) {
            return projectId + "/O/" + uidBucket + "/" + objectId + ".xml";
        },
        getSystemObjectDataFilePath: function (projectId, systemId, objectType) {
            return this.getSystemPath(systemId, projectId) + objectType + ".xml";
        },
        getRelatedDataOrderFilePath: function (projectId, systemId, diagramId) {
            if (diagramId) {
                return this.getDiagramPath(diagramId, systemId, projectId) + "Resources/RelatedDataOrder.xml";
            }
            return this.getSystemPath(systemId, projectId) + "Resources/RelatedDataOrder.xml";
        },
        getFaceViewSymbol: function (symbol, systemId, projectId) {
            symbol = symbol || "";
            if (mentor.publisher.objectDataLoader.areUIDsStoredInBuckets(projectId)) {

                return projectId + "/FaceViews/" + symbol.replace(".html", ".svg");
            }
            return this.getSystemPath(systemId, projectId) + "FaceViews/" + symbol.replace(".html", ".svg");
        },
        getCavityTable: function (cavityTable, systemId, projectId) {
            if (mentor.publisher.objectDataLoader.areUIDsStoredInBuckets(projectId)) {

                return projectId + "/FaceViews/" + cavityTable;
            }
            return this.getSystemPath(systemId, projectId) + "FaceViews/" + cavityTable;
        },
        getLocationViewsFilePath: function (projectId) {
            return projectId + "/" + "LocationViews.xml";
        },
        getPopoverConfigFilePath: function (projectRoot) {
            return projectRoot + '/' +
                    "Resources/Config/popup.xml";
        },
        getSignalPath: function (systePath, signalName) {
            return systePath + "Signals" + "/" + signalName + ".xml";
        },
        getGlobalObjectFilePath: function (type, projectId) {
            return projectId + "/" + type + ".xml";
        },
        getLanguageDictionaryFilePath: function (projectId) {
            return projectId + "/" + "langdictionary" + ".xml";
        },
        getCustomGeneratorFilePath: function (name, projectId, systemId, diagramUid) {
            if (projectId && systemId && diagramUid) {
                //todo need to store this data at diagram level?
                return this.getDiagramPath(diagramUid, systemId, projectId) + name + ".xml";
            }
            else if (projectId && systemId) {
                return this.getSystemPath(systemId, projectId) + name + ".xml";
            }
            else if (projectId) {
                return projectId + '/' + name + ".xml";
            }
        },
        getOptionsFile: function (projectId) {
            return projectId + "/" + "options.xml";
        }

    };
}());

mentor.publisher.referenceObjectParser = function (objectDataDOM, systemId, objectUid) {
    "use strict";
    return {
        getReferenceIds: function () {
            if (this.shouldRedirect()) {
                var objectUIDs = [];
                $("ref", objectDataDOM).each(function () {
                    objectUIDs.push($(this).attr("id"));
                });
                if (objectUIDs.length === 1) {
                    return objectUIDs[0];
                }
                else {
                    return objectUIDs;
                }
            }
            else {
                return objectUid;
            }
        },
        hasMultipleUIDRefs: function () {
            return $("ref", objectDataDOM).length > 1;
        },
        shouldRedirect: function () {
            var reference = $("ref", objectDataDOM);
            return Utils.notNull(reference) && Utils.notNull(reference.attr("id"));
        }
    };
};

function createXrefsFilter(opts)
{
    var openedDiagrams = [],
            selectedSystem = getWindowObj().mentor.publisher.selectedSystem,
            NEW_DESIGN_REVISION = getWindowObj().mentor.publisher.contentType.NEW_DESIGN_REVISION,
            OLD_DESIGN_REVISION = getWindowObj().mentor.publisher.contentType.OLD_DESIGN_REVISION;

    opts = opts || {};
    opts.includeOpenedDiagrams = opts.includeOpenedDiagrams || false;
    if (opts.includeUnopenedDiagrams === undefined) {
        opts.includeUnopenedDiagrams = true;
    }

    if (opts.includeOpenedDiagrams && opts.includeUnopenedDiagrams) {
        return function (ref) {
            return true;
        }
    }

    if (!opts.includeOpenedDiagrams && !opts.includeUnopenedDiagrams) {
        return function (ref) {
            return false;
        }
    }

    var openDiagramId = selectedSystem.get("diagramId");
    if (openDiagramId) {
        openedDiagrams.push(openDiagramId.trim());
    }

    if (selectedSystem.has(OLD_DESIGN_REVISION)) {
        openedDiagrams.push(selectedSystem.get(OLD_DESIGN_REVISION).get("id").trim());
    }
    if (selectedSystem.has(NEW_DESIGN_REVISION)) {
        openedDiagrams.push(selectedSystem.get(NEW_DESIGN_REVISION).get("id").trim());
    }

    return function (xref) {
        var filterProp = opts.filterProp || "diagramId";
        var diagram = xref[filterProp];
        var index = openedDiagrams.indexOf(diagram);
        return opts.includeUnopenedDiagrams ? index < 0 : index >= 0;
    }
}

mentor.publisher.objectDataParser = function (objectDataDOM, systemId, objectUid) {
    "use strict";
    var p = mentor.publisher, attributeParser = p.attributeParser();

    return {
        get: function (type) {
            var sectionValue;
            $("section[id='" + type + "']", objectDataDOM).each(function () {
                sectionValue = JSON.parse($(this).attr("value"));
            });
            return sectionValue;
        },
        getOptionExpression: function () {
            return $("attributes", objectDataDOM).attr("optonExpression") || "";
        },
        getAttributes: function () {
            var attributes = [], events = [];
            $("attributes>attribute", objectDataDOM).each(function () {
                attributeParser(this, {
                    callback: function (attr) {
                        attributes.push(attr);
                    }
                });

            });

            return {
                listItems: attributes,
                events: events,
                title: mentor.publisher.languageTranslator.localize('AttributesTitle'),
                listItemObject: "attrListItem"
            };
        },
        getName: function () {
            return $("attributes", objectDataDOM).attr("objectName") || $("object", objectDataDOM).attr('objectName');
        },
        getCrossReferences: function (opts) {
            var xrefs = [],
                    system,
                    tooltips,
                    filteredItems,
                    listItems,
                    doNotStoreTwoInstancesInSameDiagram = {};

            opts = opts || {};
            opts.includeOpenedDiagrams = opts.includeOpenedDiagrams || false;

            $("xrefs>xref", objectDataDOM).each(function () {
                        var id = $("id", this).text(), xref, name = $("name",
                                        this).text(), shortDesc, diaId, designId, opExp,
                                schemId, objectToolTips = [], type, illustratorDesignType;
                        type =
                                $("attributes", objectDataDOM).attr("simpleName") ||
                                $("object", objectDataDOM).attr("simpleName");
                        ;
                        designId = $("designId", this).text();
                        system = mentor.publisher.project.getObjectById(designId);
                        //no short description for xref
                        shortDesc = system.subText;
                        diaId = $("diagramId", this).text();
                        tooltips = system.getToolTips();
                        objectToolTips = objectToolTips.concat(tooltips);
                        objectToolTips.push((function (objecttype) {
                            return {
                                getName: function () {
                                    return mentor.publisher.languageTranslator.localize("Type");
                                },
                                getValue: function () {
                                    return mentor.publisher.languageTranslator.localize(objecttype);
                                }

                            }
                        })(type));

                        opExp = $("optionExpression", this).text();
                        schemId = $("schemUID", this).text();
                        illustratorDesignType = $("illustrator-design-type", this).text();
                        xref = {
                            mainText: name,
                            subText: shortDesc,
                            showPopoutButton: true,
                            id: designId,
                            diagramId: diaId,
                            idToHighlight: designId,
                            getSchemeId: function () {
                                return schemId;
                            },
                            systemId: designId,
                            getOptionExpression: function () {
                                return opExp;
                            },
                            objectId: id,
                            idAttribute: designId,
                            getToolTips: (function (tooltips) {
                                return function () {
                                    return tooltips;
                                };
                            }(objectToolTips)),
                            illustratorDesignType: illustratorDesignType
                        };
                        //if there are two instances in same diagram then only store one.
                        if (!doNotStoreTwoInstancesInSameDiagram[diaId]) {
                            xrefs.push(xref);
                            doNotStoreTwoInstancesInSameDiagram[diaId] = xref;
                        }

                    }
            );
            filteredItems = p.filter.applyFilter(xrefs);
            listItems = filteredItems.filter(createXrefsFilter(opts));
            return {
                listItems: listItems,
                title: mentor.publisher.languageTranslator.localize('XRefTitle'),
                textFilterable: true,
                xrefs: xrefs,
                filterEvents: [
                    p.events.APPLY_CONFIGURATION_FILTER_ON_POPOVER
                ]
            };
        },
        getHarnessLayouts: function () {
            var xPath = "section[id='harnessLayouts']>item";
            return this.getRelatedObjects(xPath);
        },

        getElementsByXPath: function (xPath) {
            var relatedObjects = [];
            $(xPath, objectDataDOM).each(function () {
                var relObject = {};
                relObject.objectids = [];
                $("object", this).each(function () {
                    relObject.objectids.push($(this).attr("id"));
                });
                relObject.objectId = relObject.objectids.length > 0 && relObject.objectids[0];
                relObject.logicObjectId = $("object", this).attr("logicId");
                relObject.id = $(this).attr("id");
                relObject.diagramId = $(this).attr("diagramId");
                relObject.mainText = $(this).attr("name");
                relObject.path = $(this).attr("path");
                relObject.objectuId = objectUid;
                relatedObjects.push(relObject);
            });
            return relatedObjects;
        }, getRelatedObjects: function (xPath) {
            xPath = xPath || "section[id='relatedObjects']>item";
            return this.getElementsByXPath(xPath);
        },
        getShieldBodyUIDs: function () {
            var shieldBodyDataArray = [];
            $('shieldbody', objectDataDOM).each(function () {
                var shieldBodyObject = {};
                shieldBodyObject.id = $("id", this).text();
                shieldBodyDataArray.push(shieldBodyObject);
            });
        },

        getCustomData: function () {
            var customDataMap = {};
            $("customeObjectData", objectDataDOM).each(function () {
                var customData, title, isReportWithHotSpot;
                title = $(this).attr("title") || "";
                if (!customDataMap[title]) {
                    customDataMap[title] = {
                        title: title,
                        listItems: []
                    };
                }
                customData = customDataMap[title];

                isReportWithHotSpot = $(this).attr("report") || false;
                $("objectDataEntry", this).each(function () {
                    var name, path, entry;
                    name = $("name", this).text() || "";
                    path = $("path", this).text() || "";
                    //remove .html extenstion from name
                    if (name) {
                        name = name.replace(".html", "");
                    }
                    entry = {
                        mainText: name,
                        path: path,
                        systemId: systemId,
                        objectId: objectUid,
                        objectReport: isReportWithHotSpot,
                        showPopoutButton: true
                    };
                    //XMLs are not supported
                    if (path && !path.endsWith(".xml")) {
                        customData.listItems.push(entry);
                    }
                });
            });
            return _.values(customDataMap)
                    .filter(function (customData) {
                        return customData.listItems.length > 0;
                    });
        },

        get3DViews: function () {
            var view3ds = [];
            var jtPartIds = [];
            var raItemIds = [];
            $("diagrams>diagram", objectDataDOM).each(function () {
                var view3d = {}, name, mainText;
                name = $("name", this).text() || "";
                view3d.showPopoutButton = true;
                view3d.path = $("path", this).text() || "";
                view3d.type = $("type", this).text() || "";
                mainText = view3d.path.substring(view3d.path.lastIndexOf("\\") + 1, view3d.path.length);
                view3d.mainText = $("maintext", this).text() || mainText;
                view3d.objectId = $("referencepath", this).text();
                view3d.id = view3d.mainText + view3d.objectId;
                view3d.systemId = systemId;
                //Set Tooltip
                var toolTips = [],
                        name = $("tooltip", this).attr("name"),
                        value = $("tooltip", this).attr("value"),
                        tooltip = {
                            getName: function () {
                                return name;
                            },
                            getValue: function () {
                                return value;
                            }
                        }
                toolTips.push(tooltip);
                view3d.getToolTips = function () {
                    return toolTips;
                }
                if (view3d.type === mentor.publisher.contentType.RA_3D) {
                    if (!Utils.is_msie()) {//RA_3D not supported in IE11
                        //Don't show link if view is visible
                        if (!(p.rapidAuthorCatalogPanel // instantiated
                                && p.rapidAuthorCatalogPanel.isVisible() //visible
                                && view3d.mainText.startsWith(p.rapidAuthorCatalogPanel.getMainText()))) { //with same name
                            view3ds.push(view3d);
                        }
                    }
                }
                else {
                    view3ds.push(view3d);
                }

                if (view3d.objectId) {
                    if (view3d.type == mentor.publisher.contentType.JT_3D) {
                        jtPartIds.push(view3d.objectId);
                    }
                    else if (view3d.type === mentor.publisher.contentType.RA_3D) {
                        raItemIds.push(view3d.objectId);
                    }
                    else {
                        window.crossHighlightHandler.zoomObjectIn3DXML(view3d.objectId, view3d.type);
                    }
                }
            });
            if (jtPartIds.length > 0) {
                window.crossHighlightHandler.crossHighlighJTViews(jtPartIds);
            }

            if (raItemIds.length > 0) {
                window.crossHighlightHandler.crossHighlightInRapidAuthorViews(raItemIds, document, false);
            }

            return {
                listItems: view3ds,
                title: mentor.publisher.languageTranslator.localize('LocationViewTitle')
            };
        }
        ,

        getFaceviews: function () {
            var faceviews = [];
            $("faceview", objectDataDOM).each(function () {
                p.faceviewParser().parse(faceviews, $(this));
            });
            return {
                listItems: faceviews,
                title: mentor.publisher.languageTranslator.localize(splitPanelView.getConnectorViewTitle())
            };
        },

        getSignal: function () {
            return $("signal", objectDataDOM).text();
        }
        ,

        getGlobalSignal: function () {
            return $("globalsignal", objectDataDOM).text();
        }
        ,

        getSignalTraceFiles: function () {
            var getSignalTraceFile, getFullInstanceFile, getGroundAndPowerSignal, getGlobalSignalFile, gsts = [],
                    gst = {}, signalTraceFile, fullInstanceFile, getText, getStopAtGroundSignal, signalStopAtGround,
                    getCustomFaultSignal, customFaultSignal;

            getSignalTraceFile = function () {
                return getText("connectivityxml");
            };
            getFullInstanceFile = function () {
                return getText("fullinstancexml");
            };
            getGroundAndPowerSignal = function () {
                return getText("groundAndPowerSignal");
            };
            getGlobalSignalFile = function () {
                return getText("globalsignal");
            };

            getStopAtGroundSignal = function () {
                return getText("signalStopAtGround");
            };

            getCustomFaultSignal = function () {
                return getText("customFaultSignal");
            };

            getText = function (tag) {
                var dom = $(tag, objectDataDOM), fileName;
                if (dom && dom.length > 0) {
                    fileName = $(dom[0]).text();
                }
                return fileName;
            };

            if (getSignalTraceFile()) {
                //RenderConnectivityHandler.updateRenderer(getSignalTraceFile() + ".zip", 'Signal Path');
                signalTraceFile = getSignalTraceFile() + ".zip";
            }
            if (getFullInstanceFile()) {
                //RenderConnectivityHandler.updateRenderer(getFullInstanceFile() + ".zip", 'Full Instance');
                fullInstanceFile = getFullInstanceFile() + ".zip";
            }
            if (getStopAtGroundSignal()) {
                signalStopAtGround = getStopAtGroundSignal() + ".zip";
            }

            if (getCustomFaultSignal()) {
                customFaultSignal = getCustomFaultSignal() + ".zip";
            }

            if (getGroundAndPowerSignal()) {
                gst.mainText = mentor.publisher.languageTranslator.localize('groundAndPowerSignalTitle');
                gst.id = getGroundAndPowerSignal() + ".zip";
                gst.showPopoutButton = true;
                gsts.push(gst);
            }

            //todo change the texts to resource driven texts
            return {
                listItems: gsts,
                title: mentor.publisher.languageTranslator.localize('groundAndPowerSignalTitle'),
                signalTraceFile: signalTraceFile,
                fullInstanceFile: fullInstanceFile,
                signalStopAtGround: signalStopAtGround,
                customFaultSignal: customFaultSignal
            };

        }
        ,

        get2dLocationViews: function () {
            var name = this.getName(), locationViews = [], length, index, filteredLocationViews = [], currentView,
                    currentOpenedViewName;
            locationViews = p.locationViews.locationViewByName(name);

            length = locationViews.path && locationViews.path.length;
            for (index = 0; index < length; index = index + 1) {
                locationViews.path[index].objectId = objectUid;
                //need not show the current opened 2d view in the popover so filter.
                currentView = mentor.publisher.selectedSystem.get("locationView") ||
                        mentor.publisher.selectedSystem.get("customContent");
                if (currentView) {
                    //if the current opened view and this view are same, then do not add to the filtered views
                    currentOpenedViewName = currentView.mainText || currentView.get('mainText');
                    if (!(currentOpenedViewName == locationViews.path[index].mainText)) {
                        filteredLocationViews.push(locationViews.path[index]);
                    }
                }
                else {
                    //if the current view does not exist which means current opened artifact is not 2d view
                    filteredLocationViews.push(locationViews.path[index]);
                }
            }

            return {
                listItems: filteredLocationViews,
                title: "TwoDLocationViewTitle"
            };
        }
        ,

        getType: function () {
            return $("attributes", objectDataDOM).attr("simpleName") || $("object", objectDataDOM).attr("simpleName");
        },

        getAttr: function (attributeName) {
            if (!attributeName) {
                return "";
            }
            if (attributeName === 'type') {
                return this.getType();
            }
            var that = this;
            var values = this.getAttributes().listItems.filter(function (attribute) {
                return attribute && attribute.name.toLowerCase().trim() === attributeName.toLowerCase().trim();
            }).map(function (attribute) {
                return attribute.value;
            });
            return (values.length > 0 && values[0]) || "";
        },

        createListGroups: function () {
            var listGroups = [], customObjecDataArray, length, index;
            listGroups.Attributes = this.getAttributes();
            listGroups.Links = this.getCrossReferences();
            listGroups.LocationView = this.get3DViews();
            /*listGroups.SignalTrace = this.getSignalTraceFiles();*/
            listGroups.TwodViews = this.get2dLocationViews();
            listGroups.ShowGroundAndPowerSignal = this.getSignalTraceFiles();
            //todo test custom data
            listGroups.CustomData = this.getCustomData();
            listGroups.FaceViews = this.getFaceviews();
            return listGroups;
        },

        createXrefsFilter: createXrefsFilter,
    }
            ;
};

mentor.publisher.shortenedUIDMap = (function (p) {
    "use strict";
    var loadShortenedUIDMap;

    loadShortenedUIDMap = function (uidMapPath) {

        var xmlDOM, uidNodes, i, len, uidMap = [];
        xmlDOM = p.xmlLoader.loadXMLByAjax(uidMapPath, false, true);
        if (xmlDOM.data) {
            uidNodes = $("uid", xmlDOM.data);
            len = uidNodes.length;
            for (i = 0; i < len; i = i + 1) {
                uidMap[$(uidNodes[i]).attr('ouid')] = $(uidNodes[i]).attr('suid');
            }
        }
        else {
            xmlDOM.data = null;
        }
        return uidMap;

    };

    return {
        getShortenedObjectUID: function (objectId, systemId, projectId) {
            var uidMap, objectUIDMapPath = p.pathResolver.getObjectUIDMapPath(systemId,
                    projectId), objectUIDTokens, hostId, shortenedHostId;
            uidMap = loadShortenedUIDMap(objectUIDMapPath);
            if (uidMap) {
                objectUIDTokens = objectId.split('-');
                hostId = objectUIDTokens[2];
                shortenedHostId = uidMap[hostId];
                if (shortenedHostId) {
                    //reverse engineer the object Id from the host id map
                    objectId = objectUIDTokens[0].replace("UID", "") + objectUIDTokens[1] + shortenedHostId;
                }
            }
            return objectId;
        },
        getShortenedDesignUID: function (systemId, projectId) {
            var uidMap, designUIDMapPath = p.pathResolver.getDesignUIDMapPath(projectId);
            uidMap = loadShortenedUIDMap(designUIDMapPath);
            if (uidMap) {
                systemId = uidMap[systemId] || systemId;
            }
            return systemId;
        }
    };
}(mentor.publisher));

mentor.publisher.uidBuckets = (function (p) {
    "use strict";

    return {
        getUIDBucket: function (objectId) {
            var objectUIDTokens, timeStamp, shortenedHostId, bucketSize = 100;
            if (!objectId) {
                return objectId;
            }
            objectUIDTokens = objectId.split('-');
            timeStamp = objectUIDTokens[1];
            if (timeStamp) {
                //hexa string to integer
                timeStamp = parseInt(timeStamp, 16);
                //timestamp modulus bucket size currently hard coded to 100
                return (timeStamp % bucketSize);
            }
            return "0";
        }
    };
}(mentor.publisher));

mentor.publisher.objectDataLoader = (function (p) {
    "use strict";
    var dataLoader = xmlDataLoader(objectFactoryImpl()), parseXMLToJSObject;

    parseXMLToJSObject = function (element) {
        var object = element;
    };

    return {
        project: "",
        setProject: function (proj) {
            this.project = proj;
        },
        mostRecentObjectData: {},

        loadObjectDataFrom: function (objectPath, systemId, shortenedUID, projectId, objectUid) {
            if (this.mostRecentObjectData.path === objectPath) {
                return this.mostRecentObjectData.data;
            }
            else {
                var objectDataResponse = mentor.publisher.xmlLoader.loadXMLByAjax(objectPath, false, false);
                if (objectDataResponse.data) {
                    if (p.referenceObjectParser(objectDataResponse.data, systemId, shortenedUID).shouldRedirect()) {

                        if (p.referenceObjectParser(objectDataResponse.data, systemId,
                                shortenedUID).hasMultipleUIDRefs()) {
                            return p.objectDataLoader.loadCrossRefsFromMultipleObjects(systemId,
                                    p.referenceObjectParser(objectDataResponse.data, systemId,
                                            shortenedUID).getReferenceIds(),
                                    projectId);
                        }
                        else {
                            return p.objectDataLoader.load(systemId,
                                    p.referenceObjectParser(objectDataResponse.data, systemId,
                                            shortenedUID).getReferenceIds(),
                                    projectId);
                        }
                    }
                    this.mostRecentObjectData.path = objectPath;
                    this.mostRecentObjectData.data = this.objectParser(objectDataResponse.data, systemId, objectUid);
                }
                else {
                    this.mostRecentObjectData.path = objectPath;
                    this.mostRecentObjectData.data = {};
                }
            }
            return this.mostRecentObjectData.data;

        },

        loadObjectDataReferences: function (objectPath, systemId, shortenedUID, projectId, objectUid) {
            var objectDataResponse = mentor.publisher.xmlLoader.loadXMLByAjax(objectPath, false, false);
            if (objectDataResponse.data) {
                var referenceObjectParser = p.referenceObjectParser(objectDataResponse.data, systemId, shortenedUID);
                if (referenceObjectParser.shouldRedirect()) {
                    return referenceObjectParser.getReferenceIds();
                }
                else {
                    return [objectUid];
                }
            }
        },
        areUIDsStoredInBuckets: function (projectId) {
            var path = p.pathResolver.getVersionFilePath(projectId), data;
            data = p.xmlLoader.loadFile(path, false, true, "json") || {data: {}};
            if (data.data) {
                return data.data.version;
            }
        },

        getObjectBucket: function (systemId, objectUid, projectId) {
            if (this.areUIDsStoredInBuckets(projectId)) {
                return p.uidBuckets.getUIDBucket(objectUid);
            }
            else {
                return p.shortenedUIDMap.getShortenedDesignUID(systemId, projectId)
            }
        },
        getObjectPath: function (systemId, objectUid, projectId, uidBucket) {
            if (this.areUIDsStoredInBuckets(projectId)) {

                return mentor.publisher.pathResolver.getObjectPath(systemId, objectUid, projectId, uidBucket)
            }
            else {
                return projectId + "/" + uidBucket + "/O" + "/" + objectUid + ".xml"
            }
        },

        mergeCrossRefsFromObjs: function (designObjects) {
            if (designObjects.length > 0) {

                var index, designObject, xrefs = [], listItems = [];
                for (index in designObjects) {
                    designObject = designObjects[index];
                    if (designObject.getCrossReferences && designObject.getCrossReferences().xrefs) {
                        xrefs = xrefs.concat(designObject.getCrossReferences().xrefs);
                        listItems = listItems.concat(designObject.getCrossReferences().listItems);
                    }
                }
                return {
                    getName: function () {
                        return designObjects[0].getName();
                    },

                    getType: function () {
                        return "";
                    },

                    getCrossReferences: function () {
                        return {
                            listItems: listItems,
                            title: mentor.publisher.languageTranslator.localize('XRefTitle'),
                            textFilterable: true,
                            xrefs: xrefs
                        };
                    }
                }
            }
            return {};

        },

        loadCrossRefsFromMultipleObjects: function (systemId, objectUids, projectId) {
            var uid, designObjects = [], designObject;
            for (uid in objectUids) {
                designObject = this.load(systemId, objectUids[uid], projectId);
                if (designObject.getName()) {
                    designObjects.push(designObject);
                }
            }
            return this.mergeCrossRefsFromObjs(designObjects);
        },
        load: function (systemId, objectUid, projectId) {
            var objectPath, uidBucket, shortenedUID;
            uidBucket = this.getObjectBucket(systemId, objectUid, projectId);
            shortenedUID = p.shortenedUIDMap.getShortenedObjectUID(objectUid, systemId, projectId);
            objectPath = this.getObjectPath(systemId, shortenedUID, projectId, uidBucket);
            return this.loadObjectDataFrom(objectPath, systemId, shortenedUID, projectId, objectUid);
        },

        loadRefernceIdsIfAny: function (systemId, objectUid, projectId) {
            var objectPath, uidBucket, shortenedUID;
            uidBucket = this.getObjectBucket(systemId, objectUid, projectId);
            shortenedUID = p.shortenedUIDMap.getShortenedObjectUID(objectUid, systemId, projectId);
            objectPath = this.getObjectPath(systemId, shortenedUID, projectId, uidBucket);
            return this.loadObjectDataReferences(objectPath, systemId, shortenedUID, projectId, objectUid);
        },
        loadObjects: function (type, loadAllObjects) {
            var filePath = p.pathResolver.getGlobalObjectFilePath(type, p.project.getId()), xmlData;
            xmlData = p.xmlLoader.loadXMLByAjax(filePath, false, false);
            if (xmlData.data) {
                return dataLoader.parseXMLForTag("object", xmlData.data, xmlData.data, loadAllObjects);
            }

        },
        getDiagram: function (objectInfo) {
            var systemId, objectId, objectData, diagrams;
            objectInfo = objectInfo || {};
            systemId = objectInfo.systemId;
            objectId = objectInfo.objectId;
            objectData = mentor.publisher.project.loadObjectData(systemId, objectId) || {};
            if (objectData.getCrossReferences) {
                diagrams = objectData.getCrossReferences() && objectData.getCrossReferences().xrefs;
            }
            if (diagrams && diagrams.length) {
                return diagrams[0];
            }
            if (objectData.getHarnessLayouts) {
                diagrams = objectData.getHarnessLayouts() && objectData.getHarnessLayouts();
            }
            if (diagrams && diagrams.length > 0) {
                var content = diagrams[0] || {};
                content.type = p.contentType.HARNESS_LAYOUT_DIAGRAM;
                return content;
            }

        },
        getObjectFileName: function (type) {
            return type + "s"
        },
        findXrefsByObjectId: function (systemObjects) {

            systemObjects = systemObjects || [];
            var xrefs = [], that = this;
            systemObjects.map(function (systemObject) {
                var xrefsForObject = [], project = that.project || p.project;
                if (systemObject.systemId && systemObject.objectId) {
                    var objectData = project.loadObjectData(systemObject.systemId, systemObject.objectId);
                    if (objectData.getCrossReferences) {
                        xrefsForObject = objectData.getCrossReferences();
                        if (xrefsForObject.listItems) {
                            xrefs = xrefs.concat(xrefsForObject.listItems);
                        }
                    }

                }
                return xrefsForObject;
            });
            return xrefs;

        },
        findObjectInGlobalObjectFile: function (id, xmlData, name) {
            var objects = [];
            if (id) {
                $("object[objectId='" + id + "']", xmlData.data).each(function () {
                    objects.push({systemId: $(this).attr("systemUid"), objectId: $(this).attr("objectId")});
                });

            }
            else {
                $("object[name='" + name + "']", xmlData.data).each(function () {
                    objects.push({systemId: $(this).attr("systemUid"), objectId: $(this).attr("objectId")});
                });

            }
            return objects;
        }, loadObjectsFromGlobalFile: function (type) {
            var filePath = p.pathResolver.getGlobalObjectFilePath(this.getObjectFileName(type),
                    p.project.getId()), xmlData;
            xmlData = p.xmlLoader.loadXMLByAjax(filePath, false, false);
            return xmlData;
        }, findObjectByName: function (name, type, id) {
            var xmlData = this.loadObjectsFromGlobalFile(type);
            var objects = [];
            if (xmlData.data) {
                objects = this.findObjectInGlobalObjectFile(id, xmlData, name);
                return this.getDiagram(objects.length > 0 && objects[0]);
            }
        },
        objectParser: mentor.publisher.objectDataParser
    };
}(mentor.publisher));

mentor.publisher.customGeneratorDataLoader = (function (p) {
    "use strict";
    var factory = objectFactoryImpl(), listItems = [];

    return {
        load: function (type, projectId, systemId, diagramUid) {
            var customGeneratorFilePath = mentor.publisher.pathResolver.getCustomGeneratorFilePath(type, projectId,
                    systemId, diagramUid), customGeneratorDataResponse;
            customGeneratorDataResponse =
                    mentor.publisher.xmlLoader.loadXMLByAjax(customGeneratorFilePath, false, false);
            if (customGeneratorDataResponse.data) {
                listItems = factory.createObjects('objectDataEntry', customGeneratorDataResponse.data) || [];

                return _.filter(listItems, function (item) {
                    return item.path && !item.path.endsWith(".xml");
                });
            }
            else {
                return [];
            }
        }
    };
}(mentor.publisher));

mentor.publisher.xmlLoader = (function () {
    "use strict";
    var cache = {},
            convertData,
            loadURL;

    convertData = function (data) {
        var xml = null;
        if (typeof data === "string") {
            xml = new ActiveXObject("Microsoft.XMLDOM");
            xml.async = false;
            xml.loadXML(data);
        }
        else {
            xml = data;
        }
        return xml;
    };

    loadURL = function (url, async, cacheFile, type) {
        var result = {};
        type = type || ((Utils.is_msie()) ? "text" : "xml");
        cacheFile = typeof cacheFile === "undefined" ? true : cacheFile;

        if (typeof cache[url] === "undefined") {
            $.ajax({
                url: url,
                async: async,
                success: function (data, textStatus, XMLHttpRequest) {
                    result.data = data;
                    result.error = null;
                    result.ajaxStatus = textStatus;
                    result.xmlHttpObject = XMLHttpRequest;
                },
                error: function (XMLHttpRequest, textStatus, errorThrown) {
                    result.data = null;
                    result.error = "[Error] " + errorThrown;
                    result.ajaxStatus = textStatus;
                    result.xmlHttpObject = XMLHttpRequest;
                },
                dataType: type
            });
            if (Utils.is_msie()) {
                result.data = convertData(result.data);
            }
            if (cacheFile) {
                cache[url] = result;
            }

        }
        else {
            result = cache[url];
        }

        return result;
    };

    return {

        callAPI: function (endPoint, params, async, cacheFile, type) {
            var encodedEndPoint,
                    paramStrings,
                    queryString;

            encodedEndPoint = endPoint;
            paramStrings = _.map(params || {}, function (value, key) {
                return key + "=" + encodeURIComponent(value);
            });
            queryString = paramStrings.join('&');

            var url = Utils.prepareFilePath(encodedEndPoint) + "&" + queryString;

            return loadURL(
                    url,
                    async,
                    cacheFile,
                    type
            );
        },
        loadFile: function (url, async, cacheFile, type) {
            return loadURL(Utils.prepareFilePath(url), async, cacheFile, type
            );
        },
        loadGlobalFile: function (url, async, cacheFile, type) {
            return loadURL(url, async, cacheFile, type);
        },
        loadInternalURLsFromConfig: function () {
            var urls = loadURL(
                    "internalURLs.json",
                    false,
                    true,
                    "json"
            );
            if (!urls.data) {
                return [];
            }
            return urls.data;
        },
        loadXMLByAjax: function (xmlUrl, async, cacheFile, type) {
            return this.loadFile(xmlUrl, async, cacheFile, type);
        },
        convertStringXMLToDOM: function (data) {
            return convertData(data);
        },
        isItaReport: function (path) {
            var htmlContent = this.loadXMLByAjax(path, false, false, "html"), isAReport = false;
            if (htmlContent && htmlContent.xmlHttpObject && htmlContent.xmlHttpObject.responseText &&
                    (htmlContent.xmlHttpObject.responseText.indexOf("<table class=\"reportTableStyle") >= 0 ||
                            htmlContent.xmlHttpObject.responseText.indexOf("<table class='reportTableStyle") >= 0)) {
                isAReport = true;
            }

            return isAReport;
        }
    };
}());

(function (p) {
    "use strict";
    var getCavityReportFileName, getSymbolFileName;
    getSymbolFileName = function (name) {
        return name + ".svg";
    };
    getCavityReportFileName = function (cavityTableName) {
        cavityTableName = cavityTableName || "";
        if (cavityTableName.indexOf(".html") >= 0) {
            return cavityTableName;
        }
        return cavityTableName + ".html";
    };
    p.faceviewParser = function () {
        return {
            parse: function (faceviews, fvDOMElement, systemId) {
                var faceview = {}, cavityTable = $(fvDOMElement).attr("cavityTable");
                faceview.mainText = $(fvDOMElement).attr("name") || "";
                faceview.name = $(fvDOMElement).attr("name") || "";
                faceview.id = faceview.mainText + cavityTable;
                faceview['faceviewId'] = $(fvDOMElement).attr("faceviewId");
                if (faceview['faceviewId']) {
                    faceview.id = faceview.faceviewId;
                }
                faceview.objectId = $(fvDOMElement).attr("id");
                faceview.symbol = getSymbolFileName(faceview.mainText);
                faceview.showPopoutButton = true;
                faceview.path = $(fvDOMElement).attr("path");
                faceview.separator = $(fvDOMElement).attr("separator");
                faceview['multiple-faceview-support'] = $(fvDOMElement).attr("multiple-faceview-support");
                faceview.view = $(fvDOMElement).attr("view");

                faceview['lib'] = $(fvDOMElement).attr("lib");
                if (faceview['multiple-faceview-support']) {
                    if (faceview['view']) {
                        faceview.mainText =
                                faceview.mainText + " " + $(fvDOMElement).attr("separator") + " " +
                                this.translator(faceview['view']);
                    }
                    else {
                        faceview['view'] = "noViewSpecified";
                    }
                }
                faceview.cavityTable = getCavityReportFileName(cavityTable);
                faceview.systemId = systemId;
                faceviews.push(faceview);
            },
            translator: function (text) {
                return Utils.translate(text);
            }
        };
    };

}(mentor.publisher));

var diagramAsSystemsObjectFactoryImpl;



