/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global jQuery, document, $, Msg, javaPluginInstalled, callParent, window, mentor, addDocumentEventListener, Msg, splitPanelView, crossHighlightHandler, RenderConnectivityHandler, restoreRenderConnectivityState, Msg, initializeSignalRenderer*/
jQuery(document).ready(function () {
    "use strict";
    callParent();
});
/**/
function callParent() {
    "use strict";
    window.opener.popoutWindowHandle(window);
}

var childInitialized = function (popoutId) {
    "use strict";
    restoreRenderConnectivityState();
    window.popoutId = popoutId;
	window.touchEnabled = window.opener.touchEnabled;
	window.heavySVGs = window.opener.heavySVGs;
};

window.crossHighlightHandler.initiateHighlightInChildWindow =
    function (uid, sourceContainerId, fromParentWindow, notToResetFlag, popoutId, data) {
        "use strict";
        if (!data.fromMainWindow) {
            window.opener.crossHighlightHandler.initCrossHighlight(uid, "", "",
                data.systemId, window.popoutId);
        }

    };

splitPanelView.addSpaceBetPathAndConnectorInfo = function (pathInfo, connInfo) {
    "use strict";
    return pathInfo + (connInfo ? (", " + connInfo) : "");
};

mentor.publisher.popoutHandler.highlightSignalPath = function (objectsInSignalPath, checkFlag, color, data) {
    "use strict";
    data = data || {};
    if (!data.fromMainWindow) {
        data.popoutId = window.popoutId;
        window.opener.crossHighlightHandler.highlightSignalPath(objectsInSignalPath, checkFlag, color, data);
    }
};

function restoreRenderConnectivityState() {
    "use strict";
    initializeSignalRenderer(window.parent.isHTTPProtocol());
}


function applyConfigurationFilter(vinOptions) {
    "use strict";
    window.opener.applyConfigurationFilter(vinOptions);
    window.close();
}

function resetConfigurationFilter() {
    "use strict";
    window.opener.resetConfigurationFilter();
}

function applyVINFilter(vinOptions, fromConfigurationFilter) {
    "use strict";
    window.opener.applyVINFilter(vinOptions, fromConfigurationFilter);
    window.close();
}

