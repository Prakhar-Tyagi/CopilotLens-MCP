/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global require, describe, it, expect, beforeEach, afterEach, mentor, runs, waitsFor, $*/
(function () {
    "use strict";
    window.diagramAsSystemsObjectFactoryImpl = false;
    var context, stubs, Model = Backbone.Model.extend(), Collection = Backbone.Collection.extend(),
        fileDisplayHandler = {
            display: function (content) {
                this.content = content;
            }
        };

    stubs = {
        systems: new Collection(),
        router: {
            loadProject: function (options) {
                //this.options = options;
                options.success && options.success();
            },
            open3DView: function (connId, systemId) {
                this.connId = connId;
                this.systemId = systemId;
            },
            openFaceView: function (connId, systemId) {
                this.connId = connId;
                this.systemId = systemId;
            },
            loadProjectAndView: function (projectToLoad) {
                this.options = projectToLoad;
                projectToLoad.success();
            }
        },
        backbone: Backbone,
        jquery: $,
        underscore: _,
        Package: new Backbone.Model({}),
        Packages: Backbone.Model.extend({
            fetch: function () {

            },
            findPackageByName: function (packageName) {
                stubs.Packages.packageName = packageName;
                return {id: "testId", name: "testProjectId"};
            },
            findSubPackageByParams: function (param) {
                stubs.Packages.packageName = param.packageName;
                return {id: "testId", name: "testProjectId"};
            }
        }),
        fileDisplayHandler: fileDisplayHandler
    };

    context = createContext(stubs);

    require(["ConfigurationsModel"], function(configModel) {
        context(["componentRouter", "routers/systemRouter", "routers/systemReportRouter",
                "routers/projectDataRouter", "routers/objectFaceViewRouter", "routers/object3DViewRouter", "systems"],
            function (componentRouter,
                systemRouter,
                systemReportRouter,
                projectDataRouter,
                objectFaceViewRouter,
                object3DViewRouter,
                systems) {

                var preFindPackageByName, preDataloader, preRouter, objectParserGetDiagram, popoverObjects;
                var origApplyCofig;
                var componentRouterUnderTest;

                describe("componentRouterTest", function () {
                    var applyConfigurationCalledWith = "";
                    var modelFetchCalled = false;
                    var fetch_param1;
                    var fetch_param2;

                    beforeEach(function () {

                        var object1 = new Model(), object2 = new Model(), objects = new Collection();
                        mentor.publisher.urlParams = {};

                        objectParserGetDiagram =
                            mentor.publisher.objectDataLoader && mentor.publisher.objectDataLoader.getDiagram;
                        mentor.publisher.objectDataLoader = {
                            getDiagram: function (content) {
                                content.diagramId = "testDiagramId";
                                return content;
                            },
                            findXrefsByObjectId: function () {
                                return [{
                                    maintText: "objec1",
                                    systemId: "testSystemId",
                                    objectId: "testObjectId",
                                    getDiagrams: function () {
                                        return [
                                            {mainText: "diagramName", diagramId: "diagramId"}
                                        ];
                                    },
                                    getReports: function () {
                                        return [
                                            {
                                                mainText: "reportName",
                                                systemId: "testSystemId"
                                            }
                                        ];
                                    }
                                }, {
                                    maintText: "objec2",
                                    systemId: "testSystemId",
                                    objectId: "testObjectId",
                                    getDiagrams: function () {
                                        return [
                                            {mainText: "diagramName", diagramId: "diagramId"}
                                        ];
                                    },
                                    getReports: function () {
                                        return [
                                            {
                                                mainText: "reportName",
                                                systemId: "testSystemId"
                                            }
                                        ];
                                    }
                                }];
                            }
                        };
                        object1.set({mainText: "conn1", objectId: "testObjectId", systemId: "testSystemId"});
                        object2.set({
                            mainText: "systemName",
                            diagramId: "testDiagramId",
                            systemId: "testSystemId",
                            getDiagrams: function () {
                                return [
                                    {mainText: "diagramName", diagramId: "diagramId"}
                                ];
                            },
                            getReports: function () {
                                return [
                                    {
                                        mainText: "reportName",
                                        systemId: "testSystemId"
                                    }
                                ];
                            }
                        });
                        systems.add(object2);
                        objects.add(object1);
                        componentRouterUnderTest = Object.create(componentRouter);

                        componentRouterUnderTest.findElementsByNameMatch = function () {

                            return [object1, object2];

                        };
                        componentRouterUnderTest.showObjectPopover = function (itemToSearch, x, y, objects) {
                            popoverObjects = objects;
                        };

                        componentRouterUnderTest.loadComponentDataToGetComponentUID = function (options) {
                            expect(options.componentType).toBe('connector');
                            areTheParametersExtractedCorrectly.call(this, options);

                        };

                        objectFaceViewRouter.loadComponentDataToGetComponentUID = function (options) {

                            expect(options.componentType).toBe('connector');
                            areTheParametersExtractedCorrectly.call(this, options);
                        };

                        function areTheParametersExtractedCorrectly(options)
                        {
                            expect(options.projectId).toBe('testId');
                            expect(options.componentName).toBe('conn1');
                            this.showComponent
                            (
                                {
                                    objectId: "testObjectId",
                                    systemId: "testSystemId"
                                }
                            );
                        }

                        origApplyCofig = componentRouterUnderTest.applyConfiguration;
                        componentRouterUnderTest.applyConfiguration = function (config) {
                            origApplyCofig.call(componentRouterUnderTest, config);
                            applyConfigurationCalledWith = config.config;
                        };

                        configModel.fetch = function(a, b) {
                            fetch_param1 = a;
                            fetch_param2 = b;
                            modelFetchCalled = true;
                        };


                        object3DViewRouter.loadComponentDataToGetComponentUID = function (options) {
                            expect(options.componentType).toBe('connectors');
                            areTheParametersExtractedCorrectly.call(this, options);
                        };
                        preDataloader = mentor.publisher.dataLoader;

                        mentor.publisher.dataLoader = {
                            getObjectByName: function (name, type, diagramName) {
                                this.name = name;
                                this.type = type;
                                this.diagramName = diagramName;
                                return {objectId: "testObjId", systemId: "testSystemId", diagramId: "testDiagramId"};
                            }
                        };
                        preRouter = mentor.publisher.router;

                    });

                    afterEach(function () {
                        mentor.publisher.dataLoader.name = undefined;
                        mentor.publisher.dataLoader = preDataloader;
                        mentor.publisher.router = stubs.router;
                        fileDisplayHandler.content = "";
                        mentor.publisher.objectDataLoader &&
                        (mentor.publisher.objectDataLoader.getDiagram = objectParserGetDiagram);
                        applyConfigurationCalledWith = "";
                        modelFetchCalled = false;
                        fetch_param1 = null;
                        fetch_param2 = null;
                        componentRouterUnderTest.applyConfiguration = origApplyCofig;
                    });

                    it("should be able to load componentRouter Module", function () {
                        expect(componentRouter).toBeDefined();
                        expect(systemRouter).toBeDefined();
                        expect(systemReportRouter).toBeDefined();
                        expect(projectDataRouter).toBeDefined();
                        expect(objectFaceViewRouter).toBeDefined();
                        expect(object3DViewRouter).toBeDefined();
                    });

                    it("should be able to load component URL", function () {
                        componentRouterUnderTest.findAndShowComponentByType("testPackage", "conn1",
                            "connector?componentType=connector&language=en&config=c1");
                        expect(fileDisplayHandler.content.systemId).toBe("testSystemId");
                        expect(fileDisplayHandler.content.diagramId).toBe("testDiagramId");
                        expect(fileDisplayHandler.content.objectId).toBe("testObjectId");
                        expect(fileDisplayHandler.content.type).toBe("systemSVG");
                    });

                    it("should be able to load component URL in correct language", function () {
                        mentor.publisher.urlParams.language = "en";
                        componentRouterUnderTest.findAndShowComponentByType("testPackage", "conn1",
                            "connector?language=en&config=c1");
                        expect(stubs.router.options.language).toBe('en');
                    });

                    it("should be able to load component URL in correct configuration", function () {
                        mentor.publisher.urlParams.config = "c1";
                        var isWaiting = true;
                        runs(function() {
                            componentRouterUnderTest.findAndShowComponentByType("testPackage", "conn1",
                                    "connector?language=en&config=c1");
                            setTimeout(function() {
                                isWaiting = false;
                            }, 200);
                        });

                        waitsFor(function() {
                           return !isWaiting;
                        }, 1000);

                        runs(function() {
                            expect(stubs.router.options.config).toBe('c1');
                            expect(applyConfigurationCalledWith).toBe('c1');

                            expect(modelFetchCalled).toBeTruthy();
                            expect(typeof fetch_param2).toBe('function');

                            var onModelLoadIsPassedAsCallBackToFetch = fetch_param2.name.indexOf("onModelLoad") > -1;
                            expect(onModelLoadIsPassedAsCallBackToFetch).toBeTruthy();
                        });
                    });

                    it("should be able to load diagram URL by decoding each URI component", function () {
                        componentRouterUnderTest.findAndShowComponentByType("testPackage%2FsomeName", "conn1",
                            "connector?language=en&config=c1");
                        expect(stubs.Packages.packageName).toBe('testPackage/someName');
                    });

                    it("should be able to load system URL", function () {
                        var openSystem = systemRouter.openComponent;
                        systemRouter.openComponent = function (options) {
                            this.displaySystemDiagram(options, systems);
                        };
                        mentor.publisher.urlParams = {
                            project: "testPackage%2FsomeName",
                            system: "systemName",
                            component: "diagramName",
                            componentType: "diagram"
                        };

                        systemRouter.findAndShowComponentByType("testPackage%2FsomeName", "systemName",
                            mentor.publisher.urlParams);
                        expect(JSON.stringify(fileDisplayHandler.content)).toBe(
                            '{"diagramId":"diagramId","type":"systemSVG"}')
                        systemRouter.openComponent = openSystem;
                    });

                    it("should be able to show object popover for componentURL", function () {
                        mentor.publisher.urlParams.internalLink = true;
                        componentRouterUnderTest.findElementInCollection({}, {}, "devices");
                        expect(componentRouterUnderTest.findElementInCollection({}, {}, "devices")).toBeFalsy();
                        mentor.publisher.urlParams.internalLink = false;
                    });

                    it("should be able to load system report URL", function () {
                        var openSystem = systemRouter.openComponent;
                        var isWaiting = true;

                        runs(function() {
                            systemReportRouter.openComponent = function (options) {
                                this.showSystemreport(options, systems);
                            };
                            systemReportRouter.findAndShowComponentByType("testPackage%2FsomeName", "systemName",
                                    {system: "systemName"});

                            setTimeout(function() {
                                isWaiting = false;
                            }, 200);
                        })

                        waitsFor(function() {
                            return !isWaiting;
                        }, 1000);

                        runs(function() {
                            expect(JSON.stringify(fileDisplayHandler.content)).toBe(
                                    '{"id":"testSystemId","reportId":"reportName","systemId":"testSystemId","type":"systemReport"}');
                            systemReportRouter.openComponent = openSystem;
                        });
                    });

                    it("should be able to load project data URL", function () {
                        var isWaiting = true;

                        runs(function() {
                            projectDataRouter.findAndShowProjectData("testPackage%2FsomeName",
                                    mentor.publisher.contentType.CUSTOM_VIEW, "LocationView");
                            setTimeout(function() {
                                isWaiting = false;
                            }, 200);
                        });

                        waitsFor(function() {
                            return !isWaiting;
                        }, 1000);

                        runs(function() {
                            expect(JSON.stringify(fileDisplayHandler.content)).toBe(
                                    '{"id":"LocationView","type":"customView","mainText":"LocationView"}');
                        });
                    });

                    it("should be able to load component FaceView", function () {
                        objectFaceViewRouter.findAndShowComponentByType("testPackage", "conn1",
                            "connector?language=en&config=c1");
                        expect(stubs.router.connId).toBe('testObjectId');
                    });

                    it("should be able to load component three d view", function () {
                        object3DViewRouter.findAndShowComponentByType("testPackage", "conn1",
                            "connectors?language=en&config=c1");
                        expect(stubs.router.connId).toBe('testObjectId');
                    });

                    it("should load both faceview and component for a faceview URL", function () {
                        var faceviewURLDisplayed = false, componentShown = false, fun1, fun2;
                        fun1 = mentor.publisher.router.openFaceView;
                        fun2 = componentRouterUnderTest.showComponent;
                        mentor.publisher.router.openFaceView = function () {
                            faceviewURLDisplayed = true;
                        };
                        componentRouter.showComponent = function () {
                            componentShown = true;
                        };
                        objectFaceViewRouter.showComponent({systemId: "testSystemID", objectId: "testObjectId"});
                        expect(componentShown).toBeTruthy();
                        expect(faceviewURLDisplayed).toBeTruthy();

                        mentor.publisher.router.openFaceView = fun1;
                        componentRouterUnderTest.showComponent = fun2;
                    });

                    function getComponentViaAMDLoader()
                    {
                        return {
                            componentLoaderLib: function (moduleName) {
                                expect(moduleName).toBe('ComponentLoader');
                                return {
                                    getComponentViewByName: function (componentType) {
                                        expect(componentType).toBe('connectors');
                                        return {
                                            getData: function () {
                                                return {
                                                    fetchData: function (message, callback) {
                                                        expect(message.method).toBe('getObjectByAttribute');
                                                        expect(message.attributes.name).toBe('name');
                                                        callback.success(this);
                                                    },
                                                    objectId: "connId",
                                                    systemId: "testSystemId"
                                                };

                                            }
                                        };
                                    }
                                };
                            }
                        };
                    }

                    it("should be able to fetch connector's UID and systemId using loaded view for connector",
                        function () {
                            var config = getComponentViaAMDLoader();
                            var options = {componentType: "connector"};
                            var data = componentRouterUnderTest.fetchComponentData(config, options);
                            expect(data.objectId).toBe("connId");
                            expect(data.systemId).toBe("testSystemId");
                        });
                    it("createComponentQuery should be able to use componentId when it is available", function () {
                        var options = {uid: "testObjectId"};
                        var query = componentRouterUnderTest.createComponentQuery(options);
                        expect(query.name).toBe('uid');
                        expect(query.value).toBe('testObjectId');
                    });

                    it("createComponentQuery should be able to use componentName when uid is not available",
                        function () {
                            var options = {componentName: "testObjectName"};
                            var query = componentRouterUnderTest.createComponentQuery(options);
                            expect(query.name).toBe('name');
                            expect(query.value).toBe('testObjectName');
                        });
                    it("loadComponentDataToGetComponentUID should be able to load data from component and call showComponent method",
                        function () {
                            var objUt = Object.create(componentRouter);
                            var componentLoaded = false;
                            objUt.onComponentLoad = function (data, options) {
                                componentLoaded = true;
                                expect(data.objectId).toBe('connId');
                                expect(data.systemId).toBe('testSystemId');
                            };
                            var config = getComponentViaAMDLoader();
                            var options = {componentType: "connector"};
                            objUt.loadComponentDataToGetComponentUID(options, config);
                            expect(componentLoaded).toBeTruthy();

                        });
                    it("componentNotFound should be called when object in URL does not exists", function () {
                        var objUt = Object.create(componentRouter);
                        var errorMessageShown;
                        objUt.componentNotFound = function (name, type) {
                            expect(name).toBe('testObjectName');
                            expect(type).toBe('connector');
                            errorMessageShown = true;
                        };
                        objUt.onComponentLoad({}, {
                            componentName: "testObjectName",
                            componentType: "connector"
                        });
                        expect(errorMessageShown).toBeTruthy();

                    });
                    afterEach(function () {
                        popoverObjects = '';
                    });

                });

            });
    });
})();

