/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */describe("PathResolverTest", function ()
{
    var objectDataLoader;
    beforeEach(function ()
    {
        objectDataLoader = mentor.publisher.objectDataLoader;
        mentor.publisher.objectDataLoader = {
            areUIDsStoredInBuckets: function ()
            {
                return false;
            }
        };
    });

    afterEach(function ()
    {
        mentor.publisher.objectDataLoader = objectDataLoader;
    });

    it("path resolver should be defined.", function ()
    {
        var path = mentor.publisher.pathResolver;
        expect(path).toBeDefined();
    });
    it("should be able to construct cavity table path correctly", function ()
    {
        var path = mentor.publisher.pathResolver.getCavityTable("cavityTable", "systemId", "projectId");
        expect(path).toBe("projectId/systemId/FaceViews/cavityTable");
    });

    it("should be able to construct custom generator file path correctly", function ()
    {
        var path = mentor.publisher.pathResolver.getCustomGeneratorFilePath("customGeneratorName", "projectId",
            "systemUID", "diagramUid");
        expect(path).toBe("projectId/systemUID/diagramUid/customGeneratorName.xml");
    });

    it("should be able to construct system diagram SVG file path correctly", function ()
    {
        var path = mentor.publisher.pathResolver.getDiagramPath("diagramId", "systemId", "diagramUid");
        expect(path).toBe("diagramUid/systemId/diagramId/");
    });

    it("should be able to construct faceview diagram SVG file path correctly", function ()
    {
        var path = mentor.publisher.pathResolver.getFaceViewSymbol("symbolName", "systemId", "diagramUid");
        expect(path).toBe("diagramUid/systemId/FaceViews/symbolName");
    });

    it("should be able to construct language dictionary XML file path correctly", function ()
    {
        var path = mentor.publisher.pathResolver.getGlobalLanguageDictionaryFilePath();
        expect(path).toBe("unzipped/data/langdictionary.xml");
    });
    it("should be able to construct design objects xml file path correctly", function ()
    {
        var path = mentor.publisher.pathResolver.getGlobalObjectFilePath("connector", "projectId");
        expect(path).toBe("projectId/connector.xml");
    });

    it("should be able to construct 2d location view file path correctly", function ()
    {
        var path = mentor.publisher.pathResolver.getLocationViewsFilePath("projectId");
        expect(path).toBe("projectId/LocationViews.xml");
    });
    it("should be able to construct Object UID xml file path correctly", function ()
    {
        var path = mentor.publisher.pathResolver.getObjectPath("systemId", "objectUID", "projectId", "uidBucket");
        expect(path).toBe("projectId/O/uidBucket/objectUID.xml");
    });
    it("should be able to construct vin options xml file path correctly", function ()
    {
        var path = mentor.publisher.pathResolver.getOptionsFile("projectId");
        expect(path).toBe("projectId/options.xml");
    });

    it("should be able to construct system objects file path correctly", function ()
    {
        var path = mentor.publisher.pathResolver.getSystemObjectDataFilePath("projectId", "systenid", "connector");
        expect(path).toBe("projectId/systenid/connector.xml");
    });

    it("should be able to construct packages file path correctly", function ()
    {
        var path = mentor.publisher.pathResolver.getPackagesFilePath();
        expect(path).toBe("unzipped/data/packages.xml");
    });

    it("should be able to construct popover config file path correctly", function ()
    {
        var path = mentor.publisher.pathResolver.getPopoverConfigFilePath("packageRoot");
        expect(path).toBe("packageRoot/Resources/Config/popup.xml");
    });

    it("should be able to construct popover project xml file path correctly", function ()
    {
        var path = mentor.publisher.pathResolver.getProjectXML("packageId");
        expect(path).toBe("packageId/index.xml");
    });

    it("should be able to construct related data order file path correctly", function ()
    {
        var path = mentor.publisher.pathResolver.getRelatedDataOrderFilePath("projectId", "systemid", "diagramid");
        expect(path).toBe("projectId/systemid/diagramid/Resources/RelatedDataOrder.xml");
    });

    it("should be able to construct signal xml file path correctly", function ()
    {
        var path = mentor.publisher.pathResolver.getSignalPath("systemPath", "signalName");
        expect(path).toBe("systemPathSignals/signalName.xml");
    });

    it("should be able to construct objectUID map file path correctly", function ()
    {
        var path = mentor.publisher.pathResolver.getObjectUIDMapPath("systemPath", "projectId");
        expect(path).toBe("projectId/systemPath/O/uidMap.xml");
    });

});

describe("PathResolverWhenUIDsAreGroupdIntoBuckets", function ()
{
    var objectDataLoader, objectFactory;
    beforeEach(function ()
    {
        objectFactory = window.objectFactoryImpl;
        window.objectFactoryImpl = function(){};
        objectDataLoader = mentor.publisher.objectDataLoader;
        mentor.publisher.objectDataLoader = {
            areUIDsStoredInBuckets: function ()
            {
                return true;
            }
        };
    });

    afterEach(function ()
    {
        mentor.publisher.objectDataLoader = objectDataLoader;
        window.objectFactoryImpl = objectFactory;
    });

    it("should be able to construct cavity table path correctly", function ()
    {
        var path = mentor.publisher.pathResolver.getCavityTable("cavityTable", "systemId", "projectId");
        expect(path).toBe("projectId/FaceViews/cavityTable");
    });

    it("should be able to construct object UID xml path correctly", function ()
    {
        var path = mentor.publisher.pathResolver.getObjectUIDMapPath("systemId", "projectId");
        expect(path).toBe("projectId/O/uidMap.xml");
    });

    it("should be able to construct faceview diagram SVG file path correctly", function ()
    {
        var path = mentor.publisher.pathResolver.getFaceViewSymbol("symbolName", "systemId", "diagramUid");
        expect(path).toBe("diagramUid/FaceViews/symbolName");
    });

});