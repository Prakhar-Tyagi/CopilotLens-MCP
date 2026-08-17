/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/* global describe, beforeEach, spyOn, ajax_response, it, expect */


describe("xmlLoaderCallAPITest", function ()
{

    var ajax = $.ajax;
    var url = "";

    beforeEach(function ()
    {
        $.ajax = function (param)
        {
            url = param.url;

            param.success("data", "textStatus", "jqXHR");
        };
    });

    it("is it [1, 2, 3]", function ()
    {
        var response = mentor.publisher.xmlLoader.callAPI("search", {
            language: "en",
            q: "8H+J",
            options: "",
            package: "1\2"
        });
        var data = response.data;
        expect(data).toEqual("data");
        expect(url).toBe('search?packageId=12da&language=en&q=8H%2BJ&options=&package=1%02');
    });

    it("Tooltip Test for 3D Diagrams", function (){
        var testData = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><object><diagrams><diagram><name>3D</name><tooltip value='3D tooltip Test'></tooltip><maintext>3D Test</maintext></diagram></diagrams></object>";
        var loader = mentor.publisher.objectDataParser;
        var result = loader(testData, "Dummy", "Dummy").get3DViews();
        var result = result.listItems[0].getToolTips()[0].getValue();
        expect(result).toEqual("3D tooltip Test");
    });

    it("should load internal URLsFromConfig", function (){
        expect(mentor.publisher.xmlLoader.loadInternalURLsFromConfig()).toEqual("data");
    });

    it("should be able to convert string XML to DOM", function (){
        expect(mentor.publisher.xmlLoader.convertStringXMLToDOM({})).toEqual({});
    });

    it("should set the boolean if it is a report", function (){
        expect(mentor.publisher.xmlLoader.isItaReport('testPath')).toBeFalsy();
    });


    afterEach(function ()
    {
        $.ajax = ajax;
    });
});

describe("FaceViewParserTest", function ()
{
    it("should be able to parse faceviews", function (){
        var faceViews=[];
        $('body').append('<div id="fvDOMElementID" faceviewId="faceviewId" multiple-faceview-support=true/>');
        mentor.publisher.faceviewParser().parse(faceViews, '#fvDOMElementID', 'testSysID');
        expect(faceViews).toEqual([
            {
                mainText : '',
                name : '',
                id : 'faceviewId',
                faceviewId : 'faceviewId',
                objectId : 'fvDOMElementID',
                symbol : '.svg',
                showPopoutButton : true,
                "multiple-faceview-support" : 'true/',
                view : 'noViewSpecified',
                cavityTable : '.html',
                systemId : 'testSysID'
            }
        ]);
    });

    it("should be able to translate", function (){
        expect(mentor.publisher.faceviewParser().translator('testText')).toBe('translatedValue');
    });
});

describe("CustomGeneratorDataLoaderTest", function ()
{
    it("should be able to load", function (){
        expect(
            mentor.publisher.customGeneratorDataLoader.load(
            "type",
            "testProjectID",
            "testSysID",
            "testDiagramID"
            )
        ).toEqual([]);
    });
});

describe("ObjectDataLoaderTest", function ()
{
    it("should be able to load design objects", function () {
        var origGetObjectBucket = mentor.publisher.objectDataLoader.getObjectBucket,
                origGetObjectPath = mentor.publisher.objectDataLoader.getObjectPath
        ;

        mentor.publisher.objectDataLoader.getObjectBucket = function () {return "testBucket";};
        mentor.publisher.objectDataLoader.getObjectPath = function () {return "testPath";};

        spyOn(mentor.publisher.objectDataLoader, 'loadObjectDataFrom').andCallThrough();
        mentor.publisher.objectDataLoader.load(
                "testSysID",
                "testObjUID",
                "testProjID",
        );
        expect(mentor.publisher.objectDataLoader.loadObjectDataFrom).toHaveBeenCalled();

        mentor.publisher.objectDataLoader.getObjectBucket = origGetObjectBucket;
        mentor.publisher.objectDataLoader.getObjectPath = origGetObjectPath;
    });

    it("should be able load cross refs from multiple objects", function () {
        var origLoad = mentor.publisher.objectDataLoader.load,
            origMergeCrossRefsFromObjs = mentor.publisher.objectDataLoader.mergeCrossRefsFromObjs
        ;
        mentor.publisher.objectDataLoader.load = function () {return {getName: function () {return "testName";}}};
        mentor.publisher.objectDataLoader.mergeCrossRefsFromObjs = function () {};

        spyOn(mentor.publisher.objectDataLoader, 'load').andCallThrough();
        mentor.publisher.objectDataLoader.loadCrossRefsFromMultipleObjects(
                "testSysID",
                ["testObjUID1"],
                "testProjID",
        );
        expect(mentor.publisher.objectDataLoader.load).toHaveBeenCalled();

        mentor.publisher.objectDataLoader.load = origLoad;
        mentor.publisher.objectDataLoader.mergeCrossRefsFromObjs = origMergeCrossRefsFromObjs;
    });

    it("should be able to load object from", function () {
        var objectDataResponse = {
                data: {},
            },
            origReferenceObjectParser= mentor.publisher.referenceObjectParser,
            origLoadXMLByAjax = mentor.publisher.xmlLoader.loadXMLByAjax,
            origLoadCrossRefsFromMultipleObjects = mentor.publisher.objectDataLoader.loadCrossRefsFromMultipleObjects,
            origLoad = mentor.publisher.objectDataLoader.load
        ;

        mentor.publisher.xmlLoader.loadXMLByAjax = function () {return objectDataResponse;};
        mentor.publisher.objectDataLoader.loadCrossRefsFromMultipleObjects = function () {
            return {};
        };
        mentor.publisher.objectDataLoader.load = function () {
            return {};
        };

        mentor.publisher.referenceObjectParser = function () {
            return {
                shouldRedirect: function () {
                    return true;
                },
                hasMultipleUIDRefs: function () {
                    return true;
                },
                getReferenceIds: function () {
                    return ['testRefId'];
                },
            };
        };
        spyOn(mentor.publisher.objectDataLoader, 'loadCrossRefsFromMultipleObjects').andCallThrough();
        mentor.publisher.objectDataLoader.loadObjectDataFrom(
            "testObjectPath",
            "testSysID",
            "testShortUID",
            "testProjID"
        );
        expect(mentor.publisher.objectDataLoader.loadCrossRefsFromMultipleObjects).toHaveBeenCalled();

        mentor.publisher.referenceObjectParser = function () {
            return {
                shouldRedirect: function () {
                    return true;
                },
                hasMultipleUIDRefs: function () {
                    return false;
                },
                getReferenceIds: function () {
                    return ['testRefId'];
                },
            };
        };
        spyOn(mentor.publisher.objectDataLoader, 'load').andCallThrough();
        mentor.publisher.objectDataLoader.loadObjectDataFrom(
                "testObjectPath",
                "testSysID",
                "testShortUID",
                "testProjID"
        );
        expect(mentor.publisher.objectDataLoader.load).toHaveBeenCalled();

        mentor.publisher.referenceObjectParser = origReferenceObjectParser;
        mentor.publisher.xmlLoader.loadXMLByAjax = origLoadXMLByAjax;
        mentor.publisher.objectDataLoader.loadCrossRefsFromMultipleObjects = origLoadCrossRefsFromMultipleObjects;
        mentor.publisher.objectDataLoader.load = origLoad;
    });

    it("should be able to load object data references", function () {
        var objectDataResponse = {
                    data: {},
                },
                origReferenceObjectParser= mentor.publisher.referenceObjectParser,
                origLoadXMLByAjax = mentor.publisher.xmlLoader.loadXMLByAjax
        ;

        mentor.publisher.xmlLoader.loadXMLByAjax = function () {return objectDataResponse;};

        mentor.publisher.referenceObjectParser = function () {
            return {
                shouldRedirect: function () {
                    return true;
                },
                getReferenceIds: function () {
                    return ['testRefId'];
                },
            };
        };
        expect(
                mentor.publisher.objectDataLoader.loadObjectDataReferences(
                        "testObjectPath",
                        "testSysID",
                        "testShortUID",
                        "testProjID",
                        "testObjectUID"
                )
        ).toEqual(['testRefId']);

        mentor.publisher.referenceObjectParser = function () {
            return {
                shouldRedirect: function () {
                    return false;
                },
            };
        };
        expect(
                mentor.publisher.objectDataLoader.loadObjectDataReferences(
                        "testObjectPath",
                        "testSysID",
                        "testShortUID",
                        "testProjID",
                        "testObjectUID"
                )
        ).toEqual(['testObjectUID']);

        mentor.publisher.referenceObjectParser = origReferenceObjectParser;
        mentor.publisher.xmlLoader.loadXMLByAjax = origLoadXMLByAjax;
    });

    it("should be able to return true if the UIDs are stored in buckets", function () {
        var origGetVersionFilePath= mentor.publisher.xmlLoader.getVersionFilePath,
            origLoadFile = mentor.publisher.xmlLoader.loadFile
        ;

        mentor.publisher.pathResolver.getVersionFilePath = function () {}
        mentor.publisher.xmlLoader.loadFile = function () {
            return {
                data: {
                    version: 'v1',
                }
            };
        };

        expect(mentor.publisher.objectDataLoader.areUIDsStoredInBuckets("testProjID")).toBe("v1");

        mentor.publisher.pathResolver.getVersionFilePath = origGetVersionFilePath;
        mentor.publisher.xmlLoader.loadFile = origLoadFile;
    });

    it("should be able to get object bucket", function () {
        var origAreUIDsStoredInBuckets = mentor.publisher.objectDataLoader.areUIDsStoredInBuckets;

        mentor.publisher.objectDataLoader.areUIDsStoredInBuckets= function () {return true;};
        spyOn(mentor.publisher.uidBuckets, 'getUIDBucket').andCallThrough();
        mentor.publisher.objectDataLoader.getObjectBucket(
                "testSysID",
                "testObjUID",
                "testProjID"
        );
        expect(mentor.publisher.uidBuckets.getUIDBucket).toHaveBeenCalled();

        mentor.publisher.objectDataLoader.areUIDsStoredInBuckets= function () {return false;};
        spyOn(mentor.publisher.shortenedUIDMap, 'getShortenedDesignUID').andCallThrough();
        mentor.publisher.objectDataLoader.getObjectBucket(
                "testSysID",
                "testObjUID",
                "testProjID"
        );
        expect(mentor.publisher.shortenedUIDMap.getShortenedDesignUID).toHaveBeenCalled();

        mentor.publisher.objectDataLoader.areUIDsStoredInBuckets = origAreUIDsStoredInBuckets;
    });

    it("should be able to get Object Path", function () {
        var origAreUIDsStoredInBuckets = mentor.publisher.objectDataLoader.areUIDsStoredInBuckets,
            origPathResolver = mentor.publisher.pathResolver.pathResolver;

        mentor.publisher.objectDataLoader.areUIDsStoredInBuckets= function () {return true;};
        mentor.publisher.pathResolver.getObjectPath = function () {};
        spyOn(mentor.publisher.pathResolver, 'getObjectPath').andCallThrough();
        mentor.publisher.objectDataLoader.getObjectPath(
                "testSysID",
                "testObjUID",
                "testProjID",
                [],
        );
        expect(mentor.publisher.pathResolver.getObjectPath).toHaveBeenCalled();

        mentor.publisher.objectDataLoader.areUIDsStoredInBuckets= function () {return false;};
        expect(
                mentor.publisher.objectDataLoader.getObjectPath(
                        "testSysID",
                        "testObjUID",
                        "testProjID",
                        "UIDBucket",
                )
        ).toEqual("testProjID/UIDBucket/O/testObjUID.xml");


        mentor.publisher.objectDataLoader.areUIDsStoredInBuckets = origAreUIDsStoredInBuckets;
        mentor.publisher.pathResolver.pathResolver = origPathResolver;
    });

    it("should be able to merge cross refs from objects", function () {
        expect(mentor.publisher.objectDataLoader.mergeCrossRefsFromObjs([])).toEqual({});
        var result = mentor.publisher.objectDataLoader.mergeCrossRefsFromObjs(
            [
                {
                    getCrossReferences: function () {
                        return {
                            xrefs: []
                        }
                    },
                    getName: function () {
                        return "testName";
                    }
                }
            ]
        );
        expect(result.getName()).toBe("testName");
        expect(result.getType()).toBe("");
        expect(result.getCrossReferences()).toEqual(
            {
                listItems : [undefined],
                title : 'XRefTitle',
                textFilterable : true,
                xrefs : []
            }
        );

    });

    it("should be able to load reference ids if any", function () {
        var origGetObjectBucket = mentor.publisher.objectDataLoader.getObjectBucket,
            origGetObjectPath = mentor.publisher.objectDataLoader.getObjectPath,
            origAreUIDsStoredInBuckets = mentor.publisher.objectDataLoader.areUIDsStoredInBuckets
        ;

        mentor.publisher.objectDataLoader.getObjectBucket = function () {return "testBucket";};
        mentor.publisher.objectDataLoader.getObjectPath = function () {return "testPath";};
        mentor.publisher.objectDataLoader.areUIDsStoredInBuckets = function () {return true;};

        spyOn(mentor.publisher.objectDataLoader, 'loadObjectDataReferences').andCallThrough();
        mentor.publisher.objectDataLoader.loadRefernceIdsIfAny(
            "testSysID",
            "testObjUID",
            "testProjID",
        );
        expect(mentor.publisher.objectDataLoader.loadObjectDataReferences).toHaveBeenCalled();

        mentor.publisher.objectDataLoader.getObjectBucket = origGetObjectBucket;
        mentor.publisher.objectDataLoader.getObjectPath = origGetObjectPath;
        mentor.publisher.objectDataLoader.areUIDsStoredInBuckets = origAreUIDsStoredInBuckets;
    });

    it("should be able to load objects", function () {
        var origLoadXMLByAjax = mentor.publisher.xmlLoader.loadXMLByAjax;


        mentor.publisher.xmlLoader.loadXMLByAjax = function () {
            return {
                data: {}
            };
        };

        expect(
            mentor.publisher.objectDataLoader.loadObjects(
                "testType",
                true
            )
        ).toEqual({});
        ;

        mentor.publisher.xmlLoader.loadXMLByAjax = origLoadXMLByAjax;
    });

    it("should be able to get diagram", function () {
        var origLoadObjectData = mentor.publisher.project.loadObjectData;

        mentor.publisher.project.loadObjectData = function () {
            return {
                getCrossReferences: function () {
                    return {
                        xrefs: []
                    }
                },
                getHarnessLayouts: function () {
                    return [{}];
                }
            }
        };

        expect(
            mentor.publisher.objectDataLoader.getDiagram({})
        ).toEqual(
            {
                type: 'harnessLayoutDiagram'
            }
        )
        ;

        mentor.publisher.project.loadObjectData = origLoadObjectData;
    });

    it("should be able to get object file name", function () {
        expect(
            mentor.publisher.objectDataLoader.getObjectFileName("testType",)
        ).toBe("testTypes");
    });

    it("should be able to load objects from global file", function () {
        spyOn(mentor.publisher.xmlLoader, 'loadXMLByAjax').andCallThrough();
        mentor.publisher.objectDataLoader.loadObjectsFromGlobalFile(
            "testType"
        );
        expect(mentor.publisher.xmlLoader.loadXMLByAjax).toHaveBeenCalled();
    });

    it("should be able to find object by name", function () {
        var origLoadObjectsFromGlobalFile = mentor.publisher.objectDataLoader.loadObjectsFromGlobalFile;
        mentor.publisher.objectDataLoader.loadObjectsFromGlobalFile= function () {return {data: {}}};

        spyOn(mentor.publisher.objectDataLoader, 'findObjectInGlobalObjectFile').andCallThrough();
        mentor.publisher.objectDataLoader.findObjectByName(
            "testName",
            "testType",
            "testID"
        );
        expect(mentor.publisher.objectDataLoader.findObjectInGlobalObjectFile).toHaveBeenCalled();

        mentor.publisher.objectDataLoader.loadObjectsFromGlobalFile = origLoadObjectsFromGlobalFile;
    });

});

describe("ObjectDataParserTest", function () {
    var p = mentor.publisher;

    it("should be able to create list groups", function () {
        var objectDataParser = p.objectDataParser(),
            origLocationViewByName = p.locationViews.locationViewByName,
            listGroups = []
        ;
        p.locationViews.locationViewByName= function () {return {
            path: [
                {
                    path: 'testPath1',
                    objectId: 'testID1'
                },
            ],
        }};

        spyOn(objectDataParser, 'getAttributes').andCallThrough();
        spyOn(objectDataParser, 'getCrossReferences').andCallThrough();
        spyOn(objectDataParser, 'get2dLocationViews').andCallThrough();
        objectDataParser.createListGroups();
        expect(objectDataParser.getAttributes).toHaveBeenCalled();
        expect(objectDataParser.getCrossReferences).toHaveBeenCalled();
        expect(objectDataParser.get2dLocationViews).toHaveBeenCalled();

        p.locationViews.locationViewByName= origLocationViewByName;
    });

    it("should be able to get attributes", function () {
        $('body').append('<attributes simpleName="testName"/>');
        var objectDataParser = p.objectDataParser();
        expect(objectDataParser.getAttr()).toEqual('');
        expect(objectDataParser.getAttr("type")).toEqual('testName');
        expect(objectDataParser.getAttr("testAttr")).toEqual('');
    });

    it("should be able to get signal and global signals", function () {
        $('body').append('<signal>testSignal</signal><globalsignal>testGlobalSignal</globalsignal>');
        var objectDataParser = p.objectDataParser();
        objectDataParser.getShieldBodyUIDs();
        expect(objectDataParser.getSignal()).toBe('testSignal');
        expect(objectDataParser.getGlobalSignal()).toBe('testGlobalSignal');
    });


    it("should be able to get harness layouts", function () {
        var objectDataParser = p.objectDataParser();
        expect(objectDataParser.getHarnessLayouts()).toEqual([]);
    });

    it("should be able to get optionExpress", function () {
        $('body').append('<section id="testType" value="testValue"></section>');
        var objectDataParser = p.objectDataParser();
        objectDataParser.get("tesType");
        expect(objectDataParser.getOptionExpression()).toEqual("");
    });

});

describe("ReferenceObjectParserTest", function () {
    var p = mentor.publisher;

    it("should be able to get reference object", function () {
        var referenceObjectParser = p.referenceObjectParser();
        expect(referenceObjectParser.getReferenceIds()).toEqual();
    });

    it("should be able to check if it has multiple UIDRefs", function () {
        var referenceObjectParser = p.referenceObjectParser();
        expect(referenceObjectParser.hasMultipleUIDRefs()).toBeFalsy();
    });

    it("should be able to check if it should redirect", function () {
        var referenceObjectParser = p.referenceObjectParser();
        expect(referenceObjectParser.shouldRedirect()).toBeFalsy();
    });

});

describe("XmlDataLoaderTest", function () {
    var p = mentor.publisher;

    it("should be able get project preferences", function () {
        var xmlDataLoaderObject = xmlDataLoader(objectFactoryImpl())
        expect(xmlDataLoaderObject.getProjectPreferences('testProjID')).toEqual({ hookupConnectOntoMulticore : false, hookupConnectOntoOverbraid : false });
    });

    it("should be able load fault code, configurations, option filter info and packages", function () {
        var xmlDataLoaderObject = xmlDataLoader(objectFactoryImpl())
        expect(xmlDataLoaderObject.loadFaultCodeById()).toEqual([]);

        spyOn(p.xmlLoader, 'loadXMLByAjax').andCallThrough();
        xmlDataLoaderObject.loadConfigurationData('testProjID', function () {});
        expect(p.xmlLoader.loadXMLByAjax).toHaveBeenCalledWith("testProjID/Resources/Config/config.xml", false, false);

        expect(xmlDataLoaderObject.loadOptionFilterInfo()).toEqual({vin: '', config: '' });
        expect(xmlDataLoaderObject.loadPackages()).toEqual([]);
    });

    it("should be able get 3d xml by id", function () {
        var xmlDataLoaderObject = xmlDataLoader(objectFactoryImpl());
        spyOn(p.xmlLoader, 'loadXMLByAjax').andCallThrough();
        xmlDataLoaderObject.get3dXmlId("testCurrentProject", "testFolder", "testFileName", function () {});
        expect(p.xmlLoader.loadXMLByAjax).toHaveBeenCalledWith("testCurrentProject/testFolder/O/testFileName.xml", false, false);
    });

    it("should be able get signal data for highlight in rendered SVG", function () {
        var xmlDataLoaderObject = xmlDataLoader(objectFactoryImpl());
        spyOn($, 'ajax');
        xmlDataLoaderObject.getSignalDataForHighlightInRenderedSVG("testSignalName", function () {});
        expect($.ajax).toHaveBeenCalled();
    });

    it("should be able get signal objects", function () {
        var xmlDataLoaderObject = xmlDataLoader(objectFactoryImpl()),
            origLoadXMLByAjax=p.xmlLoader.loadXMLByAjax;
        p.xmlLoader.loadXMLByAjax = function () {return {data: {}}};
        expect(xmlDataLoaderObject.getSignalObjects()).toEqual([]);
        p.xmlLoader.loadXMLByAjax = origLoadXMLByAjax;
    });

    it("should be able read and parse object popup config", function () {
        var xmlDataLoaderObject = xmlDataLoader(objectFactoryImpl()),
            origLoadXMLByAjax=p.xmlLoader.loadXMLByAjax;
        p.xmlLoader.loadXMLByAjax = function () {return {data: {}}};

        spyOn(p.xmlLoader, 'loadXMLByAjax').andCallThrough();
        xmlDataLoaderObject.readAndParseObjectPopupConfig(function () {});
        expect(p.xmlLoader.loadXMLByAjax).toHaveBeenCalled();

        p.xmlLoader.loadXMLByAjax = origLoadXMLByAjax;
    });

    it("should be able get object property to use for title", function () {
        var xmlDataLoaderObject = xmlDataLoader(objectFactoryImpl())
        expect(xmlDataLoaderObject.getObjectPropertyToUseForTitle()).toEqual('');
    });

    it("should be able get Popover order, custom popover section order", function () {
        var xmlDataLoaderObject = xmlDataLoader(objectFactoryImpl());
        xmlDataLoaderObject.readAndParseObjectPopupConfig = function (callBack) {callBack();};
        spyOn(xmlDataLoaderObject, 'readAndParseObjectPopupConfig').andCallThrough();
        xmlDataLoaderObject.getPopoverOrder();
        xmlDataLoaderObject.getCustomPopoverSectionOrder();
        expect(xmlDataLoaderObject.readAndParseObjectPopupConfig).toHaveBeenCalled();
    });

    it("should be able face view symbol, cavity table and design object", function () {
        var xmlDataLoaderObject = xmlDataLoader(objectFactoryImpl()),
            origGetVersion=p.pathResolver.getVersionFilePath;
        p.pathResolver.getVersionFilePath = function () {return "testPath"};
        // xmlDataLoaderObject.getSystemPath= function () {return "testSysPath"};
        expect(xmlDataLoaderObject.getCavityTable("CavityTable.xml", "testSysID", "testProjID")).toBe("testProjID/testSysID/FaceViews/CavityTable.xml");

        spyOn(p.pathResolver, 'getFaceViewSymbol').andCallThrough();
        xmlDataLoaderObject.getFaceViewSymbol();
        expect(p.pathResolver.getFaceViewSymbol).toHaveBeenCalled();

        spyOn(p.xmlLoader, 'loadXMLByAjax').andCallThrough();
        xmlDataLoaderObject.getDesignObjects();
        expect(p.xmlLoader.loadXMLByAjax).toHaveBeenCalled();
    });

    it("should be able get window title config data", function () {
        var xmlDataLoaderObject = xmlDataLoader(objectFactoryImpl()),
            origAjax=$.ajax;
        $.ajax = function (param) {param.success();};
        expect(xmlDataLoaderObject.getWindowTitleConfigData()).toEqual(
            { attributeNames : [ ], delimiter : undefined, autoFitSVGOnWindowResize : undefined, showPathForFaceViews : undefined, showPathFor2dViews : undefined }
        );
        $.ajax = origAjax;
    });

    it("should be able get related order, navigation panels and navigation panels order", function () {
        var xmlDataLoaderObject = xmlDataLoader(objectFactoryImpl()),
            origLoadXMLByAjax=p.xmlLoader.loadXMLByAjax;
        p.xmlLoader.loadXMLByAjax = function () {return {data: {}}};
        expect(
            xmlDataLoaderObject.getRelatedDataOrder('testProjID', 'testSysID', 'testSysName')
        ).toEqual([]);
        expect(xmlDataLoaderObject.getNavigationPanels()).toEqual([]);
        expect(xmlDataLoaderObject.getNavigationPanelOrder()).toEqual([]);
        p.xmlLoader.loadXMLByAjax = origLoadXMLByAjax;
    });

    it("should be able get navigation panel object", function () {
        var xmlDataLoaderObject = xmlDataLoader(objectFactoryImpl());
        var navPanelObject = xmlDataLoaderObject.getNavigationPanelObject("introduction-page", "testType");
        expect(navPanelObject.title).toBe("introduction-page");
    });

    it("should be able get navigation panel object map", function () {
        var xmlDataLoaderObject = xmlDataLoader(objectFactoryImpl());
        var resultMap=xmlDataLoaderObject.getNavigationPanelObjectMap();
        var origGetDiagrams = mentor.publisher.project.getDiagrams,
            origGetReports = mentor.publisher.project.getReports
            origGetID = mentor.publisher.project.getID,
            origGetObjects = mentor.publisher.project.getObjects,
            origAjax=$.ajax;
        ;
        mentor.publisher.project.getDiagrams = function () {return []};
        mentor.publisher.project.getReports = function () {return []};
        mentor.publisher.project.getId = function () {return "testId"};
        mentor.publisher.project.getObjects = function () {return []};

        $.ajax=function (param) {};
        Object.keys(resultMap).forEach(function (key, index){
            resultMap[key].listItems();
        });

        expect(resultMap.system).toBeDefined();
        expect(resultMap.LocationViews).toBeDefined();
        expect(resultMap.faultcode).toBeDefined();
        expect(resultMap.diagnostics).toBeDefined();
        expect(resultMap.harness).toBeDefined();

        $.ajax=origAjax;
        mentor.publisher.project.getDiagrams = origGetDiagrams;
        mentor.publisher.project.getReports = origGetReports;
        mentor.publisher.project.getId = origGetID;
        mentor.publisher.project.getObjects = origGetObjects;
    });

    it("should be able get project", function () {
        var xmlDataLoaderObject = xmlDataLoader(objectFactoryImpl());
        var resProject = xmlDataLoaderObject.getProject("testProjectID");
        var origGetDiagrams = mentor.publisher.project.getDiagrams,
            origGetReports = mentor.publisher.project.getReports
            origGetID = mentor.publisher.project.getID,
            origGetObjects = mentor.publisher.project.getObjects,
            origGetNavigationPanelOrder = mentor.publisher.dataLoader.getNavigationPanelOrder,
            origGetNavigationPanelObjectMap = mentor.publisher.dataLoader.getNavigationPanelObjectMap,
            origGetNavigationPanels = mentor.publisher.dataLoader.getNavigationPanels
        ;
        p.project.getDiagrams = function () {return []};
        p.project.getReports = function () {return []};
        p.project.getId = function () {return "testId"};
        p.project.getObjects = function () {return []};
        p.dataLoader.getNavigationPanelOrder = function () {return []};
        p.dataLoader.getNavigationPanelObjectMap = function () {
            return {
                "testType": {
                    listItems: function () {
                        return [];
                    }
                }
            }
        };
        p.dataLoader.getNavigationPanels = function () {return []};

        Object.keys(resProject).forEach(function (key, index){
            if (key !== "getByType") {
                resProject[key]();
            }
            else {
                resProject[key]("testType");
            }
        });

        expect(resProject.getSystems).toBeDefined();
        expect(resProject.getObjectById).toBeDefined();
        expect(resProject.getReports).toBeDefined();
        expect(resProject.getDiagrams).toBeDefined();
        expect(resProject.getObjects).toBeDefined();

        p.project.getDiagrams = origGetDiagrams;
        p.getReports = origGetReports;
        p.getId = origGetID;
        p.getObjects = origGetObjects;
        p.dataLoader.getNavigationPanelOrder = origGetNavigationPanelOrder;
        p.dataLoader.getNavigationPanelObjectMap = origGetNavigationPanelObjectMap;
        p.dataLoader.getNavigationPanels = origGetNavigationPanels;
    });

    it("should be able find system by name", function () {
        var xmlDataLoaderObject = xmlDataLoader(objectFactoryImpl())
        expect(xmlDataLoaderObject.findSystemByName("testSysName", "testDiagName")).toBeFalsy();
    });

    it("should be able get object by ID and object by name", function () {
        var xmlDataLoaderObject = xmlDataLoader(objectFactoryImpl());
        spyOn(mentor.publisher.objectDataLoader, "findObjectByName");
        xmlDataLoaderObject.getObjectById("testId");
        expect(mentor.publisher.objectDataLoader.findObjectByName).toHaveBeenCalled();

        xmlDataLoaderObject.getObjectByName("testName", "testType", "testDiagramName");
        expect(mentor.publisher.objectDataLoader.findObjectByName).toHaveBeenCalledWith("testName", "testType");

        spyOn(xmlDataLoaderObject, "findSystemByName");
        xmlDataLoaderObject.getObjectByName("testName", "systems", "testDiagramName");
        expect(xmlDataLoaderObject.findSystemByName).toHaveBeenCalledWith("testName", "testDiagramName");
    });

});
