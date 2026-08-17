/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global objectFactoryImpl, $, mentor, extend, Constants, createOptionExpressions, xmlDataLoader, getIdToHighlight*/
var diagramAsSystemsObjectFactoryImpl = function ()
{
    "use strict";
    var that = objectFactoryImpl();
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

        getSystem = function ()
        {
            var systemObj, attribute = that.createAttributeObj(domSystemElement, "Systems"), methods = extend(attribute,
                    {
                        getFolders: function ()
                        {
                            return that.getAttributeFromDomElement("folders", domSystemElement);
                        },

                        getReports: function ()
                        {
                            var reports = that.createObjects("report", domSystemElement);
                            cache(reports);
                            return reports;
                        },
                        getDiagrams: function ()
                        {
                            var diagrams = that.createObjects("diagram", domSystemElement);
                            cache(diagrams);
                            return diagrams;
                        },
                        getFirstDiagram: function ()
                        {
                            var diagrams = this.getDiagrams();
                            return diagrams.length > 0 && diagrams[0];
                        },
                        getTagName: function ()
                        {
                            //todo check internationalization
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
                        systemId: attribute.getId()
                    }), index, diagrams, diagram, length, objects, diagramAssystem, opnEx;
            systemObj = extend(mentor.publisher.objectModel.createSystemObj(methods.getId()), methods);

            mentor.publisher.cache.storeObjectInCache([systemObj]);
            diagrams = systemObj.getDiagrams() || [];
            length = diagrams.length;
            objects = [];
            diagramAssystem = {};
            for (index = 0; index < length; index = index + 1) {
                diagramAssystem = mentor.publisher.object(systemObj);
                diagram = diagrams[index];
                //this is to make sure diagram name does not appear twice
                diagram.getName = function ()
                {
                    return "";
                };

                diagramAssystem.getDiagrams = (function (diagram)
                {
                    return function ()
                    {
                        return [diagram];
                    };
                }(diagram));
                diagramAssystem.idToHighlight = diagrams[index].diagramId;
                //for diagrams as systems flow dont show design name in main text instead show it as tool tip
                diagramAssystem.mainText = /*systemObj.mainText + ":" +*/ diagrams[index].mainText;
                diagramAssystem.diagramId = diagrams[index].diagramId;
                diagramAssystem.idAttribute = diagramAssystem.idToHighlight;

                opnEx = createOptionExpressions(diagrams[index].getOptionExpression(),
                        systemObj.getOptionExpression(),
                        "&&");
                diagramAssystem.getOptionExpression = (function (opExp)
                {
                    return function ()
                    {
                        return opExp;
                    };

                }(opnEx));
                objects.push(diagramAssystem);
            }
            return objects;

        };

        return getSystem();
    };
    return that;

};

mentor.publisher.diagramsAsSystemsObjectDataParser = function (objectDataDOM, systemId, objectUid)
{
    "use strict";
    var objectData = mentor.publisher.objectDataParser(objectDataDOM, systemId, objectUid) || {};

    objectData.getCrossReferences = function (opts)
    {
        var xrefData, xrefs, length, index;
        xrefData = mentor.publisher.objectDataParser(objectDataDOM, systemId, objectUid).getCrossReferences(opts) || {};
        xrefs = xrefData.xrefs || [];
        length = xrefs.length;
        for (index = 0; index < length; index = index + 1) {
            xrefs[index].idToHighlight = xrefs[index].diagramId;
        }
        return xrefData;

    };

    return objectData;
};
mentor.publisher.objectDataLoader.objectParser = mentor.publisher.diagramsAsSystemsObjectDataParser;
mentor.publisher.dataLoader.dataLoader = xmlDataLoader(diagramAsSystemsObjectFactoryImpl());

getIdToHighlight = function (object)
{
    "use strict";
    var path, firstIndex, lastindex;
    object = object || {};
    if (object.diagramId) {
        return object.diagramId;
    }
    path = object.path || "";
    firstIndex = path.lastIndexOf("\\");
    lastindex = path.lastIndexOf(".svg");

    return path.substr(firstIndex + 1, lastindex).replace(".svg", "");
};



