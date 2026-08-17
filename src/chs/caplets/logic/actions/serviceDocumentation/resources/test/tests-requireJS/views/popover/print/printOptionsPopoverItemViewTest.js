/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, window, require, describe, expect, it, listTemplateForTest, Backbone, $, _, createContext*/
(function ()
{
    "use strict";
    var model = new (Backbone.Model.extend())(), context, stubs, xrefContent;

    stubs = {
        currentPackage: model,
        jquery: $,
        underscore: _,
        backbone: Backbone,
        PrintOptionsCollection: new (Backbone.Collection.extend())(),
        PopoverItemView: Backbone.View.extend()
    };
    context = createContext(stubs);

    context(['views/p/print/printOptionsPopoverItemView'], function (printOptionsPopoverItemView)
    {
        describe("printOptionsPopoverItemViewTest", function ()
        {
            beforeEach(function ()
            {
                $('body').append("<div id='testContainer'><div class='panel_content' id='id_to_print'>printcontent</div></div>")
            });
            it("should be able to load printOptionsPopoverItemView Module", function ()
            {
                expect(printOptionsPopoverItemView).toBeDefined();
            });

            it("should be able to get appropriate title, containerId, and className", function () {
                var origShouldShowNoOfPagesDropDown = printOptionsPopoverItemView.shouldShowNoOfPagesDropDown;
                printOptionsPopoverItemView.shouldShowNoOfPagesDropDown=function () {return true};
                var title = printOptionsPopoverItemView.getTitle(),
                    containerId = printOptionsPopoverItemView.getContainerIdToPrint(),
                    className=printOptionsPopoverItemView.getClassName()
                ;
                expect(title).toBe("");
                expect(containerId).toBeFalsy();
                expect(className).toBe("printWithNoOfPagesDropDown");
                printOptionsPopoverItemView.shouldShowNoOfPagesDropDown=origShouldShowNoOfPagesDropDown;
                className=printOptionsPopoverItemView.getClassName();
                expect(className).toBe("printOptions");
            });

            it("should be able to get expanded and should show popup states", function() {
                expect(printOptionsPopoverItemView.isExpanded()).toBeTruthy();
                expect(printOptionsPopoverItemView.shouldShowPopup()).toBeFalsy();
            });

            it("should be able to close the print popover", function () {
                var origCLOSE_POPOVER=mentor.publisher.events.CLOSE_POPOVER
                ;
                mentor.publisher.events.CLOSE_POPOVER='';

                spyOn(mentor.publisher.eventDispatcher, "dispatchEvent").andCallThrough();
                printOptionsPopoverItemView.closePrintPopover();
                expect(mentor.publisher.eventDispatcher.dispatchEvent).toHaveBeenCalled();

                mentor.publisher.events.CLOSE_POPOVER=origCLOSE_POPOVER;
            });

            it("should be able to get the data ID", function () {
                var evt, targetElem=$('<div  data-id="testId" /> ');
                evt={
                    currentTarget: targetElem,
                    detail: {},
                };
                expect(printOptionsPopoverItemView.getDataId(evt)).toBe("testId");
            });

            it("should extend PopoverItemView", function ()
            {
                expect(printOptionsPopoverItemView instanceof stubs.PopoverItemView).toBe(true);
            });

            it("should print first content when print is clicked", function ()
            {
                var printPopoverClosed, printFirst, prePrint;
                prePrint = printOptionsPopoverItemView.printFirstContent;
                printOptionsPopoverItemView.getDataId = function (event)
                {
                    return "dataId";
                };

                printOptionsPopoverItemView.closePrintPopover = function (event)
                {
                    printPopoverClosed = true;
                };

                printOptionsPopoverItemView.printFirstContent = function (event)
                {
                    printFirst = true;
                };
                model.set("id", mentor.publisher.constants.print);

                printOptionsPopoverItemView.getDataId = function (event)
                {
                    return "dataId";
                };
                stubs.PrintOptionsCollection.get = function ()
                {
                    return model;
                };
                printOptionsPopoverItemView.performPrintAction({
                    stopPropagation: function ()
                    {

                    }
                });
                printOptionsPopoverItemView.printFirstContent = prePrint;
                expect(printPopoverClosed).toBeTruthy();
                expect(printFirst).toBeTruthy();
                model.clear();

            });

            it("should show Print Selection Popover when print selection option is clicked", function ()
            {
                var printPopoverClosed, printSelection, preMethod = printOptionsPopoverItemView.showPrintSelectionPopover;
                printOptionsPopoverItemView.getDataId = function (event)
                {
                    return "dataId";
                };

                printOptionsPopoverItemView.closePrintPopover = function (event)
                {
                    printPopoverClosed = true;
                };

                printOptionsPopoverItemView.showPrintSelectionPopover = function (event)
                {
                    printSelection = true;
                };
                model.set("id", mentor.publisher.constants.printSelection);

                printOptionsPopoverItemView.getDataId = function (event)
                {
                    return "dataId";
                };
                stubs.PrintOptionsCollection.get = function ()
                {
                    return model;
                };
                printOptionsPopoverItemView.performPrintAction({
                    stopPropagation: function ()
                    {

                    }
                });
                printOptionsPopoverItemView.showPrintSelectionPopover = preMethod;
                expect(printPopoverClosed).toBeTruthy();
                expect(printSelection).toBeTruthy();

            });

            it("should print first content from open spilt panels", function ()
            {
                var previousMethod = mentor.publisher.contentArea, printSelection, content, spy = mentor.publisher.printer;
                mentor.publisher.printer = {
                    initiatePrinting: function (contentToPrint)
                    {
                        content = contentToPrint;
                    }
                };

                mentor.publisher.contentArea = {
                    getAllOpenContentDetails: function ()
                    {
                        var obj = {};
                        obj.SVGPanel = {};
                        obj.twoDPanel = {};
                        return obj;
                    }
                };
                printOptionsPopoverItemView.printFirstContent(1, "testContainer");

                expect(JSON.stringify(content)).toBe('["id_to_print"]');

                mentor.publisher.contentArea = previousMethod;
                mentor.publisher.printer = spy;

            });

            it("should show print selection popover", function ()
            {

                var spy = mentor.publisher.printer, event = {stopPropagation: function ()
                {
                    this.stop = true;
                }};

                mentor.publisher.printer = {
                    printSelectionClickHandler: function ()
                    {
                        this.called = true;
                    }
                };
                printOptionsPopoverItemView.showPrintSelectionPopover(event);
                expect(event.stop).toBeTruthy();
                expect(mentor.publisher.printer.called).toBeTruthy();

                mentor.publisher.printer = spy;
            });

        });
    });
})();
