/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

describe("locationViews DAO", function ()
{
    var p = mentor.publisher;

    it("getLocationViewName should handle linux paths", function ()
    {
        expect(mentor.publisher.locationViews.getLocationViewName('a/b/name.svg')).toBe('name');
    });

    it("should be able to load", function ()
    {
        var backgroundTemplate = '<div id="root"><div class="LocationViews"><div class="LocationView"></div></div></div>',
            origLoadXMLByAjax = p.xmlLoader.loadXMLByAjax
            ;
        $("body").html(backgroundTemplate);

        p.xmlLoader.loadXMLByAjax = function () {
            return {
                data: {}
            };
        };

        spyOn(p.xmlLoader, 'loadXMLByAjax').andCallThrough();
        mentor.publisher.locationViews.load();
        expect(p.xmlLoader.loadXMLByAjax).toHaveBeenCalled();

        p.xmlLoader.loadXMLByAjax = origLoadXMLByAjax;
    });

    it("should be able to get location views", function ()
    {
        expect(mentor.publisher.locationViews.getLocationViews()).toEqual([]);
    });

    it("should be able to get location view by view name", function ()
    {
        var origGetLocationViews = p.locationViews.getLocationViews;
        p.locationViews.getLocationViews = function () {return [{name: 'testName'}];};

        expect(mentor.publisher.locationViews.getLocationViewByViewName('testName')).toEqual({name: 'testName'});

        p.locationViews.getLocationViews = origGetLocationViews;
    });

    it("should be able to get location view by name", function ()
    {
        var origGetUIDsFor = mentor.publisher.nameToUIDMap.getUIDsFor;
        mentor.publisher.nameToUIDMap.getUIDsFor = function () {
            return [
                {
                    relatedDocuments: [
                        {
                            documents: [
                                {
                                    name: 'testName'
                                }
                            ]
                        }
                    ]
                }
            ];
        };

        expect(mentor.publisher.locationViews.locationViewByName('testObjectName')).toEqual(
            {
                systems : undefined,
                path : [{name : 'testName', id : undefined}]
            }
        );

        mentor.publisher.nameToUIDMap.getUIDsFor = origGetUIDsFor;
    });

    it("should be able to check if the object links within 2d view", function ()
    {
        expect(mentor.publisher.locationViews.doesObjectHasLinksWithin2dView('testObjectName')).toBeTruthy();
    });

    it("should be able to get location view by object Id", function ()
    {
        expect(mentor.publisher.locationViews.getLocationViewByObjectId('testObjectId')).toBe("");
    });

});