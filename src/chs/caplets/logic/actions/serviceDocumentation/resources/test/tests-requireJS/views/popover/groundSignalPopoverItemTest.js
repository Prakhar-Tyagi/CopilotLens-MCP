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
    var mockModel = new (Backbone.Model.extend())(), context, stubs, xrefContent;

    stubs = {
        "PopoverItemView": Backbone.View,
        "collections/p/groundPathCollection": new Backbone.Collection(),
        "fileDisplayHandler": {
            display: function (content)
            {
                this.content = content;
            }
        },
        "currentPackage": mockModel,
    };
    context = createContext(stubs);

    context(['views/p/groundSignalPopoverItem'], function (item)
    {
        describe("groundSignalPopoverItemTest", function ()
        {
            beforeEach(function ()
            {

            });
            it("should be able to load groundSignalPopoverItem", function () {
                expect(item).toBeDefined();
            });

            it("should not show popout button", function () {
                expect(item.shouldShowPopup()).toBeFalsy();
            });

            it("should test getters", function () {
                expect(item.getTitle()).toBe("groundAndPowerSignalTitle");
                expect(item.getClassName()).toBe("groundAndPowerSignalTitle");
            });

            it("should be able to get Data", function ()
            {
                expect(item.getData()).toBe(stubs["collections/p/groundPathCollection"]);
            });

            it("should be able to get ItemContent", function ()
            {
                expect(item.getItemContent("tempId")).toBe(stubs["collections/p/groundPathCollection"].get("tempId"));
            });

            it("should be able to display content", function ( ) {
                var windowObj={
                        mentor: {
                            publisher: {
                                detailLayoutManager:{
                                    resetContentPanel: function () {}
                                },
                                eventDispatcher:{
                                    dispatchEvent: function () {}
                                },
                                events: {
                                    GROUND_PATH_TRACE: ''
                                }
                            }
                        }
                    },
                    origGetWindowObj=item.getWindowObj
                ;
                item.getWindowObj=function () {return windowObj};

                spyOn(windowObj.mentor.publisher.detailLayoutManager, "resetContentPanel");
                spyOn(windowObj.mentor.publisher.eventDispatcher, "dispatchEvent");
                item.displayContent({id: "testId"});
                expect(windowObj.mentor.publisher.detailLayoutManager.resetContentPanel).toHaveBeenCalled();
                expect(windowObj.mentor.publisher.eventDispatcher.dispatchEvent).toHaveBeenCalled();

                item.getWindowObj=origGetWindowObj;
            });

        });
    });
})();