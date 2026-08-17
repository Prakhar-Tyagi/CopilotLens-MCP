/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global extend, $, mentor, Constants, createId, Msg, createOptionExpressions*/
var objectFactoryImpl = function ()
{
    "use strict";
    var p = mentor.publisher, createReport, createSignalObject,
            getAttributeFromDomElement, createToolTip, createDiagram, reArrangeReportsToShowWireListReportFirst,
            createAttributeObj, createFaultCodes, sequenceId = 0, that, createCustomizedExternalData, createObjectUsingElementType;

    that = {
        create: function (elementType, domElement)
        {
            return this.createObject(elementType, domElement);
        },
        createObjectsByType: function (elementType, domElement, loadAllObjects)
        {
            return this.createObjects(elementType, domElement, loadAllObjects);
        }
    };

    that.createObject = function (elementType, domElement)
    {
        if (elementType === "system") {
            return this.createSystem(domElement);
        }
        else if (elementType === "report") {
            return createReport(domElement, mentor.publisher.contentType.SYSTEM_REPORT);
        }
        else if (elementType === "tooltip") {
            return createToolTip(domElement);
        }
        else if (elementType === "faultcode") {
            return createFaultCodes(domElement);
        }
        else if (elementType === "harness") {
            return createReport(domElement, mentor.publisher.contentType.CUSTOM_VIEW);
        }
        else if (elementType === "globalreports") {
            var reportItem = createReport(domElement, mentor.publisher.contentType.CUSTOM_VIEW);
            reportItem.mainText = '{' + reportItem.mainText + '}';
            return reportItem;
        }
        else if (elementType === "introduction-page") {
            return createReport(domElement, mentor.publisher.contentType.CUSTOM_VIEW);
        }
        else if (elementType === "diagram") {
            return createDiagram(domElement);
        }
        else if (elementType === "signal") {
            return createSignalObject(domElement);
        }
        else if (elementType === "objectDataEntry") {
            return createCustomizedExternalData(domElement);
        }
        else {
            return createObjectUsingElementType(domElement, elementCount);
        }

    };

    createObjectUsingElementType = function (domElement, elementCount)
    {
        var element = extend(that.createAttributeObj(domElement, "customObjects"), {});
        element.designs = $(element).attr("design");
        element.showPopoutButton = true;
        return element;
    };

    createCustomizedExternalData = function (domElement)
    {
        var customizedExternalData = extend(that.createAttributeObj(domElement, "Objects"),
                {}), type = that.getAttributeFromDomElement("type", domElement);
        customizedExternalData.idForSearch =
                that.getAttributeFromDomElement("id", domElement) || customizedExternalData.nameAttr;
        customizedExternalData.mainText = customizedExternalData.mainText.replace(".html", "");
        customizedExternalData.isHTTPLink = isURLHTTPLink(customizedExternalData.path);
        customizedExternalData.showPopoutButton = !customizedExternalData.isHTTPLink;
        if (type) {
            customizedExternalData.type = type;
        }
        return customizedExternalData;
    };

    createSignalObject = function (signalDOMNode)
    {
        var id = $(signalDOMNode).attr('name'), name = $(signalDOMNode).attr('name'),
                diagramids = $(signalDOMNode).attr('diagramUids'), opExpression = $(signalDOMNode).attr(
                'optionExpression'), signal;
        signal = {
            getName: function ()
            {
                return name;
            },
            getId: function ()
            {
                return id;
            },
            getDiagramIds: function ()
            {
                return diagramids;
            },
            getOptionExpression: function ()
            {
                return opExpression;
            },
            mainText: name,
            id: id,
            signal: true,
            color: mentor.publisher.colors[mentor.publisher.constants.orangeColorMsg],
            signalName: name
        };
        signal.contextualData = [
            {name: "signal", value: name}
        ];
        return signal;
    };

    that.createObjects = function (elementType, domElement, loadAllObjects)
    {
        var elements = [], elems = $(elementType,
                domElement), totalLengh = elems.length;

        $(elems).each(function ()
        {
            var objects = that.createObject(elementType, $(this)) || {}, length, index;
            if (_.isArray(objects)) {
                length = objects.length;
                for (index = 0; index < length; index = index + 1) {
                    elements.push(objects[index]);
                }
            }
            else {
                objects.totalObjects = totalLengh;
                elements.push(objects);
            }
        });
        return elements;
    };

    createReport = function (domReportElement, reportType)
    {
        return extend(that.createAttributeObj(domReportElement, "Reports"), {
            getURL: function ()
            {
                return that.getAttributeFromDomElement("path", domReportElement);
            },
            path: that.getAttributeFromDomElement("path", domReportElement),
            systemId: that.getAttributeFromDomElement("id", $(domReportElement).parent()),
            getSystemId: function ()
            {
                return that.getAttributeFromDomElement("id", $(domReportElement).parent());
            },
            id: that.getAttributeFromDomElement("name", domReportElement),

            designs: that.getAttributeFromDomElement("design", domReportElement),
            type: reportType,
            category: mentor.publisher.documentCategory.INFORMATION
        });
    };

    createDiagram = function (diagramNode)
    {
        var schematic = $("schematic", diagramNode), path, attr = that.createAttributeObj(diagramNode, "Diagrams");
        return extend(attr, {
            getPath: function ()
            {
                return that.getAttributeFromDomElement("path", schematic);
            },
            getType: function ()
            {
                return that.getAttributeFromDomElement("type", schematic);
            },
            getSystemId: function ()
            {
                return that.getAttributeFromDomElement("id", $(schematic).parent().parent());
            },
            path: that.getAttributeFromDomElement("path", schematic),
            systemId: that.getAttributeFromDomElement("id", $(diagramNode).parent()),
            type: that.getAttributeFromDomElement("type", schematic),
            diagramId: attr.getId()
        });
    };

    createFaultCodes = function (domElement)
    {
        var optionExpression = [], designOptions, objOptions, currentOptionExp;
        $("objectData>data", domElement).each(function ()
        {
            designOptions = $(this).attr('designOptions') || "";
            objOptions = $(this).attr('optionExpression') || "";
            currentOptionExp = createOptionExpressions(designOptions, objOptions, "&&");
            if (!currentOptionExp) {
                optionExpression = "";
                return false;
            }
            optionExpression.push(currentOptionExp);
        });
        return extend(that.createAttributeObj(domElement, "Fault Codes"), {
            getDisplayName: function ()
            {
                return this.getName();
            },
            type: mentor.publisher.contentType.FAULT_CODE,
            getOptionExpression: function ()
            {
                return optionExpression;
            }
        });
    };

    createToolTip = function (domToolTipElement)
    {
        var name = that.getAttributeFromDomElement("name", domToolTipElement), value;
        value = that.getAttributeFromDomElement("value", domToolTipElement);
        return {
            getName: function ()
            {
                return name;
            },
            getValue: function ()
            {
                return value;
            }
        };
    };

    that.getAttributeFromDomElement = function (attributeName, domElement)
    {
        return $(domElement).attr(attributeName);
    };

    that.createAttributeObj = function (domElement, headerTitle)
    {
        var name, subText, toolTip = that.createObjects("tooltip", domElement) ||
                [], showPopout = true, attr, path, link, actualName;
        name = that.getAttributeFromDomElement("name", domElement);
        actualName = name;
        name = toolTip.length > 0 ? toolTip[0].getValue() : name;
        subText = toolTip.length > 1 ? toolTip[1].getValue() : "";
        path = that.getAttributeFromDomElement("path", domElement);
        link = that.getAttributeFromDomElement("link", domElement);

        if ("Fault Codes" === headerTitle) {
            name = that.getAttributeFromDomElement("id", domElement);
            subText = that.getAttributeFromDomElement("name", domElement);
            showPopout = false;
        }
        attr = {
            getId: function ()
            {
                if (!this) {
                    return "";
                }

                if (!this.id) {
                    var id = that.getAttributeFromDomElement("id", domElement);
                    if (!id) {
                        var pathValue = this.path;
                        if (pathValue) {
                            id = pathValue.hashCode();
                        }
                        else {
                            id = createId(sequenceId);
                            sequenceId = id;
                        }
                    }
                    this.id = id;
                }
                return this.id;
            },
            nameAttr: actualName,
            getName: function ()
            {
                return name;
            },
            getShortDescription: function ()
            {
                return subText;
            },
            getRevision: function ()
            {
                return that.getAttributeFromDomElement("revision", domElement);
            },
            getDisplayName: function ()
            {
                var revision = this.getRevision();
                return this.getName() + (revision ? (":" + revision) : "");
            },
            getOptionExpression: function ()
            {
                return that.getAttributeFromDomElement("optionExpression", domElement);
            },
            getToolTips: function ()
            {
                return toolTip;
            },
            getHeader: function ()
            {
                return headerTitle;
            },
            getTagName: function ()
            {
                return headerTitle;
            },
            mainText: name,
            subText: subText,
            showPopoutButton: showPopout,
            path: path,
            link: link

        };

        attr.idToHighlight = attr.getId();
        return attr;

    };

    that.createSystem = function (domSystemElement)
    {
        var cachedObjects = {}, cache, getSystem;

        cache = function (items)
        {
            var index;
            for (index = 0; index < items.length; index = index + 1) {
                cachedObjects[items[index].getId()] = items[index];
            }
        };

        reArrangeReportsToShowWireListReportFirst = function (reports)
        {
            var index, reorderedReports = [], tempArray = [], index2;
            if (!reports || !reports.length || reports.length <= 0) {
                return reports;
            }
            for (index in reports) {
                if (reports.hasOwnProperty(index)) {
                    if (reports[index] && reports[index].mainText &&
                            reports[index].mainText.indexOf("Wire List") >= 0) {
                        reorderedReports.push(reports[index]);
                    }
                    else {
                        tempArray.push(reports[index]);
                    }
                }
            }

            for (index2 in tempArray) {
                if (tempArray.hasOwnProperty(index2)) {
                    reorderedReports.push(tempArray[index2]);
                }
            }
            return reorderedReports;
        };

        function getDiagrams(domSystemElement)
        {
            if($("schematic", domSystemElement).length > 0) {
                return that.createObjects("diagram", domSystemElement) || [];
            }
            return [];
        }

        getSystem = function ()
        {
            var diagrams = getDiagrams(domSystemElement), reports = that.createObjects("report",
                            domSystemElement) || [], firstDiaOrReport, systemObj;
            reports = reArrangeReportsToShowWireListReportFirst(reports);
            firstDiaOrReport = (diagrams.length > 0 && diagrams[0]) ||
                    (reports.length > 0 && reports[0]);
            var systemObj, toolTip, attribute = that.createAttributeObj(domSystemElement,
                    "Systems"), methods = extend(attribute, {
                getFolders: function ()
                {
                    return that.getAttributeFromDomElement("folders", domSystemElement);
                },
                getReports: function ()
                {
                    cache(reports);
                    return reports;
                },
                getDiagrams: function ()
                {

                    cache(diagrams);
                    return diagrams;
                },
                getFirstDiagram: function ()
                {
                    return firstDiaOrReport;
                },
                getTagName: function ()
                {
                    return mentor.publisher.constants.systemMsg.toLowerCase();
                },
                get: function (id)
                {
                    if (!cachedObjects.length || cachedObjects.length <= 0) {
                        this.getDiagrams();
                        this.getReports();
                    }
                    return cachedObjects[id];
                },
                getObjectById: function (id)
                {
                    return this.get(id);
                },
                systemId: attribute.getId(),
                firstDiagramOrReport: firstDiaOrReport && (firstDiaOrReport.path || firstDiaOrReport.diagramId),
                type: mentor.publisher.contentType.SYSTEM_SVG,
                idAttribute: attribute.getId()
            });
            systemObj = extend(mentor.publisher.objectModel.createSystemObj(methods.getId()), methods);
            return systemObj;

        };

        return getSystem();
    };

    return that;

};





