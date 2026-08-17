/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global require, describe, it, expect, beforeEach, afterEach, mentor, $*/
var listViewTest = function (viewUnderTest, viewCollection, viewTemplate, fileDisplayHandler) {
    return function () {
        var oldGetObjects;

        beforeEach(function () {
            $('body').append("<div id='collections'></div>");
            viewUnderTest.container = "#collections";
            viewUnderTest.templateHTML = viewTemplate;

          /*  mentor.publisher.project.getByType = function () {
                return [];
            }*/
        });
        it("should be able to load systemsView Module", function () {
            expect(viewUnderTest).toBeDefined();
        });

        it("should render system view when systems collection changes", function () {
            // viewUnderTest.setElement(viewUnderTest.container);
            expect(viewUnderTest.$el.html()).toBeFalsy();

            runs(function () {
                viewCollection.fetch();
            }, "Render view");

            waitsFor(function () {
                return viewUnderTest.$el.html() !== '';
            }, "wait for view to render", 5000);

            runs(function () {
                var noOfVisibleItems = $(".headingCountNumber", viewUnderTest.$el).html();
                expect(noOfVisibleItems,
                    "View list should render only one item as collection contains on item.").toBe('1');
                expect($(".listItem", viewUnderTest.$el).css("display"),
                    "by default all the list item input a list should be hidden").toBe('none');
                $(".titlebar", viewUnderTest.$el).trigger("click");
                expect(viewUnderTest.expanded).toBeTruthy();
            }, "execute assert condition");
        });

        it("should display the selected item when it is clicked", function () {
            runs(function () {
                viewCollection.fetch();
            }, "Render view");

            waitsFor(function () {
                return viewUnderTest.$el.html() !== '';
            }, "wait for view to render", 5000);

            $(".listItem", viewUnderTest.$el).trigger("click");
        });

        xit("should be able to pop out", function () {
            var event={
                stopPropagation: function () {},
            };
            spyOn(event, "stopPropagation");
            viewUnderTest.popOut(event);
            expect(event.stopPropagation).toHaveBeenCalled();
        });

        xit("should be able to hide and collapse all", function () {
            var event={
                currentTarget: $('<div collapseAll/>')
            };
            viewUnderTest.hideCollapseAll(event);
            viewUnderTest.showCollapseAll(event);
        });

        xit("should be able to hide and collapse all", function () {
            var event={
                currentTarget: $('<div highlight/>'),
                stopPropagation: function () {}
            };
            spyOn(event, "stopPropagation");
            viewUnderTest.listItemClicked(event);
            expect(event.stopPropagation).toHaveBeenCalled();
        });

        afterEach(function () {
            $('#collections').html('');
        });

    };
};
/*  });
 */