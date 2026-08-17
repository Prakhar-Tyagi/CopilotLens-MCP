/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, window, require, describe, expect, it, listTemplateForTest*/
(function ()
{
    "use strict";
    var mockPack = new (Backbone.Model.extend())(), context, stubs, threeDContent;

    stubs = {
        currentPackage: mockPack,
        jquery: $,
        underscore: _,
        backbone: Backbone,
        ThreeDViewCollection: new (Backbone.Collection.extend())(),
        fileDisplayHandler: {
            display: function (content)
            {
                threeDContent = content;
            }
        }
    };
    context = createContext(stubs);

    context(['views/p/threeDviewView'], function (threeDview)
    {
        describe("threeDviewViewTest", function ()
        {

            beforeEach(function ()
            {
                stubs.currentPackage.set("id", "projectId");
                $('body').append($('<div class="threeDLocations"><div class="listItem"></div> </div>'));

            });

            it("should be able to load threeDview Module", function ()
            {
                expect(threeDview).toBeDefined();
            });

            it("it should be able to open a threeDview  in popout", function ()
            {
                var three = new (Backbone.Model.extend())(), popoutURL;
                three.set({mainText: "threeDFileName", path: "filePath", type:"ThreeDXML",objectId:"objId1"});
                popoutURL = threeDview.createURL(three);
                expect(popoutURL).toBe("popout.html#/threeDXML/threeDFileName/projectId/ThreeDXML/objId1/filePath");

                three = new (Backbone.Model.extend())(), popoutURL;
                three.set({mainText: "threeDFileName", path: "filePath", type:"JT",objectId:"objId2"});
                popoutURL = threeDview.createURL(three);
                expect(popoutURL).toBe("popout.html#/threeDXML/threeDFileName/projectId/JT/objId2/filePath");
            });

            it("should be able to display a threeDview", function ()
            {
                var displayedContent, three = new (Backbone.Model.extend())(), popoutURL;
                three.set({mainText: "threeDFileName", path: "filePath"});

                stubs.ThreeDViewCollection.get = function ()
                {
                    return three;
                };
                displayedContent = threeDview.getItemContent("threeDFileName");
                expect(JSON.stringify(displayedContent)).toBe('{"mainText":"threeDFileName","path":"filePath"}');

            });

            it("should be able to click the three d view", function ()
            {
                var displayedContent, three = new (Backbone.Model.extend())(), popoutURL;
                three.set({mainText: "threeDFileName", path: "filePath"});

                stubs.ThreeDViewCollection.get = function ()
                {
                    return three;
                };
                var itemclicked = false;
                threeDview.popoverItemClicked = function () {
                    itemclicked = true;
                }
                threeDview.setElement("body");
                $(".listItem").trigger("click");

                expect(itemclicked).toBe(true);
            });

            it("should return the Title", function () {
                expect(threeDview.getTitle()).toBe("LocationViewTitle");
            });

            it("should return the ClassName", function () {
                expect(threeDview.getClassName()).toBe("threeDLocations");
            });

            afterEach(function(){
                 $(".threeDLocations").remove();
            });
        });
    });
})();