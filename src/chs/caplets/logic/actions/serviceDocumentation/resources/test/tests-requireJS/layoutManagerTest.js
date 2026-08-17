/**
 * Created with IntelliJ IDEA.
 * User: mukumar
 * Date: 17/10/12
 * Time: 10:45 AM
 * To change this template use File | Settings | File Templates.
 */
/* globals describe, it, beforeEach, afterEach, expect, initResizebars, hResizeBarMouseDown, hasSVGPath, hasSVGType,
 window, mentor, $ */
describe("layoutManagerTest2", function ()
{
    "use strict";
    beforeEach(function ()
    {
        var html = "<div id='detail'><div id='splitter1' style='height: 200px'><div id='svg' class='detailContent'></div></div><div id='verticalResizebar' class='verticalResizebar' style='display: block; height: 473px;'></div><div id='splitter2'><div id='diagram" +
                "' class='detailContent'></div></div><div id='horizontalResizebar' class='horizontalResizebar' style='display: block;'></div><div id='splitter3' style='display: block; height: 473px;'><div id='customFile' class='detailContent'></div></div>";

        var outerHTML = '<div id="detailNavigationResizeBar" class="iesdResizeBar"></div>';

        var bodyEle = $('body');
        $(bodyEle).html(html);
        $(bodyEle).append($(outerHTML));
        initResizebars();
        window.resizeVinFilterBox = function ()
        {

        };
    });
    afterEach(function ()
    {
        $("#detail").remove();
        $('body').html('');
    });

    /*    it("test layout maanager should layout harness diagrams in 2 spit pane.", function ()
     {
     var id = mentor.publisher.detailLayoutManager.layout(mentor.publisher.contentType.HARNESS_LAYOUT_DIAGRAM, "");
     expect(id).toBe("diagram");
     });*/
    it("test horizontal and vertical resize bars should resize detail panel.", function ()
    {
        var horToolBar = $('#horizontalResizebar'), divForResize;
        $(horToolBar).on("click", function (event)
        {
            hResizeBarMouseDown(event);
            divForResize = $('#divForReisze');
            $(divForResize).trigger("mousemove");
            $(divForResize).trigger("mouseup");

        });

        $(horToolBar).trigger("click");
        $(horToolBar).trigger("mouseup");

        expect($("#splitter3").height()).toBe(473);
    });

    it("test maximise panel hides all panel except one.", function ()
    {
        var origGetDimensions = mentor.publisher.contentPanel.getDimensions;
        mentor.publisher.contentPanel.getDimensions=function () {return {width: 100, height: 100};};
        mentor.publisher.detailLayoutManager.reset();
        mentor.publisher.detailLayoutManager.relayout(mentor.publisher.contentType.SYSTEM_SVG, "systemid");
        mentor.publisher.detailLayoutManager.relayout(mentor.publisher.contentType.CUSTOM_VIEW, "systemid");
        mentor.publisher.detailLayoutManager.relayout(mentor.publisher.contentType.SYSTEM_REPORT, "systemid");

        mentor.publisher.detailLayoutManager.maximizePanel('splitter1');
        var splitter2 = $("#splitter2").css("height");
        var splitter3 = $("#splitter3").css("height");
        expect(splitter2).toBe('0px');
        expect(splitter3).toBe('0px');
        // TODO: disabling part of this test which tests that restoring a maximized panel, restore previously opened panels too.
        /*
        mentor.publisher.detailLayoutManager.restorePanel('splitter1');
        splitter2 = $("#splitter2").css("display");
        splitter3 = $("#splitter3").css("display");
        expect(splitter3).toBe('block');
        expect(splitter2).toBe('block');
        */
        mentor.publisher.contentPanel.getDimensions = origGetDimensions;
    });

    //TODO: this has conflict with some global state and fails when run together with other tests. otherwise it passes
    // This test has varible conflicts with contentPanel.js. When Content panel is loaded, it sets the layout manager
    // to 's\illustrator\views\layoutManager.js' which changes the map(mapOfTypes)
    // This is due to the fact that we are running change illustrator UTs and SmartClient UTs in the same env.
    // disabling the UT for now.
    it("should be able to close panels.", function ()
    {
        var origGetDimensions = mentor.publisher.contentPanel.getDimensions;
        mentor.publisher.contentPanel.getDimensions=function () {return {width: 100, height: 100};};
        mentor.publisher.detailLayoutManager.relayout(mentor.publisher.contentType.SYSTEM_SVG, "systemid");
        mentor.publisher.detailLayoutManager.relayout(mentor.publisher.contentType.SYSTEM_REPORT, "systemid");

        mentor.publisher.detailLayoutManager.enableMaximizeAndCloseBtns(false);
        mentor.publisher.detailLayoutManager.relayout(mentor.publisher.contentType.CUSTOM_VIEW, "systemid");
        var splitter1 = $("#splitter1").css("display");
        expect(splitter1).toBe('block');
        mentor.publisher.detailLayoutManager.close(mentor.publisher.contentType.SYSTEM_SVG);
        var splitter1 = $("#splitter1").css("display");
        expect(splitter1).toBe('none');

        mentor.publisher.detailLayoutManager.close(mentor.publisher.contentType.SYSTEM_REPORT);

        splitter1 = $("#splitter3").css("display");
        expect(splitter1).toBe('none');

        mentor.publisher.detailLayoutManager.reset();

        expect(mentor.publisher.detailLayoutManager.isContentActive(
                mentor.publisher.contentType.CUSTOM_VIEW)).toBeUndefined();

        mentor.publisher.contentPanel.getDimensions = origGetDimensions;
    });

    it('should accept content panels with incorrect path types', function ()
    {
        var hasSVGPath = window.hasSVGPath({
            path: {}
        });
        expect(hasSVGPath).toBeFalsy();
    });

    it('should accept content panels with incorrect type', function ()
    {
        var hasSVGType = window.hasSVGType({
            type: {}
        });
        expect(hasSVGType).toBeFalsy();
    });

    it('layout manager should be able to resize navigation bar correctly', function ()
    {
        $("body").append("<div id='navigation' style='height: 100px'></div>").append(
                "<div id='platformToolbar' style='height: 10px'></div>" +
                "<div style='height: 10px' id='vinSearchToolbar'></div>" +
                "<div id='platform-grouped-list'></div>" +
                "<div id='navigationBottomToolbar' style='height: 10px'></div>");
        resizeNavContentBar();
        expect($("#platform-grouped-list").height()).toBe(70);
    });
});

