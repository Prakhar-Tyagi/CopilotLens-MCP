/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
(function () {
    var Model = Backbone.Model.extend();
    var contentToTest;
    var getSystemsOriginal;
    var stubs = {
        "fileDisplayHandler": {
            display: function (content)
            {
                console.log('!!! fdh called');
                contentToTest = content;
            }
        },
        jquery : $,
        underscore : _,
        backbone : Backbone,
        PackagesInSession: new Backbone.Model(),
    };

    var context = createContext(stubs);

    // TODO: This Test affects the global fn 'mentor.publisher.project.getSystems' and affects other runs.
    context(['routers/systemRouter', 'fileDisplayHandler'], function(systemRouter, displayHandler) {

        xdescribe("SystemRouterTest", function() {
            beforeEach(function () {
                getSystemsOriginal = mentor.publisher.project.getSystems;
                mentor.publisher.project.getSystems = function () {
                    return [{
                        nameAttr: 'Create Topology',
                        getDiagrams: function () {
                            return [{
                                systemId: 'SystemID',
                                diagramId: 'DiagramID',
                                mainText: 'Diagram1'
                            }];
                        },
                        getFolders: function () {
                            return "";
                        }
                    }, {
                        nameAttr: 'Create Topology Plane',
                        getDiagrams: function () {
                            return [{
                                mainText: 'Diagram2'
                            }];
                        },
                        getFolders: function () {
                            return "";
                        }
                    }]
                };
            });

            xit("Should open the system with matching name", function() {
                var isWaiting = true;
                require(["systems"], function (systems) {
                    systems.fetch();
                    // console.log("systems.length: " + systems.length);
                    // console.log("systems: " + JSON.stringify(systems));
                });
                runs(function() {
                    systemRouter.openComponent({
                        parameters: {
                            system: "Create Topology",
                            component: 'Diagram1'
                        }
                    });
                    setTimeout(function() {
                        isWaiting = false;
                    }, 10);
                });

                waitsFor(function() {
                    return !isWaiting;
                }, 2000);

                runs(function() {
                    expect(contentToTest).toNotBe(undefined);
                    expect(contentToTest.type).toBe(mentor.publisher.contentType.SYSTEM_SVG);
                    expect(contentToTest.id).toBe("SystemID");
                    expect(contentToTest.diagramId).toBe("DiagramID");
                });
            });

            afterEach(function () {
                mentor.publisher.project.getSystems = getSystemsOriginal;
            });
        })
    }, function (err) {
        describe("System Router - module load Error", function ()
        {
            it("Module load failed", function ()
            {
                console.log(err.message + "::\n" + err.stack);
                expect(false).toBeTruthy();
            });
        });
    })
})();
