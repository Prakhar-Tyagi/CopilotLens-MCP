/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global describe, it, expect, mentor*/
describe("vinFilterTest", function () {
    "use strict";
    var vinFilter = mentor.publisher.VINFilteredProject, project = {
        getSystems : function () {
            return [
                {
                    getOptionExpression : function () {
                        return "op1 || op2";
                    },
                    getDiagrams : function () {
                        return [
                            {
                                getOptionExpression : function () {
                                    return "op1";
                                }
                            },
                            {
                                getOptionExpression : function () {
                                    return "op2";
                                }
                            }
                        ];
                    },
                    getReports : function () {
                        return [
                            {
                                getOptionExpression : function () {
                                    return "op1";
                                }
                            },
                            {
                                getOptionExpression : function () {
                                    return "op2";
                                }
                            }
                        ];
                    }
                },
                {
                    getOptionExpression : function () {
                        return "op2";
                    }
                }
            ];
        },
        getObjects : function () {
            return [
                {
                    getOptionExpression : function () {
                        return "op2";
                    }
                },
                {
                    getOptionExpression : function () {
                        return "op1";
                    }
                },
                {
                    getOptionExpression : function () {
                        return "op1 || op2";
                    }
                }
            ];
        },
        getFirstSection : function () {
            return {listItems : function () {
                return [
                    {
                        getOptionExpression : function () {
                            return "op2";
                        }
                    },
                    {
                        getOptionExpression : function () {
                            return "op1";
                        }
                    },
                    {
                        getOptionExpression : function () {
                            return "op1 || op2";
                        }
                    }
                ];
            }};
        },
        getByType : function () {
            return [
                {
                    getOptionExpression : function () {
                        return "op2";
                    }
                },
                {
                    getOptionExpression : function () {
                        return "op1";
                    }
                },
                {
                    getOptionExpression : function () {
                        return "op1 || op2";
                    }
                }
            ];
        }
    };
    vinFilter.setVINOptions("op1");
    vinFilter.setProject(project);
    it("should be able to load the vinFilterProject successfully", function () {
        expect(mentor.publisher.VINFilteredProject).toBeDefined();
    });

    it("should be able to filter systems correctly when vin options are set", function () {
        var filteredSystems;
        filteredSystems = vinFilter.getSystems();
        expect(filteredSystems.length).toBe(1);
    });

    it("should be able to filter diagram and reports of a system correctly when vin options are set", function () {
        var filteredSystems;
        filteredSystems = vinFilter.getSystems();
        expect(filteredSystems[0].getDiagrams().length).toBe(1);
        expect(filteredSystems[0].getReports().length).toBe(1);
    });

    it("should be able to filter objects", function () {
        var filterObjects;
        filterObjects = vinFilter.getObjects();
        expect(filterObjects.length).toBe(2);
    });

    it("should be able to filter firstSection", function () {
        var filterObjects;
        filterObjects = vinFilter.getFirstSection();
        expect(filterObjects.listItems().length).toBe(2);
    });

    it("should be able to filter objects returned by getByType", function () {
        var filterObjects;
        filterObjects = vinFilter.getByType("systems");
        expect(filterObjects.length).toBe(2);
    });
});
