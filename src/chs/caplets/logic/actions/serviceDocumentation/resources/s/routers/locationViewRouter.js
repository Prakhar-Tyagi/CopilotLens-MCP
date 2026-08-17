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
           /* openComponent : function (options) {
                var component = this.findComponentByName(options.componentName, options.componentType) ||
                    "";
                if (component) {
                    mentor.publisher.router.open2DView(component.objectId, component.systemId);
                } else {
                    alert("can not load object by name and type " + options.componentName + " " +
                        options.componentType);
                }
            },*/

            showComponent : function (content) {
                mentor.publisher.router.open2DView(content.objectId, content.systemId);

                require(["SignalTracerModel"], function (signalTraceModel) {
                    signalTraceModel.updateData(content.systemId, content.objectId);
                });
            }
        });
    });
