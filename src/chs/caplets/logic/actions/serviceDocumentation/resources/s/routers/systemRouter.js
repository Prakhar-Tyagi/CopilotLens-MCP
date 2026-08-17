/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
define(["fileDisplayHandler", "componentRouter"],
    function (fileDisplayHandler, componentRouter) {
        return extend(componentRouter, {
            displaySystemDiagram : function (options, systems) {
                var that = this, resetViewerView;
                var diagram, diagramName, matchedSystem;
                diagramName = /*this.getFirstArgumentBeforeQueryString(*/options.parameters.component/*)*/;
                var systemToSearch = options.parameters.system;
                if (diagramAsSystemsObjectFactoryImpl) {
                    systemToSearch = diagramName;
                }
                matchedSystem = that.findElementInCollection(systems, systemToSearch, "System");
                if (!matchedSystem) {
                    matchedSystem = that.findByExactMatch(systems, systemToSearch);
                }
                if (diagramName) {
                    diagram = _.find(matchedSystem.get("getDiagrams")(), function (item) {
                        var name = item.mainText || "";
                        return name.toLowerCase() === diagramName.toLowerCase();
                    });
                } else {
                    diagram = matchedSystem.get("getFirstDiagram")();
                }
                //diagram = this.findComponentByName(options.parameters.system, "systems", diagramName) || "";
                if (diagram) {
                    resetViewerView =  options.parameters ? options.parameters.reset : true;
                    var cd = {
                        systemId: diagram.systemId,
                        id: diagram.systemId,
                        diagramId: diagram.diagramId,
                        reset: resetViewerView,
                        type: mentor.publisher.contentType.SYSTEM_SVG
                    };
                    if (diagramAsSystemsObjectFactoryImpl) {
                        cd.id = diagram.diagramId;
                    }
                    fileDisplayHandler.display(cd);
                } else {
                    alert(mentor.publisher.languageTranslator.localize("AlertCanNotLoadDiaByNameAndType").format(options.component));
                }
            },
            findByExactMatch: function (systems, systemToSearch) {
                var matchedSystems = systems.filter(function (system) {
                    var name = system.attributes.nameAttr || system.attributes.mainText;
                    return systemToSearch === name;
                });
                return matchedSystems.length == 1 ? matchedSystems[0] : undefined;
            },
            openComponent : function (options) {
                var that = this;
                require(["systems"], function (systems) {
                    that.displaySystemDiagram(options, systems);
                });

            }
        });
    });
