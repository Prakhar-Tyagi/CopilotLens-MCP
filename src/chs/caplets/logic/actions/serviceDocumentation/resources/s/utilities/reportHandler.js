/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global $, mentor, Utils, isReportElementActive, window, packageModel, filterReportDOM, clearTimeout, setTimeout,
 Constants, displayAttributes, getSystemId*/
/**
 * @fileoverview This class is used to load the events for the reports.
 */
var packageModel = (function ()
{
    "use strict";
    var storage = {};
    return {
        set: function (name, value)
        {
            storage[name] = value;
        },
        get: function (name)
        {
            return storage[name];
        }
    };
}());
var ReportEventHandler = function (designs)
{
    "use strict";
    var ReportEventHandler = this;

    this.designs = designs;

    this.mouseout = false;
    this.currentTarget = "";
    // It has the report loaded container id.
    this.reportContainerId = '';
    this.popupData = null;

    this.tdClickHandler = function (event)
    {
        var containerId = ReportEventHandler.reportContainerId, className;
        if (event.target.getAttribute('class') === null) {
            ReportEventHandler.resetReportHighlighting(containerId);
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.CLOSE_POPOVER, {});
            window.hidePopup = true;
        }
        else {
            className = event.target.getAttribute('class').split(' ')[0];
            if (className.split('-')[0] !== 'clickable') {
                ReportEventHandler.resetReportHighlighting(containerId);
                mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.CLOSE_POPOVER, {});
                window.hidePopup = true;
            }
        }
        event.preventDefault();
    };

    this.spanClickHandler = function (event)
    {
        var containerId = ReportEventHandler.reportContainerId;
        if ($("span", this).length > 0) {
            ReportEventHandler.reportCrossHighLight($("span", this), event);
        }
        else {
            ReportEventHandler.resetReportHighlighting(containerId);
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.CLOSE_POPOVER, {});
            window.hidePopup = true;
        }
        event.preventDefault();
    };

    this.columnDblClickHanlder = function (event)
    {
        event.preventDefault();
    };

    this.columnClickHandler = function (event)
    {
        var containerId = ReportEventHandler.reportContainerId;
        if ($("span", this).length > 0) {
            ReportEventHandler.reportCrossHighLight($("span", this), event);
            if (ReportEventHandler.currentTarget.timer) {
                clearTimeout(ReportEventHandler.currentTarget.timer);
                ReportEventHandler.currentTarget.timer = null;
            }
        }
        else {
            ReportEventHandler.resetReportHighlighting(containerId);
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.CLOSE_POPOVER, {});
            window.hidePopup = true;
        }
        event.preventDefault();
        return false;
    };

    this.columnMouseLeaveHandler = function (event) {
        var containerId = ReportEventHandler.reportContainerId;
        $("span", this).each(function () {
            var target = event.target;
            if (target.timer) {
                clearTimeout(target.timer);
                target.timer = null;
                if (!ReportEventHandler.mouseout) {
                    ReportEventHandler.mouseout = true;
                    ReportEventHandler.resetReportHighlighting(containerId);
                }
            }
        });
        event.preventDefault();
    };

    this.columnMouseEnterHandler = function (event) {
        $("span", this).each(function () {
            var target = event.target, uid;
            ReportEventHandler.currentTarget = target;
            if (target.timer) {
                clearTimeout(target.timer);
                target.timer = null;
            }
            uid = $(this).attr("id") || $(this).data('id');
            target.timer = setTimeout(function () {
                ReportEventHandler.mouseout = false;
                ReportEventHandler.highlightSignalPath(uid);
            }, 1000);

        });
        event.preventDefault();
    };

    this.tdElementClickHandler = function (event)
    {
        var containerId = ReportEventHandler.reportContainerId;
        clearTimeout(ReportEventHandler.currentTarget.timer);
        ReportEventHandler.reportCrossHighLight($("span", event.data.tdElem), event, containerId);
        event.preventDefault();
        return false;
    };

    this.tdElementMouseEnterHandler = function (event)
    {
        var target = event.target;
        ReportEventHandler.currentTarget = target;
        if (target.timer) {
            clearTimeout(target.timer);
            target.timer = null;
        }
        target.timer = setTimeout(function ()
        {
            ReportEventHandler.mouseout = false;
            ReportEventHandler.signalTraceFromMultiValuedColumn($("span", event.data.tdElem));
        }, 1000);
        event.preventDefault();
    };

    this.tdElementMouseLeaveHandler = function (event)
    {
        var containerId = ReportEventHandler.reportContainerId, target = event.target;
        if (target.timer) {
            clearTimeout(target.timer);
            target.timer = null;
            if (!ReportEventHandler.mouseout) {
                ReportEventHandler.mouseout = true;
                ReportEventHandler.resetReportHighlighting(containerId);
            }
        }
        event.preventDefault();
    };


    function attachEventHandlersToClickableSpan(reportElement, reportHandler)
    {
        var elem = $(reportElement);
        elem.on("click", reportHandler.spanClickHandler);
        elem = null;
    }

    function attachEventHandlersToClickableColumns(reportElement, reportHandler)
    {
        var elem = $(reportElement);
        elem.on("dblclick", reportHandler.columnDblClickHanlder);
        elem.on("click", reportHandler.columnClickHandler);
        elem.on("mouseenter", reportHandler.columnMouseEnterHandler);
        elem.on("mouseleave", reportHandler.columnMouseLeaveHandler);
        elem = null;
    }

    function attachEventHandlersToMultiValuedColumns(reportElement, reportHandler)
    {
        var tdElement = $(reportElement), cellValue = tdElement.text().split(":"), spanElements;

        //get all the span elements . span elements has UID values of objects
        spanElements = $("span", tdElement);

        if (cellValue.length > 1 && spanElements.length > 1) {
            tdElement.removeClass('clickable-multivalued');
            reportHandler.modifyHtml(tdElement, spanElements, cellValue, "clickable-span");
        }
        else if (cellValue.length === 1) {
            tdElement.on("click", {tdElem: tdElement}, reportHandler.tdElementClickHandler);
            tdElement.on("mouseenter", {tdElem: tdElement}, reportHandler.tdElementMouseEnterHandler);
            tdElement.on("mouseleave", {tdElem: tdElement}, reportHandler.tdElementMouseLeaveHandler);
        }
        tdElement = null;
    }

    this.getSelectedOptions = function ()
    {
        return  window.opener && window.opener.mentor ? window.opener.mentor.publisher.filter.vinOptions :
                mentor.publisher.filter.vinOptions;
    };

    /**
     * This method initialises the events for the report.
     * @param containerId Report container id.
     */
    this.initialiseEvents = function (containerId)
    {
        var selectedOptions = this.getSelectedOptions(), reportHandler = this;

        var dom = $("#" + containerId);
        /**
         * do option expression filtering only when option filter has some options to filter
         */
        if ((typeof (selectedOptions) !== "undefined" && selectedOptions !== null && selectedOptions !== "")) {
            var start = new Date().getTime();

            filterReportDOM(dom, function (optionExpression) {
                return !isReportElementActive(optionExpression);
            });

            var end = new Date().getTime();
            var time = end - start;
            //console.log('Execution time: ' + time);
        }

        ReportEventHandler.reportContainerId = containerId;

        $('.clickable-column', dom).each(function ()
        {
            attachEventHandlersToClickableColumns(this, reportHandler);
        });

        $('.clickable-span', dom).each(function ()
        {
            attachEventHandlersToClickableSpan(this, reportHandler);
        });

        dom.each(function ()
        {
            var elem = $(this);
            elem.on("click", ReportEventHandler.tdClickHandler);
            elem = null;
        });

        //for NetList, and MutltiCore Reports one cell can have two values which are clickable
        $('.clickable-multivalued', dom).each(function ()
        {
            attachEventHandlersToMultiValuedColumns(this, reportHandler);
        });
    };

    /**
     * For signal tracing on mouse-hover of multivalued columns
     */
    this.signalTraceFromMultiValuedColumn = function (tdElement)
    {
        var uidPaths = [], systemuid = '', multicoreUids = '', uid;

        tdElement.each(function ()
        {
            var k, uidString = $(this).attr("id");
            //if the uidString is a '#' seperated string of uids, then add those uids to the list.
            //else add the only uid to the list.
            //the '#' seperated uids could come for multicore list-contents/parent column
            if (uidString.split('#').length > 1) {
                for (k = 0; k < uidString.split('#').length; k = k + 1) {
                    uidPaths.push(uidString.split('#')[k]);
                }
            }
            else {
                uidPaths.push(uidString);
            }
            //systemuid = $(this).attr("data-systemuid");
            multicoreUids = $(this).attr("data-multicorecontentsuid");
        });
        if (uidPaths.length > 1) {
            ReportEventHandler.highlightSignalPath(uidPaths[uidPaths.length - 1]);
            packageModel.set('currentCrossHighlightId', uidPaths[0]);
        }
        else if (uidPaths.length === 1) {
            uid = uidPaths[0];
            if (typeof (multicoreUids) !== "undefined" && multicoreUids !== '') {
                uidPaths = multicoreUids.split(",");
                ReportEventHandler.highlightSignalPath(uid);
                packageModel.set('currentCrossHighlightId', uidPaths[0]);
            }
            else {
                ReportEventHandler.highlightSignalPath(uid);
            }
        }
    };

    /**
     * This method calls at the time of report single click.
     * This highlights the cell and check it is already in active.
     * If it is active then it shows the attributes also.
     * @param tdElement
     * @param event
     */
    this.reportCrossHighLight = function (tdElement, event)
    {
        window.crossHighlightHandler.isReportOrWICTableClick = true;
        var uidPaths = [], systemuid = '', multicoreUids = '', t, uid,  systemId, designForUID;
        t = this;
        tdElement.each(function ()
        {
            //if the uidString is a '#' seperated string of uids, then add those uids to the list.
            //else add the only uid to the list.
            //the '#' seperated uids could come for multicore list-contents/parent column
            var uidString = $(this).attr("id") || $(this).data('id'), k;
            /**
             * when option expressions are used, they are appened with UID.
             */
            uidString = uidString.split("$")[0];

            if (uidString.split('#').length > 1) {
                for (k = 0; k < uidString.split('#').length; k = k + 1) {
                    uidPaths.push(uidString.split('#')[k]);
                }
            }
            else {
                uidPaths.push(uidString);
            }
            systemuid = $(this).attr("data-systemuid");
            multicoreUids = $(this).attr("data-multicorecontentsuid");
        });
        t.popupData = null;
        if (uidPaths.length > 1) {
            this.showAttrAndUpdateSignalTracer(null, uidPaths[uidPaths.length - 1], event,
                    "");

            designs = designs || "";
            designForUID = "";
            window.crossHighlightHandler.highlightSignalPath(uidPaths, undefined,
                    mentor.publisher.colors[mentor.publisher.constants.redColorMsg]);
            window.crossHighlightHandler.crossHighLightAcrossWindows(uidPaths[uidPaths.length - 1], designForUID, "",
                    "", "", "", false);

        }
        else if (uidPaths.length === 1) {
            uid = uidPaths[0];

            this.showAttrAndUpdateSignalTracer(null, uid, event, "");

            setTimeout(function ()
            {
                ReportEventHandler.highlightElementInReport(uid, ReportEventHandler.reportContainerId);
            }, 500);
        }
    };

    this.showAttrAndUpdateSignalTracer = function (schemUID, connUId, event, designUID)
    {
        this.showAttributes(schemUID, connUId, event, designUID);
        this.updateSignaleTracer(connUId, event, designUID);
    };

    this.showAttributes = function (schemUID, connUId, event, designUID)
    {
        require(["routers/multipleDocumentRouter"], function (multipleDocumentRouter)
        {
            multipleDocumentRouter.save(true, connUId);
        });

        this.popupData = displayAttributes(schemUID, connUId, event.clientX, event.clientY, designUID);
        this.popupData.showPopUpPanel(event);
    };

    this.updateSignaleTracer = function (connUId, event, designUID)
    {
        mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.ALT_CLICK_TRIGGERED,
                {altKey: event.altKey});
        mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.UPDATE_SIGNAL_TRACER,
                {systemId: designUID, id: connUId, flush: true});
    };

    /**
     * @return {boolean}
     */
    this.UrlExists = function (address)
    {
        var returnFlag = false;
        $.ajax({ url: address,
            success: function (data, textStatus, XMLHttpRequest)
            {
                returnFlag = true;
            },
            error: function (XMLHttpRequest, textStatus, errorThrown)
            {
                returnFlag = false;
            }, dataType: (Utils.is_msie()) ? "text" : "xml", async: false});
        return returnFlag;
    };

    /**
     * This method is used if the span is multivalued clickble item.
     * @param elementToModify
     * @param idContainer
     * @param texts
     * @param cssClass
     * @param event
     */
    this.modifyHtml = function (elementToModify, idContainer, texts, cssClass)
    {
        var count = 0;
        elementToModify.text('');
        var lastElement;

        idContainer.each(function ()
        {
            var currentElement = $(this);
            currentElement.text(texts[count]);
            count = count + 1;
            currentElement.addClass(cssClass);
            currentElement.on("click", function (event)
            {
                ReportEventHandler.reportCrossHighLight($(this), event, ReportEventHandler.reportContainerId);
            });
            elementToModify.append(currentElement);
            if (count !== texts.length) {
                elementToModify.append(":");
            }
            lastElement = currentElement;
        });
        if (count !== texts.length) {
            var lastElementtext = lastElement.text();
            lastElement.text(lastElementtext + ":" + texts[count]);
        }

    };

    /**
     * This method calls for highlighting the report either for single or double click.
     * @param uid
     * @param sourceContainerId
     */
    this.highlightElementInReport = function (uid, sourceContainerId)
    {
        ReportEventHandler.resetReportHighlighting(sourceContainerId);
        ReportEventHandler.crossHighlightReport(uid, sourceContainerId);
        designs = designs || "";
        window.crossHighlightHandler.initCrossHighlight(uid, "", "", "");
    };



    /**
     * This method removes the highlighting from the current report.
     * @param containerId
     */
    this.resetReportHighlighting = function (containerId)
    {
        $('#' + containerId + ' .highlighted').each(function ()
        {
            $(this).removeClass("highlighted");
        });
        packageModel.set('currentCrossHighlightId', '');
    };

    /**
     * This method applies the highlighting for the report.
     * @param uid
     * @param containerId
     */
    this.crossHighlightReport = function (uid, containerId)
    {
        $('#' + containerId + ' .clickable-column>span[id="' + uid + '"]').parent().addClass('highlighted');
        $('#' + containerId + ' .clickable-span[id="' + uid + '"]').addClass('highlighted');
        $('#' + containerId + '.clickable-multivalued>span[id="' + uid + '"]').parent().addClass('highlighted');
        packageModel.set('currentCrossHighlightId', uid);
    };

    /**
     * This method gets the signal name based on the uid .
     * @param uid
     */
    this.highlightSignalPath = function (uid)
    {
        window.crossHighlightHandler.isReportOrWICTableClick = true;
        uid = uid || "";
        uid = uid.split("$")[0];
        var activeProject = mentor.publisher.project.getId(),
                currentFolder = "",
                objectData = mentor.publisher.objectDataLoader.load(currentFolder, uid, activeProject),
                signal = objectData.getSignal(),
                globalSignal = mentor.publisher.objectDataLoader.load(currentFolder, uid,
                        activeProject).getGlobalSignal();

        if (signal !== "") {
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.HIGHLIGHT_OBJECT_ACROSS_WINDOWS, {
                objectId: uid,
                systemId: currentFolder,
                signalName: signal,
                globalSignalName:globalSignal,
                signal: true,
                color: mentor.publisher.colors[mentor.publisher.constants.orangeColorMsg]
            });
        }
        else if (signal === "") {
            //this will get all the xref ids for shared object. If none, it will return the uid
            var oData = window.crossHighlightHandler.getAllObjectIdsToHighlight(objectData), actualIds = [uid];
            $.each(oData, function (i) {
                actualIds.push(this.objectId);
            });
            //if there is no signal information, just highlight the uid.
            window.crossHighlightHandler.resetWhatsInCommonTable();
            window.crossHighlightHandler.highlightSignalPath(actualIds, false);
            window.crossHighlightHandler.crossHighlightWhatsInCommonTableOnHover(uid, "");
            window.crossHighlightHandler.isReportOrWICTableClick = false;
        }
    };
};
