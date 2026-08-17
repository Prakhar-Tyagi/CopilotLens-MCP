/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

define(["fileDisplayHandler", "componentRouter", "models/selectedSystem"],
    function (fileDisplayHandler, componentRouter, selectedSystem) {
        var typeToCollectionMap = {
            harness : "Harnesses",
            locationviews : "LocationViews",
            faultcode : "FaultCodes",
            diagnostic : "Diagnostics",
            globalreports: "GlobalReports",
            capitalreport: "CustomDataCollection",
            customView: "PanelDataCollection",
            JT: "PanelDataCollection",
            RA: "PanelDataCollection"
        };

        function isPDFContent(content) {
            return content && content.get && getPluginType(content.get('path'))
                    && getPluginType(content.get('path')).indexOf("pdf") > 0;
        }

        return extend(componentRouter, {

            openComponent : function (options) {
                var diagram, objectId, content, that = this, resetViewerView, componentType;
                options.contentType = options.contentType && options.contentType.toLowerCase();
                if (options.componentType) {
                    require([typeToCollectionMap[options.componentType]], function (collection) {
                        if (options.componentType === mentor.publisher.contentType.CAPITAL_REPORT && mentor.publisher.urlParams.viewName) {
                            const capitalReportObject = mentor.publisher.project.getByType(options.componentType);
                            const capitalReport = capitalReportObject.filter(capitalReport => capitalReport.name === mentor.publisher.urlParams.viewName);
                            if (capitalReport && capitalReport[0] && capitalReport[0].withoutTranslation && capitalReport[0].withoutTranslation.objectId) {
                                const model = mentor.publisher.project.loadObjectData(selectedSystem.get("systemId"), capitalReport[0].withoutTranslation.objectId);
                                collection.fetch(model);
                                collection.getModels().filter(collec => {
                                    if (collec) {
                                        const dataContent = collection.findDataContent(collec, mentor.publisher.urlParams.viewName);
                                        dataContent.id = dataContent.objectId;
                                        fileDisplayHandler.display(dataContent);
                                    }
                                });
                            }
                        } else {
                            if (collection) {
                                content = that.findElementInCollection(collection, options.componentName,
                                        options.componentType);
                                /* DOCS-9831, DOCS-9692
                                 * Following code normalize (setting a direct property on object) the model for use in fileDisplayHandler.
                                 * Given that the content we are gettingfrom a Backbone collection and
                                 * each backbone collection in smartClient is backed by a Backbone Model, we are using '.get'
                                 * on the content.
                                 * For Models that do not have type or id attribute on them, options.componentType and
                                 * options.componentName are fallback values resectively.
                                 */
                                if (content) {
                                    content.type = content.get('type') || options.componentType;
                                    content.path = content.get('path');
                                    content.mainText = content.get('mainText');
                                    content.id = content.get('id') || options.componentName;
                                }
                            }
                            else {
                                resetViewerView = options.parameters ? options.parameters.reset : true;
                                content = {
                                    id: options.componentName,
                                    'reset': resetViewerView, // reset is a reserved keyword in javascript
                                    type: options.contentType,
                                    mainText: options.componentName
                                };
                                content.type = options.componentType || mentor.publisher.contentType.CUSTOM_VIEW;
                            }

                            if (isPDFContent(content)) {
                                content.type = mentor.publisher.contentType.CUSTOM_VIEW;
                            }

                            fileDisplayHandler.display(content);
                        }
                    });
                }
            }
        });
    });
