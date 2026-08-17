/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global define, Backbone, xmlDataLoader, objectFactoryImpl, mentor*/
define("Diagnostics", ["SectionCollection", "currentPackage"],
        function (BaseCollection, currentPackage) {
            "use strict";
            if (Utils.isPopoutWindow()) {
                mentor.publisher.diagnosticsData = getWindowObj().mentor.publisher.diagnosticsData;
            }
            else {
                var p = mentor.publisher;
                var Diagnostics = BaseCollection.extend({
                    category: p.documentCategory.DIAGNOSTICS,
                    initialize: function () {
                        currentPackage.on("change:id", this.fetch, this);
                        currentPackage.on("change:language", this.fetch, this);
                    },

                    getData: function (project) {
                        var diagnostics = (project && project.getByType('diagnostics')) || [];
                        diagnostics.forEach(function (diagnostic) {
                            diagnostic.getToolTips = function () {
                                function createToolTip(key, value)
                                {
                                    return {
                                        getName: function () {
                                            return key;
                                        },

                                        getValue: function () {
                                            return value;
                                        },
                                    }
                                }

                                var toolTips = [];
                                toolTips.push(createToolTip(Utils.translate("Name"), diagnostic.mainText));
                                toolTips.push(createToolTip(Utils.translate("Description"), diagnostic.subText));
                                if (diagnostic.folder) {
                                    toolTips.push(createToolTip(Utils.translate("Folder"), diagnostic.folder));
                                }
                                return toolTips;
                            };
                            diagnostic.showToolTipAlways = true;
                            diagnostic.nameAttr = diagnostic.mainText;
                            if (diagnostic.folder) {
                                diagnostic.nameAttr =
                                        diagnostic.folder + Utils.getDiagnosticFolderDelimiter() + diagnostic.nameAttr;
                            }
                        });
                        return diagnostics;
                    }
                });
                mentor.publisher.diagnosticsData = new Diagnostics();
            }
            return mentor.publisher.diagnosticsData;
        }
);