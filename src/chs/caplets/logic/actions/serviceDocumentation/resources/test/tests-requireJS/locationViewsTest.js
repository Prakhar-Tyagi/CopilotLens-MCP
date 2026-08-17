/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, window, require, describe*/
require(["LocationViews", "currentPackage"], function (locationViews, currentPackage) {
    window.mentor = {
        publisher : {
            dataLoader : {
                loadProject : function (id) {
                    return {
                        getDiagrams : function (type) {
                            return [
                                {mainText : "location1", subText : "subText123", getToolTips : function () {
                                        return [
                                            {
                                                getName : function () {
                                                    return "partNo";
                                                },
                                                getValue : function () {
                                                    return "partNo123";
                                                }

                                            }
                                        ];
                                    }},
                                {mainText : "location123", subText : "subText", getToolTips : function () {
                                        return [
                                            {
                                                getName : function () {
                                                    return "partNo";
                                                },
                                                getValue : function () {
                                                    return "partNo123";
                                                }

                                            }
                                        ];
                                    }},
                                {mainText : "location3", subText : "subText", getToolTips : function () {
                                        return [
                                            {
                                                getName : function () {
                                                    return "partNo";
                                                },
                                                getValue : function () {
                                                    return "partNo1234";
                                                }

                                            }
                                        ];
                                    }},
                                {mainText : "location4", subText : "subxyz", getToolTips : function () {
                                        return [
                                            {
                                                getName : function () {
                                                    return "partNo";
                                                },
                                                getValue : function () {
                                                    return "partNo123";
                                                }

                                            }
                                        ];
                                    }},
                                {mainText : "location5", subText : "subText", getToolTips : function () {
                                        return [
                                            {
                                                getName : function () {
                                                    return "partNo1234";
                                                },
                                                getValue : function () {
                                                    return "partNo12345";
                                                }

                                            }
                                        ];
                                    }}
                            ];
                        }
                    };
                }
            }
        }
    };
    //currentPackage.set("id", 'changedID');

    describe("LocationViewsTest", function () {
        beforeEach(function() {
            spyOn(currentPackage, 'get').andReturn('changedID');
        });

        it("should be able to load Location views collection module", function () {
            expect(locationViews).toBeDefined();
        });

        it("should be able to fetch Location views when current Project changes", function () {
            expect(locationViews.models.length).toBe(5);
        });
    });

});