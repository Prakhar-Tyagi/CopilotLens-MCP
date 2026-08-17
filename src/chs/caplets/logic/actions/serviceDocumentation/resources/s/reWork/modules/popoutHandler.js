/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global window, mentor*/
mentor.publisher.popoutHandler = (function (p) {
    "use strict";
    var popouts = [], URLs = {}, contentType = mentor.publisher.contentType;

    URLs[contentType.CAPITAL_REPORT] = "popout.html#/globalReport/__mainText__/__projectId__/__path__";
    URLs[contentType.SYSTEM_REPORT] = "popout.html#/report/__systemId__/__reportId__/__projectId__";
    URLs[contentType.HARNESS] = "popout.html#/showHarness/__mainText__/__projectId__";
    URLs[contentType.OBJECT_REPORT] = "popout.html#/report/__mainText__/__systemId__/__projectId__/__path__";
    URLs[contentType.SYSTEM_SVG] = "popout.html#/system/__systemId__/__diagramId__/__projectId__/__objectId__";
    URLs[contentType.JT_3D] = "popout.html#/threeDXML/__mainText__/__projectId__/__type__/__objectId__/__path__";
    URLs[contentType.RA_3D] = "popout.html#/ra3DXML/__mainText__/__projectId__/__objectId__/__path__";
    URLs[contentType.LOCATION_VIEWS] = "popout.html#/showLocation/__mainText__/__projectId__/__objectId__";
    URLs[contentType.OLD_DESIGN_REVISION] =
            "popout.html#/olddesignrevision/__layoutId__/__diagramId__/__projectId__/__objectId__";
    URLs[contentType.NEW_DESIGN_REVISION] =
            "popout.html#/newdesignrevision/__layoutId__/__diagramId__/__projectId__/__objectId__";
    URLs[contentType.CUSTOM_VIEW] = "popout.html#/customFile/__mainText__/__projectId__/__path__";
    URLs[contentType.TROUBLESHOOT] = "popout.html#/troubleshoot/__projectId__/__activeCodes__/__passiveCodes__";
    URLs[contentType.FAULT_OBJECT_TABLE] =
            "popout.html#/faultObjectTable/__projectId__/__activeCodes__/__passiveCodes__";

    return {
        openPopoutWindow: function (content) {
            content = content || {};
            this.getPopoutOpener().mentor.publisher.popoutHandler.data = content.data;
            this.getPopoutOpener().mentor.publisher.popoutHandler.events = content.events;
            this.getPopoutOpener().mentor.publisher.popoutHandler.projectId = mentor.publisher.project.getId();
            this.getPopoutOpener().mentor.publisher.popoutHandler.vinOptions = mentor.publisher.filter.vinOptions;
            this.getPopoutOpener().mentor.publisher.popoutHandler.languageCode =
                    mentor.publisher.languageTranslator.currentLanguage();
            this.openPopout();
        },
        highlighObject: function (uid, sourceContainerId, notToResetFlag, twoDHotSpotText, popoutId, object) {
            try {
                if (uid) {
                    object = object || {objectId: uid};
                    var popoutsLength = popouts.length, index;
                    for (index = 0; index < popoutsLength; index = index + 1) {
                        if (popouts[index] && !popouts[index].closed && popoutId !== popouts[index].popoutId) {
                            object.fromMainWindow = true;
                            popouts[index].mentor.publisher.eventDispatcher.dispatchEvent(
                                    p.events.HIGHLIGHT_OBJECT_ACROSS_WINDOWS,
                                    object);
                        }
                    }
                }
            }
            catch (e) {

            }

        },
        highlightSignalPath: function (objectsInSignalPath, checkFlag, color, data) {
            try {
                data = data || {};
                data.fromMainWindow = true;
                var popoutsLength = popouts.length, index;
                for (index = 0; index < popoutsLength; index = index + 1) {
                    if (popouts[index] && !popouts[index].closed && data.popoutId !== popouts[index].popoutId) {
                        popouts[index].crossHighlightHandler.highlightSignalPath(objectsInSignalPath, checkFlag, color,
                                data);
                    }
                }
            }
            catch (e) {

            }
        },
        getPopoutOpener: function () {
            if (window.opener && window.opener.mentor) {
                return window.opener;
            }
            else {
                return window;
            }
            // return window.opener || window;
        },
        openPopout: function (url, openInTab, windowFeatures) {
            var popoutWindow,
                    height = 600,
                    width = 800;
            url = url || 'popout.html';

            if (screen.height && screen.height < height) {
                height = screen.height;
            }

            if (screen.width && screen.width < width) {
                width = screen.width;
            }

            var mainWindow = getWindowObj();
            if (openInTab) {
                popoutWindow = mainWindow.open(url, "_blank", windowFeatures);

            }
            else {
                popoutWindow = mainWindow.open(url, "",
                        "width=" + width + ",height=" + height +
                        ",toolbar=no,location=0,status=no,menubar=no,resizable=1,left=0,top=0" +
                        (windowFeatures ? ("," + windowFeatures) : ""));

            }

            if (popoutWindow != null) {
                popoutWindow.opener = popoutWindow.opener || mainWindow;
                popouts.push(popoutWindow);
            }
        },
        closePopoutWindows: function () {
            try {
                var popoutsLength = popouts.length, index;
                for (index = 0; index < popoutsLength; index = index + 1) {
                    if (popouts[index] && !popouts[index].closed) {
                        popouts[index].close();
                    }
                }
            }
            catch (e) {

            }
        },
        createURL: function (parameters) {

            var urlForType = URLs[parameters.type] || "";
            parameters = parameters || {};
            for (var property in parameters) {
                if (parameters.hasOwnProperty(property)) {
                    var value = parameters[property];
                    if (property === "mainText" || property === "reportId") {
                        value = encodeURIComponent(value);
                    }
                    if (value) {
                        urlForType = urlForType.replace("__" + property + "__", value.replace("\\", "/"));
                    }
                }
            }
            return urlForType;
        },
        closePopover: function () {
            try {
                var popoutsLength = popouts.length, index;
                for (index = 0; index < popoutsLength; index = index + 1) {
                    if (popouts[index] && !popouts[index].closed && popouts[index].mentor) {
                        popouts[index].mentor.publisher.eventDispatcher.dispatchEvent(
                                mentor.publisher.events.CLOSE_POPOVER,
                                {});
                    }
                }
            }
            catch (e) {

            }
        },
        popouts: popouts
    };
}(mentor.publisher));

var popoutWindowHandle = function (child) {
    "use strict";
    child.childInitialized(mentor.publisher.popoutHandler.popouts.length);
};



