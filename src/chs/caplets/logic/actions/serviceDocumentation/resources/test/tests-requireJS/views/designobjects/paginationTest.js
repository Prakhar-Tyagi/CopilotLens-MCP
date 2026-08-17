/**
 * Created by mukumar on 08-02-2017.
 */
require(["utilities/pagination"], function (Pagination)
{
    describe("pagination module tests", function ()
    {
        var objUnderTest, config = {objectsPerPage: 2};
        beforeEach(function ()
        {
            objUnderTest = Pagination(config);
        });
        it("should be able to load module", function ()
        {
            expect(Pagination).toBeTruthy();
        });
        it("should be able return totalPages", function ()
        {
            objUnderTest.totalObjects = 9;
            var pages = objUnderTest.getTotalPages();
            expect(pages).toBe(5);
        });
        it("should be able return totalPages when number of items and page limit is same", function ()
        {
            objUnderTest.totalObjects = 2;
            var pages = objUnderTest.getTotalPages();
            expect(pages).toBe(1);
        });
        it("should be able return totalPages when number of items is less than page limit is same", function ()
        {
            objUnderTest.totalObjects = 1;
            var pages = objUnderTest.getTotalPages();
            expect(pages).toBe(1);
        });

        it("should be able give start and end index for a page when objects are undefined", function ()
        {
            objUnderTest.totalObjects = undefined;
            var range = objUnderTest.getStartAndEnd();
            expect(range[0]).toBe(0);
            expect(range[1]).toBe(2);
        });

        it("should be able give start and end index when totalObject > page limit", function ()
        {
            objUnderTest.totalObjects = 5;
            objUnderTest.page = 1;
            var range = objUnderTest.getStartAndEnd();
            expect(range[0]).toBe(0);
            expect(range[1]).toBe(2);
        });

        it("should be able give start and end index when totalObject > page limit,page=2", function ()
        {
            objUnderTest.totalObjects = 5;
            objUnderTest.page = 2;
            var range = objUnderTest.getStartAndEnd();
            expect(range[0]).toBe(2);
            expect(range[1]).toBe(4);
        });

        it("should be able give start and end index when totalObject > page limit,page=3", function ()
        {
            objUnderTest.totalObjects = 5;
            objUnderTest.page = 3;
            var range = objUnderTest.getStartAndEnd();
            expect(range[0]).toBe(4);
            expect(range[1]).toBe(5);
        });
        it("reset should set page and objects", function ()
        {
            objUnderTest.totalObjects = 5;
            objUnderTest.page = 3;
            objUnderTest.reset();
            expect(objUnderTest.totalObjects).toBeFalsy();
            expect(objUnderTest.page).toBe(1);
        });
    });

}, function ()
{
    describe("pagination module tests", function ()
    {
        it("module load failed", function ()
        {
            expect(true).toBeFalsy();
        });
    });
})