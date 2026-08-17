/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
define(["currentPackage", "preferences"], function (currentPackage, preferences) {
    "use strict";
    return {
        path: "export",
        export: function (currentPackageInfo) {
            function readPreferredExtension()
            {
                var config = mentor.publisher.xmlLoader.loadFile("config.json", false, true, "json");
                return (config && config.data && config.data['saveExcelAs']) || "xls";
            }

            function getEncodedURIIfExists(paramKey, paramVal)
            {
                if (paramVal && paramKey) {
                    return "&" + paramKey + "=" + encodeURIComponent(paramVal);
                }
                return "";
            }

            try {
                var fakeElement = $(
                        "<div id='fakeDiv' style='height:1px;width:1px;' class='grid-ui'><table><tr><td class='modified'></td></tr></table></div>");
                $('body').append(fakeElement);

                var styles = window.getComputedStyle($(".modified")[0]) || {};
                var extension = readPreferredExtension();
                var fontNameWithQuotes = (styles.fontFamily || "\"Segoe UI\"").split(",")[0];
                var fontName = fontNameWithQuotes.substr(1, fontNameWithQuotes.length - 2);
                currentPackageInfo = currentPackageInfo || {};
                if (currentPackageInfo.packageId) {
                    var packageName = currentPackage.get("title");
                    var bgColor = preferences.get("background-color");
                    var path = this.path
                            + "?packageId=" + encodeURI(currentPackageInfo.packageId.replace("data\\", ""))
                            + getEncodedURIIfExists("language", currentPackageInfo.language)
                            + getEncodedURIIfExists("extension", extension)
                            + getEncodedURIIfExists("packageName", packageName)
                            + getEncodedURIIfExists("modifiedTableColumnColor",
                                    styles.backgroundColor || 'rgb(255, 165, 0)')
                            + getEncodedURIIfExists("bgColor", bgColor)
                            + getEncodedURIIfExists("fontName", fontName);
                    window.open(path);
                }
            }
            catch (e) {
                if (window.console) {
                    window.console.log(e);
                }
            }
            finally {
                $(fakeElement).remove();
            }
        }
    };
})
