/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, �SISW�), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer�s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global require, describe, it, expect, beforeEach, Backbone, afterEach, createContext*/
(function () {
    "use strict";
    var allPackages = [
                {
                    attributes: {
                        id: "packages1",
                        name: "Assembly_BOM_Verification",
                        projectId: "PROJ_ID"
                    },
                    id: "packages1",
                    subPackages: {
                        models: [
                            {
                                attributes: {
                                    effectivityRange: "e1-e2",
                                    start: "1",
                                    end: "2",
                                    prefix: "e",
                                    projectId: "PROJ_ID"
                                }
                            },
                            {
                                attributes: {
                                    effectivityRange: "e3-e8",
                                    start: "3",
                                    end: "8",
                                    prefix: "e",
                                    projectId: "PROJ_ID"
                                }
                            },
                            {
                                attributes: {
                                    effectivityRange: "e9-e13",
                                    start: "9",
                                    end: "13",
                                    prefix: "e",
                                    projectId: "PROJ_ID"
                                }
                            }
                        ]
                    }
                }],
            stubs = {
                jquery: $,
                underscore: _,
                backbone: Backbone,
                Package: new Backbone.Model()
            };
    var context = createContext(stubs);
    context(['collections/Packages'], function (Packages) {
        var packages = new Packages();
        describe("packageCollectionTest", function () {
            it("should be able to load packages Module", function () {
                expect(packages).toBeDefined();
            });
        });

        // packages.models = allPackages;
        packages.getAllPackages = function () {
            return {models: allPackages}
        }

        describe("Test for findSubPackageByParams method", function () {
            it("Get all subPackets from packages", function () {
                var subPackets = packages.getAllSubPackages();
                expect(subPackets).toBeDefined();
                expect(subPackets.length).toBe(3)
            });

            it("If effRange is passed as a number,", async function () {
                var machingPacket = await packages.findSubPackageByParams({effRange: "4"});
                expect(machingPacket).toBeDefined();
                expect(machingPacket.start).toBeLessThan(4);
                expect(machingPacket.end).toBeGreaterThan(4);
            });
            it("If effRange is passed as a string,", async function () {
                var machingPacket = await packages.findSubPackageByParams({effRange: "e10"});
                expect(machingPacket).toBeDefined();
                expect(machingPacket.start).toBeLessThan(10);
                expect(machingPacket.end).toBeGreaterThan(10);
            });

            it("If effRange is passed as a range", async function () {
                var machingPacket = await packages.findSubPackageByParams({effRange: "e4-e6"});
                expect(machingPacket).toBeDefined();
                expect(machingPacket.start).toBeLessThan(4);
                expect(machingPacket.end).toBeGreaterThan(6);
            });

            it("If effRange spread over 2 effectivity packets", async function () {
                var machingPacket = await packages.findSubPackageByParams({effRange: "e4-e10"});
                expect(machingPacket).not.toBeDefined();
            });
        });

        describe("Test Packages Collection", function () {
            var models, packages;
            beforeEach(function () {
                models = [{id: "p1", name: "package1"}, {id: "p2", name: "package2"}, {id: "p3", name: "package3"}];

                packages = new Packages(models, {
                    model: Backbone.Model.extend({
                        defaults: {
                            name: "",
                            id: ""
                        }
                    })
                });
            });

            it("should warn when no matched package name found", function () {
                var refreshPageCalled = false;
                var origRefreshPage = packages.reloadBaseUrl;
                packages.reloadBaseUrl = function(){
                    refreshPageCalled = true;
                }
                spyOn(window, 'alert').andReturn(false);
                var matchingPacket = packages.findPackageByName("Assembly_BOM_Verification");

                expect(window.alert).toHaveBeenCalled();
                expect(matchingPacket).toBeFalsy();
                expect(refreshPageCalled).toBeTruthy();

                packages.reloadBaseUrl = origRefreshPage;
            });

            it("should return matched Package", function () {
                spyOn(window, 'alert');
                var matchingPacket = packages.findPackageByName("package1");

                expect(window.alert).not.toHaveBeenCalled();
                expect(matchingPacket).toBeDefined();
                expect(matchingPacket.get('id')).toBe("p1");
            });

            it("Packages not available warning", function () {
                spyOn(window, 'updateClientType');
                spyOn(window, 'showError');

                packages.parse("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                        "<packages></packages>");

                expect(window.updateClientType).toHaveBeenCalledWith("NoValidPacketAvailableMsg", "");
                expect(window.showError).toHaveBeenCalled();

            });
        });

    }, function (error) {
        describe("PackagesTest - Module Loading failed", function () {
            it("This expectation should not be called", function () {
                console.log("package Loading Error ", error);
                expect(error).not.toBeDefined();
            });
        });
    });
})();
