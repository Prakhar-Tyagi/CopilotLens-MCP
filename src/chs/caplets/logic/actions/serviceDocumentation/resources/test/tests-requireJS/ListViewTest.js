/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, window, require, describe, expect, it, listTemplateForTest,Backbone*/
require(["jquery", "ListView", "currentPackage"], function ($, ListView, currentPackage) {
    "use strict";
    describe("ListViewTest", function () {
        var createConatiner = function () {
            $("html").append("<div id='navigationPanel'></div>");
        }, destroyContainer = function () {
            $("#navigationPanel").remove();
        };

        var wires = new (Backbone.Collection.extend())(), wiresView;
        var custom = new (Backbone.Collection.extend())();

        var custom = new (Backbone.Collection.extend())()

        beforeEach(function () {
            $('body').append($("<div id='wires'></div>"));
            wires.getModels = function () {
                return wires.models;
            };
            wires.reset([
                {mainText : "wire1", id : "wire1"},
                {mainText : "wire1", id : "wire2"}
            ]);

            $('body').append($("<div id='custom'></div>"));
            custom.getModels = function () {
                return custom.models;
            };
            custom.reset([
                {mainText : "banana", id : "1"},
                {mainText : "apple", id : "2"},
                {mainText : "_orange", id : "3"},
            ]);
        });

        it("should be able to load ListView module", function () {
            createConatiner();
            expect(ListView).toBeDefined();
            destroyContainer();
        });
        it("should be able to load when list of items are supplied", function () {
            var listView = new (ListView(wires))();
            createConatiner();
            listView.templateHTML = listTemplateForTest;
            listView.container = "#wires";
            listView.render();
            expect($(listView.$el).html()).toBeTruthy();
            destroyContainer();
        });
        it("should be able to generate HTML using supplied template", function () {
            var listView = new (ListView(wires))();
            createConatiner();
            listView.templateHTML = listTemplateForTest;
            listView.container = "#wires";
            listView.render();
            expect($(".headingCountNumber", listView.$el).html()).toBe("2");
            destroyContainer();
        });

         it("should be able remove list items when removeItems is called", function () {
             var listView = new (ListView(wires))();
             createConatiner();
             listView.templateHTML = listTemplateForTest;
             listView.container = "#wires";
             listView.render();
             listView.removeItems();
             expect($(".headingCountNumber", listView.$el).length).toBe(0);
             destroyContainer();
         });

        it("should be able to sort the items in List view", function () {
            var listView = new (ListView(custom))();
            const mockGetData = {
                getModels: () => [
                    { get: () => 'apple' },
                    { get: () => '_orange' },
                    { get: () => 'banana' }
                ]
            };
            let capturedResult;
            const mockConfig = {
                success: (result) => {
                    capturedResult=result;
                    console.log("Mock success callback called with:", result);
                }
            };
            const context = {
                header: true,
                expanded: undefined,
                getData: () => mockGetData,
                getTitle: () => 'Test Title'
            };
            listView.fetchData(mockConfig);
            const sortedModels = [
                { get: () => '_orange' },
                { get: () => 'apple' },
                { get: () => 'banana' }
            ];
            console.log(capturedResult.items[0].get('mainText'));
            expect(capturedResult.items[0].get('mainText')).toBe(sortedModels[0].get());
            expect(capturedResult.items[1].get('mainText')).toBe(sortedModels[1].get());
            expect(capturedResult.items[2].get('mainText')).toBe(sortedModels[2].get());
        });

    });

});

