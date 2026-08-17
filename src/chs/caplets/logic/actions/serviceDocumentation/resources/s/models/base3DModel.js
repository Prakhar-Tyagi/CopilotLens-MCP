/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
define("base3DModel",['underscore', "backbone"], function (_, Backbone) {
    "use strict";
    var Base3DModel = Backbone.Model.extend({
        defaults: {
            map: {}
        },
        parseSysPathData: function (syss, systemPath)
        {
            var systemObj, system, sysId = $(systemPath).attr("id"), path = $(systemPath).text() || "";
            systemObj = mentor.publisher.project.getObjectById(sysId);
            if (systemObj) {
                system = base3DModel.parseSystemPath(sysId, path, systemObj, systemPath);
                syss.push(system);
            }
            return syss;
        },
        parseSystemPath: function (sysId, path, systemObj, systemPath)
        {
            var system = {}, opExp, firstIndex, lastindex, toolTips, objectType;
            system.systemId = sysId;
            system.folder = systemObj.subText;
            objectType = systemPath["objectType"];
            if (systemObj.getToolTips) {
                toolTips = systemObj.getToolTips() || [];
            }
            system.getToolTips = mentor.publisher.locationViews.createToolTip(toolTips, objectType);
            system.designOptionExpression = systemPath["designOptions"];
            system.diagramId = systemPath["diagramId"];
            system.diagramName = systemPath["diagramName"];
            system.objectId = systemPath["objectConnUID"];
            system.connUID = systemPath["objectConnUID"];
            opExp = systemPath["optionExpressions"];
            system.sharedUID = systemPath["sharedUID"];
            system.objectOptionExpression = systemPath["optionExpressions"];
            system.objectSchemId = systemPath["objectSchemUid"];
            system.optionExpression =
                    createOptionExpressions(system.objectOptionExpression, system.designOptions, "&&");
            system.shortdescription = systemPath["shortdescription"];
            system.id = systemPath["id"];
            system.name = systemPath["name"] + ":" + system.diagramName;
            system.mainText = system.name;
            system.subText = system.folder;
            system.showPopoutButton = true;
            system.path = systemPath["svgPath"] || "";
            path = system.path;
            firstIndex = path.lastIndexOf("\\");
            lastindex = path.lastIndexOf(".svg");
            try {
                if (path != "") {
                    system.diagramId = path.substr(firstIndex + 1, lastindex).replace(".svg", "");
                }
            }
            catch (e) {
            }
            system.idToHighlight = system.id;
            return system;
        },
        // loadSystemPaths: function (name, systemPaths)
        // {
        //     var _data = {}, syss = [], optExprs = [];
        //     _.foldl(systemPaths, this.parseSysPathData, syss)
        //     _.each(syss, function (sys)
        //     {
        //         optExprs.push(sys.optionExpression);
        //     });
        //     _data.systems = syss;
        //     _data.optionExpression = optExprs;
        //     base3DModel.get('map')[name] = _data;
        // },
        filterData: function (_data)
        {
            var syss = [], sys, p = mentor.publisher;
            syss.push(_data || {});
            sys = p.filter.applyFilter(syss)[0] || {};
            return p.configurationsBasedOtherFilter.applyFilter(sys.systems);
        },

    }), base3DModel;
    base3DModel = new Base3DModel();
    return Base3DModel;
});


