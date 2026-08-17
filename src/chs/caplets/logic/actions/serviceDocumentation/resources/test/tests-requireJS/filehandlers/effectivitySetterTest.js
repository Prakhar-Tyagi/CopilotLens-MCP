/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, �SISW�), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer�s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
define(["filehandlers/effectivitySetter"], function (effectivitySetter) {
    "use strict";
    describe("effectivitySetterTest", function () {
        var orgPackectInfo;
        var findPackageByNameParams;
        var findPackageByIdParams;

        beforeEach(function () {
            findPackageByNameParams = '';
            findPackageByIdParams = '';
            if (!getWindowObj().mentor.publisher) {
                getWindowObj().mentor.publisher = {};
            }
            orgPackectInfo = getWindowObj().mentor.publisher.packectInfo;
            effectivitySetter.getAllPackages = dummyPackageObjFun();
        });
        it("should be able to load effectivitySetter", function () {
            expect(effectivitySetter).toBeTruthy();
        });

        it("should be able to initialize packet information for non effectivity packets", function () {
            effectivitySetter.initializeEffectivity({
                projectId: "data\\package-id"
            });
            expect(getWindowObj().mentor.publisher.packectInfo.packageId).toBe('package-id');
            expect(findPackageByNameParams).toBe('data\\package-id');
            expect(getWindowObj().mentor.publisher.packectInfo.effectivity).toBe('package-id');
        });

        it("test initialize non effectivity packets", function () {
            var subPackageInfo = {
                projectId: "data\\package-id",
                range: "e1-e2",
                projId: "data\\parentId"
            };
            effectivitySetter.initializeEffectivity(subPackageInfo);
            expect(getWindowObj().mentor.publisher.packectInfo.packageId).toBe('package-id');
            expect(JSON.stringify(findPackageByIdParams)).toBe(JSON.stringify(subPackageInfo));
            expect(getWindowObj().mentor.publisher.packectInfo.effectivity).toBe('subPackage-id');
        });

        it("test initialize should not set effectivity data for pop-out window", function () {
            var isPopoutWindowFun = effectivitySetter.isPopoutWindow;
            effectivitySetter.isPopoutWindow = function () {
                return true;
            };
            getWindowObj().mentor.publisher.packectInfo = {};
            var subPackageInfo = {
                projectId: "data\\package-id",
                range: "e1-e2",
                projId: "data\\parentId"
            };
            effectivitySetter.initializeEffectivity(subPackageInfo);
            expect(getWindowObj().mentor.publisher.packectInfo.packageId).toBeUndefined();
            effectivitySetter.isPopoutWindow = isPopoutWindowFun;
        });

        it("test url should be added with packageId and effectivityId", function () {
            var subPackageInfo = {
                projectId: "data\\package-id",
                range: "e1-e2",
                projId: "data\\parentId"
            };
            effectivitySetter.initializeEffectivity(subPackageInfo);
            var modifiedURL = effectivitySetter.addEffectivitAndZipLocationInURLAsParameters("index.xml");
            expect(modifiedURL).toBe("index.xml?effectivity=subPackage-id&packageId=package-id");
            modifiedURL = effectivitySetter.addEffectivitAndZipLocationInURLAsParameters("index.xml?system=xyz");
            expect(modifiedURL).toBe("index.xml?system=xyz&effectivity=subPackage-id&packageId=package-id");
            modifiedURL =
                    effectivitySetter.addEffectivitAndZipLocationInURLAsParameters("index.xml?system=xyz&diagram=abc");
            expect(modifiedURL).toBe("index.xml?system=xyz&diagram=abc&effectivity=subPackage-id&packageId=package-id");
        });

        it("should prepend zipped content with 'zipped' prefix", function () {
            effectivitySetter.urlPrefix = "zipped/";
            var org = effectivitySetter.urlExists;
            effectivitySetter.urlExists = function () {
                return false;
            };
            var subPackageInfo = {
                projectId: "data\\package-id",
                range: "e1-e2",
                projId: "data\\parentId"
            };
            effectivitySetter.initializeEffectivity(subPackageInfo);
            var modifiedURL = effectivitySetter.distinguishZippedContent("data/index.xml");
            expect(modifiedURL).toBe('zipped/data/index.xml');
            effectivitySetter.urlExists = org;
        });

        it("should not modify urls for non zip packet", function () {
            var org = effectivitySetter.urlExists;
            effectivitySetter.urlExists = function () {
                return true;
            };
            var subPackageInfo = {
                projectId: "data\\package-id"
            };
            effectivitySetter.checkContentType = true;
            effectivitySetter.initializeEffectivity(subPackageInfo);
            var modifiedURL = effectivitySetter.distinguishZippedContent("data/index.xml");
            expect(modifiedURL).toBe('data/index.xml');
            effectivitySetter.urlExists = org;
        });

        it("should return original URL if packageInfo is null", function () {
            getWindowObj().mentor.publisher.packectInfo = null;
            var result = effectivitySetter.distinguishZippedContent("data/index.xml");
            expect(result).toBe('data/index.xml');
        });

        it("should set package id and project id in URL", function () {
            var packageObj = {
                name: "packageName",
                id: "packageId",
                range: "",
                projectId: "projectId"
            };
            var packageInfo = new Backbone.Model(packageObj);
            var decoratedURL = effectivitySetter.addPackageInformation("index.html?system=systemName",
                    packageInfo);
            expect(decoratedURL).toBe('index.html?system=systemName&projId=projectId&packageId=packageId&package=packageName');
        });

        it("should read package id correctly in URL", function () {
            var packageObj = {
                name: "packageName",
                id: "packageId\\d467",
                range: "",
                projectId: "projectId"
            };
            var packageInfo = new Backbone.Model(packageObj);
            var decoratedURL = effectivitySetter.addPackageInformation("index.html?system=systemName",
                    packageInfo);
            expect(decoratedURL).toBe(
                    'index.html?system=systemName&projId=projectId&packageId=packageId/d467&package=packageName');
        });

        it("should set/reset effectivity in cookies", function () {
            var org_createCookie = Utils.createCookie;
            var name;
            var value;
            var days;
            Utils.createCookie = function (n, v, d) {
                name = n;
                value = v;
                days = d;
            };
            var subPackageInfo = {
                projectId: "data\\package-id",
                range: "e1-e2",
                projId: "data\\parentId"
            };
            effectivitySetter.initializeEffectivity(subPackageInfo);
            effectivitySetter.setEffectivityInCookies();
            expect(name).toBe('packageId');
            expect(value).toBe('package-id');
            effectivitySetter.resetEffectivityCookies();
            expect(name).toBe('effectivity');
            expect(days).toBe(-1);
            Utils.createCookie = org_createCookie;
        });

        afterEach(function () {
            //restore
            getWindowObj().mentor.publisher.packectInfo = orgPackectInfo;
        });

        function dummyPackageObjFun()
        {
            return function () {
                return {
                    findPackageById: function (packageId, range, projectId) {
                        findPackageByIdParams = {
                            projectId: packageId,
                            range: range,
                            projId: projectId
                        };
                        return {
                            id: "subPackage-id"
                        }
                    },
                    findPackageByName: function (params) {
                        findPackageByNameParams = params;
                        return {
                            get(id)
                            {
                                return params;
                            }
                        }
                    }
                };
            };
        }
    });

});
