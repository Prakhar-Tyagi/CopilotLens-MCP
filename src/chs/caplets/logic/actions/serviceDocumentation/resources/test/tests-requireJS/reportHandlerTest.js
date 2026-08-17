/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
(function ()
{
    "use strict";
    describe("reportHandlerTest", function ()
    {
        var reportEventHandler = new ReportEventHandler("dummDesignID"), connID, systemID, connIDToHighlight, actualsourceContainerID;
        var origShortenedUIDMap = mentor.publisher.shortenedUIDMap;
        beforeEach(function ()
        {
            var report = '<div id="report"><div class="systemReportPanel panel_content" id="panelContentArea">' +
                    '<table class="reportTableStyle" id="tableid1"><tr><td>Name</td></tr><tr><td class="clickable-column">' +
                    '<span id="UID8ebc2b-144974f2ee7-121dedf945d7c55d48682816d928ea46"></span>W041</td></tr></tbody></table></div></div>';
            $('body').append($(report));
            reportEventHandler.showAttrAndUpdateSignalTracer = function (schemUID, connUId, event, designUID)
            {
                connID = connUId;
                systemID = designUID;
            };

            reportEventHandler.getSelectedOptions = function() {
                return "";
            };

            reportEventHandler.highlightElementInReport = function(uid, sourceContainerId) {
                connIDToHighlight = uid;
                actualsourceContainerID = sourceContainerId;
            };
            mentor.publisher.shortenedUIDMap = {
                getShortenedObjectUID:function (objectId, systemId, projectId)
                {
                    return objectId;
                },
                getShortenedDesignUID:function (systemId, projectId)
                {
                    return systemId;
                }
            };
        });

        afterEach(function ()
        {
            mentor.publisher.shortenedUIDMap = origShortenedUIDMap;
            $("#report").remove();
        });
        it("Single click should show object popovers from reports", function ()
        {
            runs(function () {
                reportEventHandler.initialiseEvents("panelContentArea");
                /*
                 click the column with UID
                 */
                $("#tableid1 .clickable-column").trigger("click");
            }, "trigger click");
            waitsFor(function () {
                return connID==='UID8ebc2b-144974f2ee7-121dedf945d7c55d48682816d928ea46' && connIDToHighlight && actualsourceContainerID;
            }, "wait for the variables to be updated", 501);
            runs(function () {
                expect(connID).toBe("UID8ebc2b-144974f2ee7-121dedf945d7c55d48682816d928ea46");
                expect(connIDToHighlight).toBe("UID8ebc2b-144974f2ee7-121dedf945d7c55d48682816d928ea46");
                expect(actualsourceContainerID).toBe("panelContentArea");
            }, "expect tests");
        });

    });
})();
