/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, window, require, describe*/
require(["Harnesses", "currentPackage"], function (harnessCollection, currentPackage) {
    window.mentor = {
        publisher : {
            dataLoader : {
                loadProject : function (id) {
                    return {
                        getReports : function (type) {
                            return [
                                {mainText : "Report1", subText : "subText123", getToolTips : function () {
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
                                {mainText : "Report123", subText : "subText", getToolTips : function () {
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
                                {mainText : "Report3", subText : "subText", getToolTips : function () {
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
                                {mainText : "Report4", subText : "subxyz", getToolTips : function () {
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
                                {mainText : "Report5", subText : "subText", getToolTips : function () {
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

    describe("HarnessesTest", function () {
        beforeEach(function() {
            spyOn(currentPackage, 'get').andReturn('changedID');
        });

        it("should be able to load Harnesses collection module", function () {
            expect(harnessCollection).toBeDefined();
        });

        it("should be able to fetch Harnesses when current Project changes", function () {
            expect(harnessCollection.models.length).toBe(5);
        });
    });

});