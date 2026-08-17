/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

(function() {
    var systemUnderTest = new BaseInteractiveBuilder();
    var testMixedConfigurationData = [{
        name: "ABC",
        isLocal: true
    }, {
        name: "abc 2",
        isLocal: true
    }, {
        name: "3 DOOR RHD"
    }, {
        name: "5 DOOR LHD"
    }, {
        name: "zzz",
        isLocal: true
    }, {
        name: "Alpha 1",
        isLocal: true
    }, {
        name: "123",
        isLocal: true
    }, {
        name: "1234",
    }, {
        name: "Alpha 2"
    }];

    var testLocalConfigurationData = [{
        name: "ABC",
        isLocal: true
    }, {
        name: "abc 2",
        isLocal: true
    }, {
        name: "zzz",
        isLocal: true
    }, {
        name: "Alpha 1",
        isLocal: true
    }, {
        name: "123",
        isLocal: true
    }];

    describe("BaseInteractiveBuilderTest", function () {
       it("should sort configurations in case-insensitive manner", function() {
            var sortConfigurations = systemUnderTest.sortConfigurations(testLocalConfigurationData);
            var sortedConfigsAsString = sortConfigurations.map(function(config) {
                return config.name;
            }).reduce(function(acc, currentVal) {
                acc = acc ? acc + "," : "";
                return acc + currentVal;
            }, "");
            expect(sortedConfigsAsString).toBe("123,ABC,abc 2,Alpha 1,zzz");
       });

        it("should order all local configurations first while sorting", function() {
            var sortConfigurations = systemUnderTest.sortConfigurations(testMixedConfigurationData);
            var sortedConfigsAsString = sortConfigurations.map(function(config) {
                return config.name;
            }).reduce(function(acc, currentVal) {
                acc = acc ? acc + "," : "";
                return acc + currentVal;
            }, "");
            expect(sortedConfigsAsString).toBe("123,ABC,abc 2,Alpha 1,zzz,1234,3 DOOR RHD,5 DOOR LHD,Alpha 2");
        });

        it("should be able to get nested inclusive options", function() {
            expect(systemUnderTest.getNestedInclusiveOptions({}, [])).toEqual([]);
        });

        it("should be able to reset panel", function() {
            systemUnderTest.vehicleConfigObj = {
                setMatchedConfigurations: function () {},
            };
            spyOn(systemUnderTest.vehicleConfigObj, 'setMatchedConfigurations');
            systemUnderTest.resetPanel();
            expect(systemUnderTest.vehicleConfigObj.setMatchedConfigurations).toHaveBeenCalled();
        });

        it("should be able to process selected options", function() {
            var event = {
                currentTarget: "currentTarget",
            };
            systemUnderTest.vehicleConfigObj = {
                findSuperSetConfigNames: function () {},
                getMatchedConfigurations: function () {return []},
                getOptionsUsedInPredefinedConfiguration: function () {return []},
                setMatchedConfigurations: function () {},
                getCurrentSelectedOptions: function () {return []},
            };
            spyOn(systemUnderTest.vehicleConfigObj, 'getCurrentSelectedOptions').andCallThrough();
            systemUnderTest.optionSelected(event);
            expect(systemUnderTest.vehicleConfigObj.getCurrentSelectedOptions).toHaveBeenCalled();
        });
    });
})();