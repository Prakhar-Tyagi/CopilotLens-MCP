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
            fileDisplayHandler:fileDisplayHandler,
            getComponentType: function (p) {
                return p.urlParams.componentType;
            }, displayHarnessLayouts : function (options, harnessLayouts) {
                var layout,
                    resetViewerView,
                    diagram,
                    diagrams, p = mentor.publisher;

                layout = this.findElementInCollection(harnessLayouts, options.parameters.harnesslayout, "Harness Layout");
				var componentType = "diagrams", type  = p.contentType.HARNESS_LAYOUT_DIAGRAM;
				if(this.getComponentType(p) === "report") {
					componentType = "reports";
					type  = p.contentType.HARNESS_LAYOUT_REPORT;
				}
                diagrams = layout.getDocumentsInGroupTitled(componentType);

                var component = options.parameters.component;
                if (component) {
                    diagram = diagrams.find(function (item) {
                        var name = item.get("mainText") || "";
                        return name.toLowerCase() === component.toLowerCase();
                    });
                }
                else {
                    diagram = diagrams.at(0);
                }

                if (diagram) {
                    resetViewerView =  options.parameters ? options.parameters.reset : true;
                    this.fileDisplayHandler.display({
						group:componentType,
                        layoutId : layout.id,
                        listItemId : layout.id,
                        id: diagram.id,
                        reset : resetViewerView,
                        type : type
                    });
                } else {
                    alert(mentor.publisher.languageTranslator.localize("AlertCanNotLoadDiaByNameAndType").format(options.component));
                }
            },

            openComponent : function (options) {
                var that = this;
                require(["harnessLayouts"], function (harnessLayouts) {
                    that.displayHarnessLayouts(options, harnessLayouts);
                });

            }
        });
    });