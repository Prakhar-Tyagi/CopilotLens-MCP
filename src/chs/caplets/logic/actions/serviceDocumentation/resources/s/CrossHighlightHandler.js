/*global Utils, Constants, allpopoutsWindows, window, $, Msg, packageModel, HotSpotTextFinder, removeSelection, zoomInObject, crossHighlightHandler, mentor, require, callFunction*/
var CrossHighlightHandler = function () {
    "use strict";
    var myThis = this;
    this.svgHandlersAndIdsMap = [];
    this.reportContainerids = [];
    this.twoDHotSpotText = '';
    this.handlerHighlightedElementsMap = [];
    this.isReportOrWICTableClick = false;

    this.zoomViews = function () {
        require(["SelectedObjectsStore"], function (store) {
            store.bringToFront();
            var selectedObjectId = mentor.publisher.selectedSystem.get("objectId");
            if (selectedObjectId) {
                var actualIds = mentor.publisher.objectDataLoader.loadRefernceIdsIfAny("", selectedObjectId,
                        mentor.publisher.project.getId()) || [];
                if (actualIds && actualIds instanceof Array && actualIds.length > 0) {
                    selectedObjectId = actualIds[0];
                }
                else {
                    selectedObjectId = actualIds;
                }
                window.crossHighlightHandler.zoomObjectInRapidAuthorViews(selectedObjectId);
            }
        });

    };

    this.flushZoomedViews = function (container) {
        require(["SelectedObjectsStore"], function (store) {
            store.removeContainer(container.svgTransformModel);
        });
    };

    this.triggerHighLightingfrom2DSVG = function (fromWindow, objDataArray, sourceContainerId) {
        var childIndex, aChild, allpopoutsWindows = mentor.publisher.popoutHandler.popouts;
        if (typeof (allpopoutsWindows) === "undefined" ||
                allpopoutsWindows.length === 0 && window.opener && window.opener.mentor) {
            window.opener.crossHighlightHandler.triggerHighLightingfrom2DSVG(fromWindow, objDataArray,
                    sourceContainerId);
            return;
        }
        for (childIndex in allpopoutsWindows) {
            if (allpopoutsWindows.hasOwnProperty(childIndex)) {
                aChild = allpopoutsWindows[childIndex];
                if (!aChild.closed) {
                    if (aChild.self !== fromWindow) {
                        aChild.crossHighlightHandler.twoDHotSpotText = objDataArray[0].name;
                        this.highLightInWindow(aChild, objDataArray, sourceContainerId, true);
                    }
                }
            }
        }
        this.highLightInWindow(window.self, objDataArray, sourceContainerId, false);
    };
    this.getUidToHighlight = function (diagramUID, systemUID, objDataArray) {
        var uidToHighlightForSystemId, k;
        for (k = 0; k < objDataArray.length; k = k + 1) {
            if (objDataArray[k].diagramId === diagramUID) {
                uidToHighlightForSystemId = objDataArray[k].connUID;
                break;
            }
        }
        if (!uidToHighlightForSystemId) {
            //legacy behaviour
            for (k = 0; k < objDataArray.length; k = k + 1) {
                if (objDataArray[k].systemId === systemUID) {
                    uidToHighlightForSystemId = objDataArray[k].connUID;
                    break;
                }
            }
        }
        return uidToHighlightForSystemId;
    };
    this.highLightInWindow = function (windowToHighLight, objDataArray, sourceContainerId, fromParentWindow) {

        var diagramId = windowToHighLight.mentor.publisher.selectedSystem.get("diagramId"),
                systemId = windowToHighLight.mentor.publisher.selectedSystem.get("systemId"),
                childConnectivityId = this.getUidToHighlight(diagramId, systemId,
                        objDataArray);
        //windowToHighLight.crossHighlight(childConnectivityId);
        windowToHighLight.crossHighlightHandler.highElementsInSVG(childConnectivityId, sourceContainerId,
                fromParentWindow);
        windowToHighLight.crossHighlightHandler.highlightElementInReport(childConnectivityId, sourceContainerId);
        windowToHighLight.crossHighlightHandler.highlightElementInWhatsInCommonTable(childConnectivityId,
                sourceContainerId);
        if (!Utils.notNull(childConnectivityId)) {
            $.each(objDataArray, function (i) {
                windowToHighLight.crossHighlightHandler.highElementsInSVG(this.connId, sourceContainerId,
                        fromParentWindow, true);
                windowToHighLight.crossHighlightHandler.highlightElementInReport(this.connId, sourceContainerId, true);
                windowToHighLight.crossHighlightHandler.crossHighlightWhatsInCommonTable(this.connId, sourceContainerId,
                        true);
            });
        }
        this.isReportOrWICTableClick = false;

    };
    this.highlightElementInReport = function (uid, sourceContainerId, notToResetFlag) {
        if (!notToResetFlag) {
            this.resetReportHighlighting(sourceContainerId);
        }
        this.crossHighlightReport(uid, sourceContainerId);
    };

    this.highlightElementInWhatsInCommonTable = function (uid, sourceContainerId, notToResetFlag) {
        if (!notToResetFlag) {
            this.resetWhatsInCommonTable(sourceContainerId);
        }
        this.crossHighlightWhatsInCommonTableOnSelection(uid, sourceContainerId);
    };

    this.highElementsInSVG = function (uid, sourceContainerId, fromParentWindow, notToResetFlag) {
        var selfHighlight, childIndex, svgHandlerWithId;
        if (typeof (fromParentWindow) === "undefined") {
            selfHighlight = false;
        }
        else {
            selfHighlight = fromParentWindow;
        }
        for (var childIndex in this.svgHandlersAndIdsMap) {
            if (this.svgHandlersAndIdsMap.hasOwnProperty(childIndex)) {
                svgHandlerWithId = this.svgHandlersAndIdsMap[childIndex];
                if (sourceContainerId !== svgHandlerWithId[0] || selfHighlight) {
                    //setting hotspot text is for highlighting hotspots in 2D views
                    svgHandlerWithId[1].hotSpotText = this.twoDHotSpotText;
                    try {

                        if (typeof svgHandlerWithId[1].highlighting !== "undefined") {
                            svgHandlerWithId[1].highlighting(uid,
                                    mentor.publisher.colors[mentor.publisher.constants.redColorMsg]);
                        }
                        else {
                            svgHandlerWithId[1].highlightUid(uid,
                                    mentor.publisher.colors[mentor.publisher.constants.redColorMsg],
                                    notToResetFlag);
                        }
                    }
                    catch (e) {
                    }
                }

            }
        }
    };

    this.fromParent = function (uid, sourceContainerId) {
        var childIndex, svgHandlerWithId;
        for (childIndex in this.svgHandlersAndIdsMap) {
            if (this.svgHandlersAndIdsMap.hasOwnProperty(childIndex)) {
                svgHandlerWithId = this.svgHandlersAndIdsMap[childIndex];
                svgHandlerWithId[1].highlightUid(uid, mentor.publisher.colors[mentor.publisher.constants.redColorMsg]);

            }
        }
    };

    this.resetReportHighlighting = function (sourceContainerId) {
        $('#detail .highlighted').each(function () {
            $(this).removeClass('highlighted');
        });
        $('#detail .highlight').each(function () {
            $(this).removeClass('highlight');
        });
    };

    this.resetWhatsInCommonTable = function (sourceContainerId) {
        $('#troubleshootingPanel .highlighted').each(function () {
            $(this).removeClass('highlighted');
        });

        $('#troubleshootingPanel .hovered').each(function () {
            $(this).removeClass('hovered');
        });

        $('#troubleshootingPanel .highlight').each(function () {
            $(this).removeClass('highlight');
        });
    };

    this.crossHighlightReport = function (uid, sourceContainerId) {
        var rowToScrollTo, id, container, isSVGClick;
        $('.clickable-column>span[id*="' + uid + '"]').parent().addClass('highlighted');
        $('.clickable-column[data-id="' + uid + '"]').parent().children().addClass('highlighted');

        $('tr[data-objectid="' + uid + '"]>td').each(function () {
            $(this).addClass('highlight');
        });
        $('.clickable-span[id*="' + uid + '"]').addClass('highlighted');
        $('.clickable-multivalued>span[id*="' + uid + '"]').parent().addClass('highlighted');
        if (window.isSVGClick) {
            if ($('.clickable-column>span[id*="' + uid + '"]').parent().parent().parent().parent().parent()[0]) {
                rowToScrollTo =
                        $('.clickable-column>span[id*="' + uid + '"]').parent().parent()[0];
                id = $('.clickable-column>span[id*="' + uid + '"]').parent().parent().parent().parent().parent()[0].id;
                container = $('#' + id, $('#splitter3')[0]);
                if (container !== null && container.length > 0) {
                    container[0].scrollTop = rowToScrollTo.offsetTop;
                }

            }
            isSVGClick = false;
            window.isSVGClick = false;
        }

    };

    this.crossHighlightWhatsInCommonTable = function (uid, sourceContainerId, cssClassToApplyForHighlight) {
        const container = $('#troubleshootingPanel', $('#splitter3')[0]);

        if (!this.isReportOrWICTableClick) {
            cssClassToApplyForHighlight = "highlighted";
        }

        if (container.length > 0) {
            $('.clickable-column > span[id*="' + uid + '"]').parent().parent().addClass(cssClassToApplyForHighlight);
            $('tr[data-objectId="' + uid + '"]').addClass(cssClassToApplyForHighlight);

            if (!this.isReportOrWICTableClick) {
                const rowToScrollTo = $('.clickable-column > span[id*="' + uid + '"]').parent().parent()[0];
                if (rowToScrollTo) {
                    rowToScrollTo.scrollIntoView();
                }
            }
        }
    };

    this.crossHighlightWhatsInCommonTableOnSelection = function (uid, sourceContainerId) {
        this.crossHighlightWhatsInCommonTable(uid, sourceContainerId, "highlighted");
    };

    this.crossHighlightWhatsInCommonTableOnHover = function (uid, sourceContainerId) {
        this.crossHighlightWhatsInCommonTable(uid, sourceContainerId, "hovered");
    };

    mentor.publisher.eventDispatcher.attachEventListener(mentor.publisher.events.HIGHLIGHT_OBJECT_ACROSS_WINDOWS,
            function (event) {
                //todo load configuration file
                var data = event.detail, color = data.color ||
                        mentor.publisher.colors[mentor.publisher.constants.redColorMsg] ||
                        "red", windowObj = data.windowObj, deHighlight =
                        typeof data.clearOtherHighlights === "undefined" ? true :
                                data.clearOtherHighlights, objectData, signalObjects;
                if (data.signal) {
                    if (!data.signalName) {
                        objectData = mentor.publisher.project.loadObjectData(data.systemId, data.id) || {};
                    }

                    //color = mentor.publisher.config['highlight-onhover-color'] || color; 
                    signalObjects =
                            mentor.publisher.dataLoader.getSignalObjects(data.signalName || objectData.getSignal());
                    if (!signalObjects && data.globalSignalName) {
                        mentor.publisher.dataLoader.getSignalDataForHighlightInRenderedSVG(data.globalSignalName,
                                function (signalData) {
                                    var objectsInSignalPath = (signalData && signalData['dataArray']['objArray']) || [data.objectId];
                                    crossHighlightHandler.highlightSignalPath(objectsInSignalPath,
                                            deHighlight, color, data);
                                });
                    }
                    else {
                        signalObjects = signalObjects || [data.objectId];
                        crossHighlightHandler.highlightSignalPath(signalObjects,
                                deHighlight, color, data);
                    }
                }
                else if (data.objects) {
                    crossHighlightHandler.highlightSignalPath(data.objects, deHighlight, color, data);
                }
                else if (data.objectId) {
                    crossHighlightHandler.initCrossHighlight(data.objectId, "", "", data.systemId, "",
                            data.fromMainWindow);
                }
                else {
                    if (data.id) {
                        crossHighlightHandler.initCrossHighlight(data.id[0], "", "", data.systemId, "",
                                data.fromMainWindow);
                    }

                }

            });

    this.initCrossHighlight = function (uid, sourceContainerId, schematicUID, designUID, popoutId, fromMainWindow) {
        var activeProject, designId, that = this;
        require(["currentPackage", "models/selectedSystem"], function (currentPackage, selectedSystem) {
            designId = designUID || selectedSystem.get("systemId");
            selectedSystem.set("objectId", uid);
            activeProject = currentPackage.id;
            that.crossHighLightAcrossWindows(uid, designId, sourceContainerId, activeProject, popoutId, fromMainWindow,
                    false);
        });
    };

    this.collectUIDsToHighlight = function (relatedObjects, objects) {
        var index;
        if (relatedObjects && relatedObjects.length > 0 && relatedObjects[0].objectids) {
            for (var i in relatedObjects) {
                for (index in relatedObjects[i].objectids) {
                    objects.push({objectId: relatedObjects[i].objectids[index]});
                }

            }
        }
    };

    this.getAllObjectIdsToHighlight = function (objectData) {
        var index, objects = [];
        var harnesses, xrefs, relatedObjects;
        xrefs = objectData.getCrossReferences && callFunction(objectData.getCrossReferences.bind(objectData)) || {};
        harnesses = objectData.getHarnessLayouts && callFunction(objectData.getHarnessLayouts.bind(objectData)) || [];
        relatedObjects =
                objectData.getRelatedObjects && callFunction(objectData.getRelatedObjects.bind(objectData)) || [];
        if (xrefs) {
            objects = objects.concat(xrefs.xrefs);
        }
        if (harnesses) {
            this.collectUIDsToHighlight(harnesses, objects);
        }
        if (relatedObjects) {
            this.collectUIDsToHighlight(relatedObjects, objects);
        }

        return objects;
    };

    this.highlightObjects = function (objects, uidArray, sourceContainerId, popoutId, systemFolderId) {
        $.each(objects, function (i) {
            var notToResetFlag = true;
            //this will highlight in the present window as well as the pop-out
            if (this && this.objectId && $.inArray(this.objectId, uidArray) === -1) {
                myThis.initiateCrossHighlight(this.objectId, sourceContainerId, null, notToResetFlag, popoutId,
                        {objectId: this.objectId, systemId: systemFolderId});
                uidArray.push(this.objectId);
            }
        });
    };

    this.highlightShieldBodyIds =
            function (shieldBodyUIDs, uid, sourceContainerId, uidArray, popoutId, systemFolderId) {
                if (shieldBodyUIDs.length > 0) {
                    var that = this;
                    $.each(shieldBodyUIDs, function (i) {
                        that.initiateCrossHighlight(uid, sourceContainerId, true);
                        var notToResetFlag = true;
                        //this will highlight in the present window as well as the pop-out
                        if ($.inArray(this.id, uidArray) === -1) {
                            myThis.initiateCrossHighlight(this.id, sourceContainerId, null, notToResetFlag, popoutId,
                                    {objectId: this.id, systemId: systemFolderId});
                            //myThis.initiateCrossHighlight(this.id, sourceContainerId, null, notToResetFlag);
                            uidArray.push(this.id);
                        }
                    });
                }
            };
    this.crossHighLightAcrossWindows =
            function (uid, systemFolderId, sourceContainerId, activeProject, popoutId, fromMainWindow, notResetFlag) {
                var index, highlightFlag = true, uidArray = [], objectData = mentor.publisher.objectDataLoader.load(
                                systemFolderId,
                                uid, mentor.publisher.project.getId()) ||
                        {}, xrefs, objects = [], sheldBodyUIDs, data = {
                    objectId: uid,
                    systemId: systemFolderId,
                    fromMainWindow: fromMainWindow
                }, harnesses;

                this.initiateHighlightInChildWindow(uid, sourceContainerId, notResetFlag, this.twoDHotSpotText,
                        popoutId, data);

                this.initiateCrossHighlight(uid, sourceContainerId, null, notResetFlag, popoutId,
                        data);

                uidArray.push(uid);
                //objectData.get3DViews();
                objects = this.getAllObjectIdsToHighlight(objectData);
                this.highlightObjects(objects, uidArray, sourceContainerId, popoutId, systemFolderId);
                sheldBodyUIDs = callFunction(objectData.getShieldBodyUIDs) || [];
                //this is used for multicore highlighting
                this.highlightShieldBodyIds(sheldBodyUIDs, uid, sourceContainerId, uidArray, popoutId, systemFolderId);
            };

    this.initiateCrossHighlight = function (uid, sourceContainerId, fromParentWindow, notToResetFlag, popoutId, data) {
        var isLocationviewOpen, panelIdFor2DViwe = mentor.publisher.detailLayoutManager.getPanelId(
                mentor.publisher.contentType.CUSTOM_VIEW), objectTagNo;
        objectTagNo = $("#" + panelIdFor2DViwe + " object").length;

        if (objectTagNo === 1) {
            this.twoDHotSpotText = mentor.publisher.locationViews.getLocationViewByObjectId(uid);
        }
        else {
            this.twoDHotSpotText = '';
        }
        this.highElementsInSVG(uid, sourceContainerId, fromParentWindow, notToResetFlag);
        this.highlightElementInReport(uid, sourceContainerId, notToResetFlag);
        this.highlightElementInWhatsInCommonTable(uid, sourceContainerId, notToResetFlag);
        this.isReportOrWICTableClick = false;

    };

    this.initiateHighlightInChildWindow =
            function (uid, sourceContainerId, notToResetFlag, twoDHotSpotText, popoutId, data) {
                mentor.publisher.popoutHandler.highlighObject(uid, sourceContainerId, notToResetFlag, twoDHotSpotText,
                        popoutId, data);
            };

    this.crossHighlighJTViews = function (jtIds) {
        var allpopoutsWindows = mentor.publisher.popoutHandler.popouts, k, popW;
        try {
            this.crossHighlightInAJTView(document, jtIds);
            for (k = 0; k < allpopoutsWindows.length; k = k + 1) {
                popW = allpopoutsWindows[k];
                this.crossHighlightInAJTView(popW.document, jtIds);
            }
        }
        catch (e) {
        }
    }

    this.crossHighlightInAJTView = function (doc, jtIds) {
        var psid, selectedPsids, k, relatedPartAlreadySelected = false, selId;
        try {
            if (doc.jtViewerManager) {
                selectedPsids = doc.jtViewerManager.getSelectedParts();
                if (selectedPsids) {
                    for (k = 0; k < selectedPsids.length; k = k + 1) {
                        selId = selectedPsids[k].substr(selectedPsids[k].lastIndexOf(":") + 1, selectedPsids[k].length);
                        if (jtIds.indexOf(selId) != -1) {
                            relatedPartAlreadySelected = true;
                        }
                    }
                    //check if jt parts to highlight contain any of the already selected parts in the document
                    //if so , we will not change the zoom and highlighting of this document
                    //else we will un-select all the already selected parts and zoom/highlight the first part from the list
                    if (!relatedPartAlreadySelected) {
                        for (k = 0; k < selectedPsids.length; k = k + 1) {
                            doc.jtViewerManager.setSelectionByPsId(selectedPsids[k], false);
                        }

                        if (doc.psidVSZ) {
                            for (k = 0; k < jtIds.length; k += 1) {
                                psid = "1:" + jtIds[k];
                                doc.psidVSZ(psid);
                                selectedPsids = doc.jtViewerManager.getSelectedParts();
                                if (selectedPsids && selectedPsids.length > 0) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (e) {
        }
    }

    /**
     * Select the matching RapidAuthor model Item in all open model views
     * @param raItemNames An Array of the RA Item Names to highlight
     * @param srcDocument The document that was the event source - used to prevent
     * calling back to the originator
     * @param srcIs3D Did the event originate in a RA 3D view
     */
    this.crossHighlightInRapidAuthorViews = function (raItemNames, srcDocument, srcIs3D) {
        if (Utils.isPopoutWindow()) {
            //call handler on parent window
            window.opener.crossHighlightHandler.crossHighlightInRapidAuthorViews(raItemNames, srcDocument, srcIs3D);
        }
        else {
            var allpopoutsWindows = mentor.publisher.popoutHandler.popouts, k, popW;
            try {
                if (document !== srcDocument || !srcIs3D) {
                    this.crossHighlightInRapidAuthorView(document, raItemNames);
                }
                for (k = 0; k < allpopoutsWindows.length; k = k + 1) {
                    popW = allpopoutsWindows[k];
                    if (!popW.closed && popW.document !== srcDocument) {
                        this.crossHighlightInRapidAuthorView(popW.document, raItemNames);
                    }
                }
            }
            catch (e) {
            }
        }
    };

    /**
     * Highlight all the matching RapidAuthor model Items in the specified list in the specified document
     * @param doc The HTML document that may contain an RA 3D view
     * @param raItemIds An array of the RA Item Names to highlight
     */
    this.crossHighlightInRapidAuthorView = function (doc, raItemIds) {
        try {
            if ($("#RA3DViewLoadArea", doc)) {
                if (raItemIds) {
                    doc.defaultView.mentor.publisher.rapidAuthorCatalogPanel.highlightItems(raItemIds);
                }
                else {
                    doc.defaultView.mentor.publisher.rapidAuthorCatalogPanel.clearHighlighting();
                }
            }
        }
        catch (e) {
        }
    };

    this.zoomItemInRapidAuthorViews = function (item) {
        if (Utils.isPopoutWindow()) {
            window.opener.crossHighlightHandler.zoomItemInRapidAuthorViews(item);
        }
        else {
            var allpopoutsWindows = mentor.publisher.popoutHandler.popouts, k, popW;
            try {
                this.zoomItemInRapidAuthorView(document, item);
                for (k = 0; k < allpopoutsWindows.length; k = k + 1) {
                    popW = allpopoutsWindows[k];
                    if (!popW.closed) {
                        this.zoomItemInRapidAuthorView(popW.document, item);
                    }
                }
            }
            catch (e) {
            }
        }
    };

    this.zoomItemInRapidAuthorView = function (doc, item) {
        try {
            if (item && doc.defaultView.mentor.publisher.rapidAuthorCatalogPanel.isVisible()) {
                var itemNames = _.uniq(item.objectNames);
                doc.defaultView.mentor.publisher.rapidAuthorCatalogPanel.zoomTo3dModel(itemNames);
            }
        }
        catch (e) {
        }
    };

    this.zoomObjectInRapidAuthorViews = function (connUID) {
        if (Utils.isPopoutWindow()) {
            window.opener.crossHighlightHandler.zoomObjectInRapidAuthorViews(connUID);
        }
        else {
            var allpopoutsWindows = mentor.publisher.popoutHandler.popouts, k, popW;
            try {
                this.zoomObjectInRapidAuthorView(document, connUID);
                for (k = 0; k < allpopoutsWindows.length; k = k + 1) {
                    popW = allpopoutsWindows[k];
                    if (!popW.closed) {
                        this.zoomObjectInRapidAuthorView(popW.document, connUID);
                    }
                }
            }
            catch (e) {
            }
        }
    };

    this.zoomObjectInRapidAuthorView = function (doc, connUID) {
        try {
            if (connUID && $("#RA3DViewLoadArea", doc)) {
                doc.defaultView.mentor.publisher.rapidAuthorCatalogPanel.zoomObjects(connUID)
            }
        }
        catch (e) {
        }
    };

    this.zoomObjectIn3DXML = function (objectPathIn3dXML, type) {
        var childIndex, oldPartNumber, aChild, allpopoutsWindows = mentor.publisher.popoutHandler.popouts, psid,
                selectedPsids;
        try {
            oldPartNumber = packageModel.get('partNumber');
            removeSelection(packageModel.get('partNumber'));
            packageModel.set('partNumber', objectPathIn3dXML);
            //partNumber = objectPathIn3dXML;
            zoomInObject();
            if (typeof (allpopoutsWindows) === "undefined" || allpopoutsWindows.length === 0) {
                return;
            }
            for (childIndex in allpopoutsWindows) {
                if (allpopoutsWindows.hasOwnProperty(childIndex)) {
                    aChild = allpopoutsWindows[childIndex];
                    if (typeof (aChild) !== "undefined" && typeof (aChild.removeSelection) !== "undefined" &&
                            !aChild.closed) {
                        if (typeof (oldPartNumber) !== "undefined") {
                            aChild.removeSelection(oldPartNumber);
                            aChild.packageModel.set('partNumber', objectPathIn3dXML);
                            aChild.zoomInObject(packageModel.get('partNumber'));
                        }
                    }
                }

            }
        }
        catch (e) {
        }

    };

    this.highlightSignalPath = function (objectsInSignalPath, checkFlag, color, data) {
        try {

            var highlightColor = '', childIndex, svgHandlerWithId, aChild, objectUid;
            if (typeof (checkFlag) === 'undefined') {
                checkFlag = true;
            }
            if (checkFlag) {
                //////console.log("dehighlight?" + checkFlag);
                this.resetReportHighlighting("");
                this.resetWhatsInCommonTable("");
                for (childIndex in objectsInSignalPath) {
                    if (objectsInSignalPath.hasOwnProperty(childIndex)) {
                        objectUid = objectsInSignalPath[childIndex];
                        this.crossHighlightReport(objectUid, "");
                        this.crossHighlightWhatsInCommonTableOnHover(objectUid, "");
                    }
                }
                this.isReportOrWICTableClick = false;
            }

            if (typeof (color) === 'undefined') {
                highlightColor = mentor.publisher.colors[mentor.publisher.constants.orangeColorMsg];
            }
            else {
                highlightColor = color;
            }
            for (childIndex in this.svgHandlersAndIdsMap) {
                if (this.svgHandlersAndIdsMap.hasOwnProperty(childIndex)) {
                    svgHandlerWithId = this.svgHandlersAndIdsMap[childIndex];
                    try {
                        svgHandlerWithId[1].highlightUids(objectsInSignalPath, highlightColor);
                    }
                    catch (e) {
                        // delete  svgHandlerWithId[1];
                    }

                }
            }
            /*            if (typeof (allpopoutsWindows) === "undefined") {
             return;
             }*/
            if (data && data.objectId) {
                mentor.publisher.popoutHandler.highlighObject("", "", "", "", "", data);
            }
            else {
                mentor.publisher.popoutHandler.highlightSignalPath(objectsInSignalPath, checkFlag, color, data);
            }
        }
        catch (e) {
            //
        }
    };

};
var svgEventHandlers = {};
var addNewSvgEventHandler = function (svgContainerId, svgEventHandler) {
    var newSvgHandlers = [];
    for (childIndex in window.crossHighlightHandler.svgHandlersAndIdsMap) {
        var svgHandlerWithId = window.crossHighlightHandler.svgHandlersAndIdsMap[childIndex];
        if (svgContainerId !== svgHandlerWithId[0]) {
            newSvgHandlers.push(svgHandlerWithId);
        }
    }
    newSvgHandlers.push([svgContainerId, svgEventHandler]);
    delete crossHighlightHandler.svgHandlersAndIdsMap;
    crossHighlightHandler.svgHandlersAndIdsMap = newSvgHandlers;
    delete svgEventHandlers[svgContainerId];
    svgEventHandlers[svgContainerId] = svgEventHandler;
};

window.crossHighlightHandler = new CrossHighlightHandler();
mentor.publisher.eventDispatcher.attachEventListener(mentor.publisher.events.RESIZE_SVG, function (evt) {
    var data = evt.detail || {};
    for (childIndex in window.crossHighlightHandler.svgHandlersAndIdsMap) {
        var svgHandlerWithId = window.crossHighlightHandler.svgHandlersAndIdsMap[childIndex];
        if (svgHandlerWithId[1] && svgHandlerWithId[0] !== data.containerId && svgHandlerWithId[1].zoomLockedView) {
            svgHandlerWithId[1].zoomLockedView();
        }
    }
    require(["SelectedObjectsStore"], function (store) {
        store.panToMiddle();
    });
});



