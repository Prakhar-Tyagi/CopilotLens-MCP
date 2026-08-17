/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global packageModel, mentor, $, createObjectByCopyingProperties, extend, createOptionExpressions, jQuery */
var getIdToHighlight = function (object)
{
    "use strict";
    return object.id;
};
mentor.publisher.locationViews = (function (p)
{
    "use strict";
    var parseLocationViews,
            locationViewsArray = [], locationViewMap = {}, getLocationViewName, twoSVGs = {}, store2DSVGs,
            objectUidToLocationViewMap = {}, updateObjectUidToLocationViewMap, loadThumbnails, thumbnails = {},
            getFilteredViewByName, addToLocationViewMap, createToolTip;

    getLocationViewName = function (locationViewPath)
    {
        var startIndexOfName;
        locationViewPath = locationViewPath || "";
        if (locationViewPath.indexOf('\\') == -1) {
            locationViewPath = locationViewPath.replace(/\//g, '\\');
        }
        startIndexOfName = locationViewPath.lastIndexOf("\\");
        if (startIndexOfName >= 0) {
            return (locationViewPath.substring(startIndexOfName + 1, locationViewPath.length)).replace(/.svg/i, "");
        }
        else {
            return "";
        }

    };

    updateObjectUidToLocationViewMap = function (uid, locationViewName)
    {
        updateObjectUidToLocationViewMap[uid] = locationViewName;
    };

    var getLocationViewObj = function (name, svgPath)
    {
        return extend({id: name, name: name, path: svgPath}, {
            getURL: function ()
            {
                return svgPath;
            },
            getName: function ()
            {
                return name;
            },
            getType: function ()
            {
                return "svg";
            },
            getId: function ()
            {
                return name;
            },
            mainText: name,
            id: name,
            showPopoutButton: true,
            optionExpression: [],
            thumbNailPath: thumbnails[name],
            type: mentor.publisher.contentType.LOCATION_VIEWS
        });
    }

    store2DSVGs = function (svgPath, optionExpressions)
    {
        var name = getLocationViewName(svgPath), locView, newOptionExp;
        newOptionExp = optionExpressions;
        if (!twoSVGs[name]) {
            locView = getLocationViewObj(name, svgPath);
            locView.idToHighlight = locView.getId();
            locationViewsArray.push(locView);
            twoSVGs[name] = locView;
            twoSVGs[name].optionExpression.push(newOptionExp);
            return locView;
        }
        else {
            twoSVGs[name].optionExpression.push(newOptionExp);

            return twoSVGs[name];
        }
    };
    loadThumbnails = function (locationViewDOM)
    {
        var nodes = $("ThumbNailImageDetail", locationViewDOM) ||
                [], length = nodes.length, index, svgName;
        thumbnails = {};
        for (index = 0; index < length; index = index + 1) {
            svgName = $(nodes[index]).attr("svgName");
            if (svgName) {
                thumbnails[svgName] = Utils.prepareFilePath($(nodes[index]).attr("thumbNailName"));
            }
        }

    };

    createToolTip = function (toolTips, objectType)
    {
        return function ()
        {
            var locToolTips = [].concat(toolTips);
            locToolTips.push({

                getName: function ()
                {
                    return mentor.publisher.languageTranslator.localize("type") || "type";
                },

                getValue: function ()
                {
                    var lastIndexOfDot = objectType.indexOf(".X");
                    return mentor.publisher.languageTranslator.localize(objectType.substring(lastIndexOfDot +
                                    2)) || objectType.substring(lastIndexOfDot + 2);
                }

            });
            return locToolTips;
        };
    };

    parseLocationViews = function (locationViewDOM)
    {

        var toolTips, systemObj, locationViewsDOM = $("LocationViews>LocationView",
                        locationViewDOM) ||
                        [], i, locationView, locationViewsInfo, length, systems = [], system, systemNodes, systemsLength, j,
                systemNode, name, svgPaths, svgPathLength, k, path, opExp = '', svgPath, firstIndex, lastindex,
                objectType;

        if (locationViewDOM) {

            loadThumbnails(locationViewDOM);
            length = locationViewsDOM.length;
            for (i = 0; i < length; i = i + 1) {
                locationView = locationViewsDOM[i];
                locationViewsInfo = {};
                locationViewsInfo.name = $(locationView).attr("name") || "";
                objectType = $(locationView).attr("type") || "";
                locationViewsInfo.path = [];

                systemNodes = $("systemPath", locationView) || [];
                systemsLength = systemNodes.length;
                locationViewsInfo.systems = [];
                locationViewsInfo.optionExpression = [];
                for (j = 0; j < systemsLength; j = j + 1) {
                    systemNode = systemNodes[j];
                    system = {};
                    system.systemId = $(systemNode).attr("id");
                    systemObj = mentor.publisher.project.getObjectById(system.systemId);
                    if (systemObj) {

                        system.folder = systemObj.subText;
                        if (systemObj.getToolTips) {

                            toolTips = systemObj.getToolTips() || [];
                        }
                        system.getToolTips = (createToolTip(toolTips, objectType));
                        system.path = $(systemNode).text() || "";
                        system.designOptionExpression = $(systemNode).attr("designOptionExpression");
                        system.diagramName = $(systemNode).attr("diagramName");
                        system.diagramId = $(systemNode).attr("diagramId");
                        if (!system.diagramId) {
                            //try legacy behaviour
                            svgPath = system.path || "";
                            firstIndex = svgPath.lastIndexOf("\\");
                            lastindex = svgPath.lastIndexOf(".svg");
                            try {
                                system.diagramId = svgPath.substr(firstIndex + 1, lastindex).replace(".svg", "");
                            }
                            catch (e) {
                            }
                        }
                        system.objectId = $(systemNode).attr("objectConnId");
                        system.connUID = $(systemNode).attr("objectConnId");
                        system.sharedUID = $(systemNode).attr("sharedUID");
                        system.objectOptionExpression = $(systemNode).attr("objectOptionxpression");
                        opExp = $(systemNode).attr("optionExpression");
                        system.objectSchemId = $(systemNode).attr("objectSchemId");
                        system.optionExpression =
                                createOptionExpressions(system.objectOptionExpression, system.designOptionExpression,
                                        "&&");
                        system.shortdescription = $(systemNode).attr("shortdescription");
                        system.id = $(systemNode).attr("id");

                        system.name = $(systemNode).attr("name") + ":" + system.diagramName;
                        system.mainText = system.name;
                        system.subText = system.folder;
                        system.showPopoutButton = true;
                        system.idToHighlight = getIdToHighlight(system);
                        locationViewsInfo.systems.push(system);
                        locationViewsInfo.optionExpression.push(system.optionExpression);
                        updateObjectUidToLocationViewMap(system.objectConnId, locationViewsInfo.name);
                        objectUidToLocationViewMap[system.objectId] = locationViewsInfo.name;
                    }
                }

                svgPaths = $("path", locationView);
                svgPathLength = svgPaths.length;
                for (k = 0; k < svgPathLength; k = k + 1) {
                    path = $(svgPaths[k]).text() || "";
                    locationViewsInfo.path.push(store2DSVGs(path, opExp));
                }

                //locationViewMap[locationViewsInfo.name.trim()] = locationViewsInfo;
                addToLocationViewMap(locationViewMap, locationViewsInfo);
            }
        }

        var pdflocationViewsExists = false,
                pdflocationviews = p.xmlLoader.loadFile(p.project.getId() + "/pdfLocationViews/pdfLocationViews.json",
                        false, false,
                        "json");
        if (pdflocationviews.data) {
            var pdfViews = _.forEach(pdflocationviews.data, function (pdfloc)
            {
                var locView = extend(pdfloc,
                        getLocationViewObj(pdfloc.documentSets[0].title, pdfloc.documentSets[0].documents[0].path));
                var effSetter = require("filehandlers/effectivitySetter");
                var thumbNailPath = pdfloc.documentSets[0].documents[0].thumbNailPath;
                if (thumbNailPath) {
                    locView.thumbNailPath = Utils.prepareFilePath(thumbNailPath);
                }
                locationViewsArray.push(locView);
                pdflocationViewsExists = true;
            })
        }
        mentor.publisher.config["pdf-locationview-exists"] = pdflocationViewsExists;

    };

    addToLocationViewMap = function (locationViewMap, locationViewsInfo)
    {
        var objectName = locationViewsInfo.name.trim(), sys;
        if (locationViewMap[locationViewsInfo.name.trim()]) {
            /**
             merge location view enteries with same name
             */
            locationViewMap[locationViewsInfo.name.trim()].systems =
                    locationViewMap[locationViewsInfo.name.trim()].systems.concat(locationViewsInfo.systems)
        }
        else {
            locationViewMap[locationViewsInfo.name.trim()] = locationViewsInfo;
        }

    };

    getFilteredViewByName = function (objectName)
    {
        var views = [], clone, filteredViews, view;
        views.push(locationViewMap[objectName] || {});
        filteredViews = p.filter.applyFilter(views);
        view = filteredViews[0] || {};
        clone = jQuery.extend(true, {}, view);
        clone.systems = p.configurationsBasedOtherFilter.applyFilter(view.systems);
        return clone;
    };

    return {
        load: function ()
        {
            var url = p.pathResolver.getLocationViewsFilePath(p.project.getId()), locationViewDOM;
            locationViewsArray = [];
            locationViewMap = {};
            twoSVGs = {};
            objectUidToLocationViewMap = {};
            locationViewDOM = p.xmlLoader.loadXMLByAjax(url, false, false);
            // if (locationViewDOM.data) {
            parseLocationViews(locationViewDOM.data);
            //m}

        },
        createToolTip: function (toolTips, objectType)
        {
            return createToolTip(toolTips, objectType);
        },
        getLocationViews: function ()
        {
            return _.sortBy((p.filter.applyFilter(locationViewsArray) || []), 'mainText');
            ;
        },
        /*
         this method expects the name of the view as input, it returns location view with that name
         */
        getLocationViewByViewName: function (name)
        {
            var content;
            var contents = p.locationViews.getLocationViews();
            if (contents) {
                for (var k = 0; k < contents.length; k++) {
                    if (contents[k].name === name) {
                        content = contents[k];
                        break;
                    }
                }
            }
            return content;
        },
        /*
         this method expects the name of the object as input, it returns location view which are associated with that object
         as output
         */
        locationViewByName: function (objectName)
        {
            var svgLocationViews = getFilteredViewByName(objectName) || {
                        path: []
                    };
            svgLocationViews.path = svgLocationViews.path || [];
            var documentObject = mentor.publisher.nameToUIDMap.getUIDsFor(objectName);
            if (documentObject && documentObject.length > 0 && documentObject[0].relatedDocuments &&
                    documentObject[0].relatedDocuments.length > 0) {

                var pdflocationViews = documentObject[0].relatedDocuments[0].documents;
                var pdflocs = {path: []};
                if (pdflocationViews && pdflocationViews.length > 0) {
                    pdflocationViews = _.map(pdflocationViews, function (pdf)
                    {
                        pdf = extend(pdf, {
                            id: pdf.mainText
                        });
                        svgLocationViews.path.push(pdf);
                    });
                }
                svgLocationViews.path.sort(function (x, y)
                {
                    return x.mainText.localeCompare(y.mainText);
                });
            }
            return svgLocationViews;
        },
        doesObjectHasLinksWithin2dView: function (objectName)
        {
            return locationViewMap[objectName] !== "undefined";
        },
        getLocationViewByObjectId: function (objectUID)
        {
            return objectUidToLocationViewMap[objectUID] || "";
        },
        getLocationViewName: getLocationViewName
    };
}(mentor.publisher));
