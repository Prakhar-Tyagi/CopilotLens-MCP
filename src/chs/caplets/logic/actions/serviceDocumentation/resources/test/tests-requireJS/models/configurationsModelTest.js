/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, ?SISW?), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer?s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/* global Backbone, createContext, $, _, Backbone */
(function () {
    "use strict";

    var context, stubs, vehicleConfiguration;
    var localStorage = window.localStorage;

    stubs = {
        jquery: $,
        underscore: _,
        backbone: Backbone
    };

    context = createContext(stubs);

    context(["ConfigurationsModel", "LocalConfigurations", "currentPackage", "ConfigurationsCollection"],
            function (configModel, LocalConfigurations, currentPackage, ConfigCollection) {
                describe("ConfigurationsModelTest", function () {
                    var origConfigMgr;
                    var PUBLISHER = mentor.publisher;

                    beforeEach(function() {

                        // currentPackage.set("id", "P1245", {silent: true});
                        localStorage && localStorage.clear();
                    });

                    afterEach(function() {
                    });

                    it("create a config model", function () {
                        var callBackCalled = false;
                        function testableCallBack() {
                            callBackCalled = true;
                        };

                        var customEvent1 = createDummyEvent("dummyEvent1", 12, 50);
                        var model = configModel.createModel(customEvent1);
                        model.createPanelGUI(testableCallBack());

                        expect(model).toBeDefined();
                        expect(model.clickedFromWhere()).toBe("InteractiveButtonFromVIN");
                        expect(callBackCalled).toBe(true);
                    });

                    it("should be able to show config Save panel", function () {
                        expect(configModel.shouldShowConfigSavePanel()).toBe(true);
                    });

                    it("invoking close resets VINFilter and configFilter", function () {
                        spyOn(window, "interactiveBuilderXButtonClicked").andCallThrough();
                        // TODO: when not spied on, call to resetVinFilter causes error
                        // TypeError: mentor.publisher.filter.reset not being defined, Not sure where this is modified globally
                        // Following tests modify the mentor.publisher.filter but reset it back.
                        // 1. popoverModelTest
                        // 2. PrintTest
                        // 3. harnessReportPanelTest
                        // 4. systemsViewTest
                        spyOn(window, "resetVINFilter");
                        spyOn(window, "resetConfigurationFilter");
                        configModel.close();
                        expect(interactiveBuilderXButtonClicked).toHaveBeenCalled();
                        expect(resetVINFilter).toHaveBeenCalled();
                        expect(resetConfigurationFilter).toHaveBeenCalled();
                    });

                    it("calling fetch should set the models correctly", function () {
                        spyOn(mentor.publisher.xmlLoader, "loadXMLByAjax").andReturn({
                            data: createTestableConfigDoc([TEST_CONFIG1, TEST_OPTION1])
                        });
                        var isWaiting = true;
                        var customEvent1 = createDummyEvent("evt101", 37, 55);
                        runs(function() {
                            configModel.fetch(customEvent1);
                            setTimeout(function() {
                                isWaiting = false;
                            }, 1000);
                        });

                        waitsFor(function() {
                            return !isWaiting;
                        }, 2000);

                        runs(function() {
                            var configCount = configModel.getConfigurationsCount();
                            expect(configCount).toBe(1);
                            done();
                        }, 100); // Adjust timeout as necessary
                    });


                    it("Config names are generated with next index if exist with same name", function () {
                        spyOn(mentor.publisher.xmlLoader, "loadXMLByAjax").andReturn({
                            data: createTestableConfigDoc([TEST_CONFIG1, TEST_CONFIG2, TEST_OPTION1])
                        });
                        var isWaiting = true;
                        var customEvent1 = createDummyEvent("evt101", 37, 55);
                        runs(function() {
                            configModel.fetch(customEvent1);
                            setTimeout(function() {
                                isWaiting = false;
                            }, 1000);
                        });

                        waitsFor(function() {
                            return !isWaiting;
                        }, 2000);

                        runs(function() {
                            var generateConfigName = configModel.generateConfigName();
                            //As part of translation, this title is localized and since translation isn't available, it return CustomCOnfiguration
                            expect(generateConfigName).toBe("CustomConfiguration");
                        });
                    });

                    it("Adding and deleting local configurations", function () {
                        spyOn(mentor.publisher.xmlLoader, "loadXMLByAjax").andReturn({
                            data: createTestableConfigDoc([TEST_CONFIG1, TEST_CONFIG2, TEST_OPTION1])
                        });

                        var isWaiting = true;
                        var origConfigCount
                        var customEvent1 = createDummyEvent("evt101", 37, 55);

                        runs(function() {
                            configModel.fetch(customEvent1);
                            setTimeout(function() {
                                isWaiting = false;
                            }, 1000);
                        });

                        waitsFor(function() {
                            return !isWaiting;
                        }, 2000);

                        runs(function() {
                            origConfigCount = configModel.getConfigurationsCount();
                            // Should we keep this below line or not
                            // expect(origConfigCount).toBe(1);

                            // Why do I need updateModel call here?? - This is a hack for test to pass.
                            // TODO: DOCS-10063: SmartClient - Fix configuration initialization logic
                            configModel.updateModel({
                                currentTarget: {
                                    getAttribute: function() {
                                        return "";
                                    }
                                }
                            });

                            configModel.createNewModel({
                                type: 'configuration',
                                name: "Local config 2",
                                value: "5 Door, LHD",
                                isLocal: true
                            });

                            isWaiting = true;
                            setTimeout(function() {
                                isWaiting = false;
                            }, 1000);
                        });

                        waitsFor(function() {
                            return !isWaiting;
                        }, 2000);

                        runs(function() {
                            var newConfigCount = configModel.getConfigurationsCount();
                            expect(newConfigCount).toBe(origConfigCount + 1);

                            var newConfig = configModel.checkIfNameAlreadyExist("Local config 2");
                            expect(newConfig.name).toBe("Local config 2");
                            expect(newConfig.value).toBe("5 Door, LHD");

                            var isDeleted = configModel.delete("Local config 2");
                            expect(isDeleted).toBe(true);

                            var configAfterDelete = configModel.checkIfNameAlreadyExist("Local config 2");
                            expect(configAfterDelete).toBeUndefined();
                            expect(configModel.getConfigurationsCount()).toBe(origConfigCount);
                        });
                    });

                    it("rollbackLastSelectedOptions invokes getConfigurationsModel", function () {
                        var customEvent1 = createDummyEvent("evt123", 12, 50);
                        var model = configModel.createModel(customEvent1);
                        spyOn(model, "getConfigurationsModel").andCallThrough();

                        configModel.rollbackToLastAppliedOptions();

                        var configurationsModel = model.getConfigurationsModel();
                        expect(model.getConfigurationsModel).toHaveBeenCalled();
                    });

                    it("reset calls fetch", function () {
                        var customEvent1 = createDummyEvent("evt102", 45, 60);
                        var model = configModel.createModel(customEvent1);
                        spyOn(ConfigCollection, "fetch").andCallThrough();

                        configModel.reset();

                        expect(ConfigCollection.fetch).toHaveBeenCalled();
                    });
                });
            }, function (err)
            {
                describe("ConfigurationsModelTest - module load Error", function ()
                {
                    it("Module load failed", function ()
                    {
                        console.log(err);
                        expect(false).toBeTruthy();
                    });
                });
            });

    // Test support code
    function createDummyEvent(eventName, clientX, clientY, detail)
    {
        var event = new CustomEvent("ConfigurationsModelTest_" + eventName, {
            detail: detail || {}
        });
        // TODO: this is hack for testing. How do we set clientX and clientY on custom event??
        event.clientX = clientX;
        event.clientY = clientY;
        return event;
    }

    function createElement(eleName, attributes)
    {
        var element = document.createElementNS('http://www.w3.org/1999/xhtml', eleName);
        _.each(attributes, function(attribute) {
            element.setAttribute(attribute.name, attribute.value)
        })
        return element;
    }

    function createTestableConfigDoc(entities)
    {
        var doc = document.implementation.createDocument('http://www.w3.org/1999/xhtml', 'configurations', null);
        entities.forEach(function(ele) {
            var entity = createElement(ele.type, ele.attributes);
            doc.documentElement.appendChild(entity);
        });

        return doc;
    }

    var TEST_CONFIG1 = {
        type: 'configuration',
        attributes: [{
            name: "name",
            value: "3 Door Left Hand Drive - Basic"
        }, {
            name: "options",
            value: "3Door, Audio-Base, FuelLid, LHD, Liftgate, PwrMirrors"
        }]
    };

    var TEST_CONFIG2 = {
        type: 'configuration',
        attributes: [{
            name: "name",
            value: "Custom Configuration"
        }, {
            name: "options",
            value: "3Door, LHD, PwrMirrors"
        }]
    };

    var TEST_OPTION1 = {
        type: 'option',
        attributes: [{
            name: "name",
            value: "3Door"
        }, {
            name: "variant",
            value: "true"
        }, {
            name: "desc",
            value: "Three doors"
        }, {
            name: "inclusiveOptions",
            value: ""
        }, {
            name: "exclusiveOptions",
            value: ""
        }]
    };
    vehicleConfiguration = '<root><configurations>' +
            '<configuration name="3 Door Left Hand Drive - Basic" options="3Door, Audio-Base, FuelLid, LHD, Liftgate, PwrMirrors"/>' +
            '<configuration name="3 Door Left Hand Drive - Premium" options="3Door, Airbags, Audio-Base, CDPlayer, FuelLid, LHD, Liftgate, PassAirbag, PwrMirrors"/>' +
            '<configuration name="3 Door Right Hand Drive - Basic" options="3Door, Airbags, Audio-Base, FogLamp-Rr, PassAirbag, RHD"/>' +
            '<configuration name="3 Door Right Hand Drive - Mid" options="3Door, Airbags, Audio-Mid, CDPlayer, DoorLock-Drvr, DoorLock-Pass, FogLamp-Rr, PassAirbag, PwrMirrors, PwrWindows, RHD"/>' +
            '<configuration name="3 Door Right Hand Drive - Premium" options="3Door, Airbags, Audio-Mid, CDPlayer, DoorLock-Drvr, DoorLock-Pass, FogLamp-Ft, FogLamp-Rr, FuelLid, IllumEntry, Liftgate, PassAirbag, PwrMirrors, PwrWindows, RHD, RearJack, Subwoofer"/>' +
            '<configuration name="5 Door Left Hand Drive - Basic" options="5Door, Airbags, Audio-Mid, CDPlayer, DoorLock-Drvr, DoorLock-Pass, FogLamp-Ft, FuelLid, IllumEntry, LHD, Liftgate, PassAirbag, PwrMirrors, PwrWindows, RearJack, Subwoofer"/>' +
            '<configuration name="5 Door Left Hand Drive - Premium" options="5Door, Airbags, Audio-High, CDChanger, DoorLock-Drvr, DoorLock-Pass, FogLamp-Ft, FogLamp-Rr, FuelLid, IllumEntry, LHD, Liftgate, PassAirbag, PwrMirrors, PwrWindows, RearJack, Subwoofer"/>' +
            '<configuration name="5 Door Right Hand Drive - Basic" options="5Door, Airbags, Audio-Mid, CDPlayer, DoorLock-Drvr, DoorLock-Pass, FogLamp-Ft, FogLamp-Rr, FuelLid, IllumEntry, Liftgate, PassAirbag, PwrMirrors, PwrWindows, RHD, RearJack, Subwoofer"/>' +
            '<configuration name="5 Door Right Hand Drive - Premium" options="5Door, Airbags, Audio-High, CDChanger, DoorLock-Drvr, DoorLock-Pass, FogLamp-Ft, FogLamp-Rr, FuelLid, IllumEntry, Liftgate, PassAirbag, PwrMirrors, PwrWindows, RHD, RearJack, Subwoofer"/>' +
            '<option name="3Door" variant="true" desc="Three doors" inclusiveOptions="" exclusiveOptions=""/>' +
            '<option name="5Door" variant="true" desc="Five doors" inclusiveOptions="" exclusiveOptions=""/>' +
            '<option name="Airbags" variant="false" desc="Airbag system w/ driver\'s airbag" inclusiveOptions="" exclusiveOptions=""/>' +
            '<option name="Audio-Base" variant="false" desc="Base audio system w/ 2 speakers" inclusiveOptions="" exclusiveOptions=""/>' +
            '<option name="Audio-High" variant="false" desc="High line audio system w/ 6 speakers" inclusiveOptions="" exclusiveOptions=""/>' +
            '<option name="Audio-Mid" variant="false" desc="Mid line audio system w/ 4 speakers" inclusiveOptions="" exclusiveOptions=""/>' +
            '<option name="BLIS" variant="false" desc="Blind Spot Detection" inclusiveOptions="" exclusiveOptions=""/>' +
            '<option name="CDChanger" variant="false" desc="6-Disc Changer" inclusiveOptions="" exclusiveOptions=""/>' +
            '<option name="CDPlayer" variant="false" desc="Single disc in-dash player" inclusiveOptions="" exclusiveOptions=""/>' +
            '<option name="DoorLock-Drvr" variant="false" desc="Driver Door Lock" inclusiveOptions="DoorLock-Pass" exclusiveOptions=""/>' +
            '<option name="DoorLock-Pass" variant="false" desc="Passenger Door Lock" inclusiveOptions="" exclusiveOptions=""/>' +
            '<option name="FogLamp-Ft" variant="false" desc="Front Fog Lamps" inclusiveOptions="" exclusiveOptions=""/>' +
            '<option name="FogLamp-Rr" variant="false" desc="Rear Fog Lamps" inclusiveOptions="" exclusiveOptions=""/>' +
            '<option name="FuelLid" variant="false" desc="" inclusiveOptions="" exclusiveOptions=""/>' +
            '<option name="IllumEntry" variant="false" desc="Illuminated door entry" inclusiveOptions="" exclusiveOptions=""/>' +
            '<option name="LHD" variant="true" desc="Left hand drive market" inclusiveOptions="" exclusiveOptions="RHD"/>' +
            '<option name="Liftgate" variant="false" desc="" inclusiveOptions="" exclusiveOptions=""/>' +
            '<option name="PassAirbag" variant="false" desc="Optional passenger side airbag" inclusiveOptions="Airbags" exclusiveOptions=""/>' +
            '<option name="PwrMirrors" variant="false" desc="Power side view mirrors" inclusiveOptions="" exclusiveOptions=""/>' +
            '<option name="PwrWindows" variant="false" desc="Power Windows" inclusiveOptions="" exclusiveOptions=""/>' +
            '<option name="RearJack" variant="false" desc="Rear Headphone Jack" inclusiveOptions="" exclusiveOptions=""/>' +
            '<option name="RHD" variant="true" desc="Right hand drive market" inclusiveOptions="" exclusiveOptions="LHD"/>' +
            '<option name="Subwoofer" variant="false" desc="Rear Subwoofer" inclusiveOptions="" exclusiveOptions=""/>' +
            '</configurations></root>';
})();