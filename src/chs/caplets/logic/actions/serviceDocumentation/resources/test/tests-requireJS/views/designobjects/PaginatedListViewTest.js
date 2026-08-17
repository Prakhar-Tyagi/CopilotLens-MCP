/**
 * Created by mukumar on 08-02-2017.
 */
require(["views/component/PaginatedListView", "currentPackage"], function (PaginatedListView, currentPackage)
{
    describe("PaginatedListView tests", function ()
    {
        var objUnderTest, data;
        beforeEach(function ()
        {
            data = new (Backbone.Collection.extend({
                getModels: function ()
                {
                    return this.models;
                },
                totalObjects: 2
            }))();
            data.totalObjects = 2;
            data.reset([{objectId: "obj1"}, {objectId: "obj2"}]);
            objUnderTest = new (PaginatedListView(data))();
            objUnderTest.paginationDelegate.itemsPerPage = 1;
            data.searchModel = new (Backbone.Model.extend())();
            data.searchModel.set("searchText", "testDev");
            data.usePlainSearch = false;
            objUnderTest.documentContentBasedFilter = {

                areIndexesGenerated: function (packageId)
                {
                    return true;
                },
                fetchSearchIndexes: function (searchText)
                {
                    expect(searchText).toBe("testDev");
                    return {wires: {id: "wireDocId"}, devices: {id: "docId"}};
                }
            };
            objUnderTest.type = "wires";

        });
        it("should be able to load the module", function ()
        {
            expect(PaginatedListView).toBeDefined();
        });
        it("getDataForTemplate should populate correct data", function ()
        {
            objUnderTest.paginationDelegate.totalObjects = 2;
            var data = objUnderTest.getDataForTemplate({
                expand: false
            });
            expect(JSON.stringify(data)).toBe(
                    '{"page":1,"totalPages":2,"items":[{"objectId":"obj1"},{"objectId":"obj2"}],"expand":false,"totalItems":2}');
        });

        it("should use indexed data to search within documents", function ()
        {
            data.usePlainSearch = false;
            var isIndexGenerated = configureSearchAndCheck();
            expect(isIndexGenerated).toBeTruthy();
        });

        function configureSearchAndCheck()
        {
            objUnderTest.currentPackage = Object.create(currentPackage);
            objUnderTest.currentPackage.set("id", "testId1", {silent: true});
            objUnderTest.documentContentBasedFilter = {

                areIndexesGenerated: function (packageId)
                {
                    expect(packageId).toBe("testId1");
                    return true;
                }
            };
            var isIndexGenerated = objUnderTest.useIndexedSearch();
            return isIndexGenerated;
        }

        it("should use plain data to when it is configured", function ()
        {
            data.usePlainSearch = true;
            var isIndexGenerated = configureSearchAndCheck();
            expect(isIndexGenerated).toBeUndefined();
        });

        it("setIndexes function should set indexed search data correctly", function ()
        {
            data.usePlainSearch = false;

            var dataAttr = {};

            objUnderTest.setSearchIndexes(dataAttr);
            expect(dataAttr.indexes.id).toBe("wireDocId");
        });
        it("should not call fetch data if one is in progress", function ()
        {
            var dataFetched = testFetchData(true);
            expect(dataFetched[0]).toBeFalsy();
        });

        function testFetchData(inpro)
        {
            objUnderTest.inprogress = inpro;
            var dataFetched = [];
            data.fetchData = function (config, callbacks)
            {
                dataFetched = [true, config, callbacks];
            };
            var config = {
                success: function ()
                {

                }
            };
            objUnderTest.paginationDelegate.totalObjects = 3;
            objUnderTest.paginationDelegate.itemsPerPage = 1;
            objUnderTest.fetchData(config);
            return dataFetched;
        }

        it("should  fetch data if one is not in progress", function ()
        {
            var dataFetched = testFetchData();
            expect(dataFetched[0]).toBeTruthy();
        });

        it("fetch data should set state inprogress", function ()
        {
            var dataFetched = testFetchData();
            expect(objUnderTest.inprogress).toBeTruthy();
        });

        it("fetch data should set data start and end index", function ()
        {
            var dataFetched = testFetchData();
            expect(dataFetched[1].start).toBe(0);
            expect(dataFetched[1].end).toBe(1);
        });

        it("fetch data should set search index data", function ()
        {
            var dataFetched = testFetchData();
            expect(dataFetched[1].searchText).toBe("testDev");
            expect(dataFetched[1].isIndexEnabled).toBe(true);
            expect(dataFetched[1].indexes.id).toBe("wireDocId");
        });

        it("fetch data should set correct method name", function ()
        {
            var dataFetched = testFetchData();
            expect(dataFetched[1].method).toBe("getItems");
        });

        it("fetch data should set panel expand state", function ()
        {
            objUnderTest.expanded = true;
            var dataFetched = testFetchData();
            expect(dataFetched[1].header).toBe(true);
            expect(dataFetched[1].expand).toBe(true);
        });
        it("should  fetch data if one is not in progress", function ()
        {
            var dataFetched = testFetchData();
            expect(dataFetched[0]).toBeTruthy();
        });

        it("should  call dataDidFetch on success", function ()
        {
            var dataDidLoadCalled;
            objUnderTest.dataDidFetch = function (data, config, message)
            {
                expect(data.getModels().length).toBe(2);
                expect(data.getModels()[0].attributes.objectId).toBe('obj1');
                expect(data.getModels()[1].attributes.objectId).toBe('obj2');
                expect(config).toBeDefined();
                expect(message.method).toBe("getItems");
                dataDidLoadCalled = true;
            }
            var dataFetched = testFetchData();
            dataFetched[2].success()
            expect(dataDidLoadCalled).toBeTruthy();
        });
        it("should  call dataLoadFailed on error", function ()
        {
            var failed;
            objUnderTest.dataFetchFailed = function ()
            {
                failed = true;
            }
            var dataFetched = testFetchData();
            dataFetched[2].error()
            expect(failed).toBeTruthy();
        });
        it("dataDidFetch should set data correctly and call success callback", function ()
        {
            var afterloadCalled, successCalled;
            var config = {
                success: function (data)
                {
                    successCalled = true;
                    expect(data).toBe("testData")
                }
            }
            objUnderTest.getDataForTemplate = function (message)
            {
                expect(message).toBe("message");
                return "testData";
            }
            var data = {
                afterDataLoad: function (objects)
                {
                    expect(objects.size).toBe(2);
                    expect(objects.items.length).toBe(2);
                    afterloadCalled = true;
                }
            }
            var loadedObjects = {
                size: 2,
                items: [{}, {}]
            };
            objUnderTest.inprogress = true;
            objUnderTest.dataDidFetch(data, config, "message", loadedObjects);
            expect(successCalled).toBeTruthy();
            expect(afterloadCalled).toBeTruthy();
            expect(objUnderTest.inprogress).toBeFalsy();
        });

        it("resetView method should reset data and pagination state", function ()
        {
            var datareset;
            objUnderTest.paginationDelegate.page = 2;
            data.resetData = function ()
            {
                datareset = true;
            }
            objUnderTest.resetView();
            expect(datareset).toBeTruthy();
            expect(objUnderTest.paginationDelegate.page).toBe(1);
        });
        it("show next page", function ()
        {
            objUnderTest.paginationDelegate.page = 2;
            var paginated = false;
            objUnderTest.paginate = function ()
            {
                paginated = true;
            }
            objUnderTest.showNextPage();
            expect(objUnderTest.paginationDelegate.page).toBe(3);
            expect(paginated).toBeTruthy();
        });

        it("show previous page", function ()
        {
            objUnderTest.paginationDelegate.page = 2;
            var paginated = false;
            objUnderTest.paginate = function ()
            {
                paginated = true;
            }
            objUnderTest.showPreviousPage();
            expect(objUnderTest.paginationDelegate.page).toBe(1);
            expect(paginated).toBeTruthy();
        });

        it("paginate should rerender view", function ()
        {
            var rerendered;
            objUnderTest.reRender = function ()
            {
                rerendered = true;
            }
            var obj = {
                stopPropagation: function ()
                {
                    this.called = true;
                }
            }
            objUnderTest.paginate(obj);
            expect(obj.called).toBe(true);
            expect(rerendered).toBe(true);
        });

    });

}, function ()
{
    describe("PaginatedListView tests", function ()
    {
        it("failed to load the module", function ()
        {
            expect(true).toBeFalsy();
        });
    });
});