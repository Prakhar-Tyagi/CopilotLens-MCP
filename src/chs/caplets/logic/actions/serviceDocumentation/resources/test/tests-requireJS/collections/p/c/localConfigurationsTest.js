/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

require(["LocalConfigurations", "currentPackage"], function (systemUnderTest, selectedPackage)
{
    describe('localConfigurationsTest', function ()
    {
        "use strict";

        var localStorage = window.localStorage;
        var preExistingSelectedPackage = selectedPackage.get("id");
        var testConfig1 = {
            "type": "configuration",
            "name": "Test Config 1",
            "value": "test options 1, test options 2"
        };
        var testConfig2 = {
            "type": "configuration",
            "name": "Zulu 101",
            "value": "Alpha, Beta, Foxtort"
        };

        beforeEach(function ()
        {
            if (!localStorage) {
                throw new Error("localstorage should be avilable to test this module");
            }
            localStorage.clear();
        });

        afterEach(function ()
        {
            localStorage.clear();
            selectedPackage.set("id", preExistingSelectedPackage, {silent: true});
        });

        it("saves configuration to local storage", function ()
        {
            var configs = systemUnderTest.getConfigs();
            expect(configs.length).toBe(0);

            systemUnderTest.saveConfig(testConfig1);

            var configs = systemUnderTest.getConfigs();
            expect(configs.length).toBe(1);

            var config1 = configs[0];
            expect(config1.name).toBe(testConfig1.name);
            expect(config1.value).toBe(testConfig1.value);
            expect(config1.type).toBe(testConfig1.type);
        });

        it("returns empty array when no configurations exist", function ()
        {
            var configs = systemUnderTest.getConfigs();
            expect(configs instanceof Array).toBeTruthy();
            expect(configs.length).toBe(0);
        });

        it("saves configuration specific to each package", function ()
        {
            selectedPackage.set("id", "package1", {silent: true});
            systemUnderTest.saveConfig(testConfig1);
            var configs1 = systemUnderTest.getConfigs();
            expect(configs1.length).toBe(1);

            selectedPackage.set("id", "package2", {silent: true});
            var configs2 = systemUnderTest.getConfigs();
            expect(configs2.length).toBe(0);
        });

        it("Should be able to delete Config with case insensitive name", function ()
        {
            selectedPackage.set("id", "package1", {silent: true});
            systemUnderTest.saveConfig(testConfig1);
            systemUnderTest.saveConfig(testConfig2);
            var configs = systemUnderTest.getConfigs();

            expect(configs.length).toBe(2);
            expect(systemUnderTest.deleteConfig(testConfig1.name.toLowerCase())).toBeTruthy();

            var configsAfterDelete = systemUnderTest.getConfigs();
            expect(configsAfterDelete.length).toBe(1);
        });

        it("Should handle parse exception if config data is manipulated in localstorge", function ()
        {
            selectedPackage.set("id", "package1", {silent: true});
            // corrupted JSON
            localStorage.setItem("package1", "[\"type\":\"configuration\",\"name\":\"Test Config 1\",\"value\":\"test options 1, test options 2\"},{\"type\":\"configuration\",\"name\":\"Zulu 101\",\"value\":\"Alpha, Beta, Foxtort\"}]")

            var configs = systemUnderTest.getConfigs();
            expect(configs.length).toBe(0);

            systemUnderTest.saveConfig(testConfig1);
            var configsAfterSave = systemUnderTest.getConfigs();
            expect(configsAfterSave.length).toBe(1);
        });

        it("retains configuration on switching package", function ()
        {
            selectedPackage.set("id", "package1", {silent: true});
            systemUnderTest.saveConfig(testConfig1);
            var configs1 = systemUnderTest.getConfigs();
            expect(configs1.length).toBe(1);

            selectedPackage.set("id", "package2", {silent: true});
            var configs2 = systemUnderTest.getConfigs();
            expect(configs2.length).toBe(0);
            systemUnderTest.saveConfig(testConfig2);

            selectedPackage.set("id", "package1", {silent: true});
            var configs3 = systemUnderTest.getConfigs();
            expect(configs3.length).toBe(1);
            expect(configs3[0].name).toBe(testConfig1.name);
            expect(configs3[0].value).toBe(testConfig1.value);
            expect(configs3[0].type).toBe(testConfig1.type);

            selectedPackage.set("id", "package2", {silent: true});
            var configs4 = systemUnderTest.getConfigs();
            expect(configs4.length).toBe(1);
            expect(configs4[0].name).toBe(testConfig2.name);
            expect(configs4[0].value).toBe(testConfig2.value);
            expect(configs4[0].type).toBe(testConfig2.type);
        });
    });
}, function (err)
{
    describe("localConfigurationsTest - module load Error", function ()
    {
        it("Module load failed", function ()
        {
            console.log(err.message + "::\n" + err.stack);
            expect(false).toBeTruthy();
        });
    });
});