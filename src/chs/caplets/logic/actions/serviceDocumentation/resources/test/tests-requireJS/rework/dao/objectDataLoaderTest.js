/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

describe("objectDataLoaderTest", function(){
    var p = mentor.publisher, createTestProject;

    createTestProject = function ()
    {
        return {
            loadObjectData: function ()
            {
                return {
                    getCrossReferences: function ()
                    {
                        return {
                            listItems: [{systemId: "testSystemId"}]
                        }
                    }
                }
            }
        };
    };

    it("should be able to return combined xrefs of all object with same name", function(){
        var systemObjects = [{systemId: "testId1", objectId: "objectId1"},
            {systemId: "testId2", objectId: "objectId2"}];
        expect(p.objectDataLoader.findXrefsByObjectId(systemObjects).length).toBe(2);
    });

    it("should be not return  xrefs when system objects does not have systemId or objectId", function(){
        var systemObjects = [{objectId: "objectId1"},
            {systemId: "testId2"}];
        expect(p.objectDataLoader.findXrefsByObjectId(systemObjects).length).toBe(0);
    });

    beforeEach(function(){
        p.objectDataLoader.setProject(createTestProject());
    });


});