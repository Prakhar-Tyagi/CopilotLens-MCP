/**
 * Created with IntelliJ IDEA.
 * User: mukumar
 * Date: 10/10/12
 * Time: 11:50 AM
 * To change this template use File | Settings | File Templates.
 */
/*global $,  assertEquals, assertTrue, describe, beforeEach, afterEach, it, expect, Backbone, mentor */
describe("ContentAreaTest", function ()
{
    "use strict";
    var notifySystemChange, systemChanged;
    beforeEach(function ()
    {

        $('body').append($("<div id='splitter1'><div id='someId' class='panel_content'></div></div>"));
        mentor.publisher.detailLayoutManager.reset();
        notifySystemChange = mentor.publisher.contentArea.notifyOfSystemChange;
        systemChanged = false;
        mentor.publisher.contentArea.notifyOfSystemChange = function ()
        {
            systemChanged = true;
        }
    });
    afterEach(function ()
    {
        mentor.publisher.contentArea.notifyOfSystemChange = notifySystemChange;
        $("#splitter1").remove();

        window.LoadMask = {
            removeLoadMask: function ()
            {
            },
            addLoadMask: function ()
            {
            }
        };
        mentor.publisher.contentArea.clearContent("splitter1");
        mentor.publisher.detailLayoutManager.close(mentor.publisher.contentType.SYSTEM_REPORT);

    });
    //todo this has conflict with some global state and fails when run together with other tests. otherwise it passes 
    it("test layoutManager should be able to open system SVG", function ()
    {
        var container, Content = Backbone.Model.extend();
        var content = {
            type: mentor.publisher.contentType.OLD_DESIGN_REVISION,
            systemId: "testSystemID"
        };
        container = mentor.publisher.contentArea.layoutContentPanel(content, false);
        expect("someId").toBe(container);

    });

    it("test contentArea should be able return all open contents", function ()
    {
        // setup a dummy DOM
        var $div = $('<div>', {
            id: 'detail'
        });
        $('body').append($div);
        var contents, container, Content = Backbone.Model.extend({
            path: "testpath",
            type: mentor.publisher.contentType.SYSTEM_SVG,
            systemId: "testSystemID"
        });
        container = mentor.publisher.contentArea.layoutContentPanel(new Content(), false);
        expect(mentor.publisher.contentArea.getAllOpenContentDetails()).toBeDefined();

        // Remove the dummy DOM
        $div.remove();
    });
    it("test should be able to close an existing panel", function ()
    {

        var container, Content = Backbone.Model.extend({
            type: mentor.publisher.contentType.SYSTEM_SVG,
            systemId: "testSystemID"
        }), panelClosed = false, navigationPanelSelectionCleared = false;

        var activeContent = new Content();
        mentor.publisher.contentArea.setActiveContent(activeContent);
        container = mentor.publisher.contentArea.layoutContentPanel(activeContent, false);
        activeContent.on("change:clearNavigationPanelSelection", function ()
        {
            navigationPanelSelectionCleared = true;
        });

        mentor.publisher.contentArea.closeExistingPanel(activeContent, {
            cid: "1", close: function ()
            {
                panelClosed = true;
            }
        });

        mentor.publisher.contentArea.closeExistingPanel(activeContent, {
            cid: "2", close: function ()
            {
                panelClosed = true;
            }
        });

        expect(panelClosed).toBeTruthy();
        expect(navigationPanelSelectionCleared).toBeTruthy();

    });
    it("close all existing panels on system change and notifies of system change", function ()
    {
        var splitPanesAreReset = false, p = mentor.publisher, fakeExistingSystemId;
        p.detailLayoutManager.reset();
        p.contentArea.setMainWindowLayoutManager({
            reset: function ()
            {
                splitPanesAreReset = true;
            }
        });
        p.contentArea.setSelectedSystem({
            get: function ()
            {
                return fakeExistingSystemId;
            }
        });
        fakeExistingSystemId = "openedSystemId";

        p.contentArea.closeAllSplitPanelsIfNewSystemIsOpened({
            systemId: fakeExistingSystemId
        });
        expect(splitPanesAreReset).toBeFalsy();

        //open report
        p.detailLayoutManager.relayout(p.contentType.SYSTEM_REPORT, "openedSystemId");

        //Now try to open a new system from xrefs
        p.contentArea.closeAllSplitPanelsIfNewSystemIsOpened({
            systemId: "newSystemId"
        });
        expect(splitPanesAreReset).toBeTruthy();

        expect(systemChanged).toBeTruthy();

    });
});
