/**
 * Created by mukumar on 08-02-2017.
 */
require(["views/designobjects/designObjectsCollection"], function (CollectionFactory)
{
    describe("DesignObjectsCollection tests", function ()
    {
        var config,
                objUnderTest,
                mainthreadCreated,
                workerCreated,
                workerScript,
                urltoload,
                dataloaderResetCalled,
                dataloaderInitilized,
                dataloadExecuteMethodArguments,
                dataloadExecuteMethodCalled;
        afterEach(function ()
        {
            mainthreadCreated = false;
            workerCreated = false;
            dataloaderResetCalled = false;
            dataloaderInitilized = false;
            dataloadExecuteMethodCalled = false;

        });
        beforeEach(function ()
        {

            config = {
                mainthread: function ()
                {
                    var fun = function ()
                    {
                        this.initialize = function (url, script)
                        {
                            workerScript = script;
                            urltoload = url;
                            dataloaderInitilized = true;
                        };
                        this.reset = function ()
                        {
                            dataloaderResetCalled = true;
                        }
                        this.execute = function (parameters)
                        {
                            dataloadExecuteMethodCalled = true;
                            dataloadExecuteMethodArguments = parameters;

                        }
                        this.type = "mainthread";
                    }

                    mainthreadCreated = true;
                    return new fun();
                },
                worker: function ()
                {
                    var fun = function ()
                    {
                        this.initialize = function (url, script)
                        {
                            workerScript = script;
                            urltoload = url;
                            dataloaderInitilized = true;
                        };
                        this.reset = function ()
                        {
                            dataloaderResetCalled = true;
                        }
                        this.execute = function (parameters)
                        {
                            dataloadExecuteMethodCalled = true;
                            dataloadExecuteMethodArguments = parameters;

                        }
                        this.type = "usingwebworker";
                    }
                    workerCreated = true;
                    return new fun();
                }
            };
            var Collection = CollectionFactory(config);
            objUnderTest = new Collection();
        });
        it("should be able to load design object collection", function ()
        {
            expect(CollectionFactory).toBeDefined();
        });

        it("getModels method should retrun all the models in the collection", function ()
        {
            objUnderTest.set({objectId: "testObjectId", systemId: "testSystemId"});
            var models = objUnderTest.getModels();
            expect(models.length).toBe(1);
            expect(models[0].get("objectId")).toBe("testObjectId");
            expect(models[0].get("systemId")).toBe("testSystemId");
        });
        it("filter method should return items without filtering", function ()
        {
            var filteredObject = objUnderTest.filter([{objectId: "testObjectId"}]);
            expect(filteredObject.length).toBe(1);
            expect(filteredObject[0].objectId).toBe("testObjectId")
        });
        it("afterLoad method should reset the collection with new objects", function ()
        {
            var loadedObjects = {size: 2, items: [{objectId: "testobj1"}, {objectId: "testobj2"}]}
            objUnderTest.afterDataLoad(loadedObjects);
            expect(objUnderTest.totalObjects).toBe(loadedObjects.size);
            expect(objUnderTest.getModels().length).toBe(2);
            var models = objUnderTest.getModels();
            expect(models[0].get("objectId")).toBe("testobj1");
            expect(models[1].get("objectId")).toBe("testobj2");
        });
        it("ondataload should execute success callback with data", function ()
        {
            var data = {objectId: "obj1"};
            var dataToCaller;
            objUnderTest.callback = {
                success: function (data)
                {
                    dataToCaller = data;
                }
            };
            objUnderTest.ondataload(data);
            expect(dataToCaller.objectId).toBe("obj1");
        });

        it("getDataURL should construct global objects URL", function ()
        {
            objUnderTest.currentPackage = new (Backbone.Model.extend({}))();
            objUnderTest.currentPackage.set("id", "testId", {silent: true});
            objUnderTest.type = "wires";
            var url = objUnderTest.getDataURL();
            expect(url).toBe("testId/wires.json");
        });

        it("getDataURL should get url from config it getURL method is present in it", function ()
        {
            config.getUrl = function ()
            {
                return "testURL";
            };
            var url = objUnderTest.getDataURL();
            expect(url).toBe("testURL");
        });
        it("should use main thread for file based urls", function ()
        {
            var useWorker = objUnderTest.useWorkerToFetchDataInASeparateThread({href: "file://index.html"});
            expect(useWorker).toBeFalsy();
        });

        it("should use worker thread for http based urls", function ()
        {
            var useWorker = objUnderTest.useWorkerToFetchDataInASeparateThread({href: "http://index.html"});
            expect(useWorker).toBeTruthy();
        });

        it("should use main thread when useSameThreadToLoad is set", function ()
        {
            config.useSameThreadToLoad = true;
            var useWorker = objUnderTest.useWorkerToFetchDataInASeparateThread({href: "http://index.html"});
            expect(useWorker).toBeFalsy();
        });
        it("initialize method should use mainthread when useSameThreadToLoad is true", function ()
        {
            config.useSameThreadToLoad = true;
            config.getUrl = function ()
            {
                return "testUrl";
            }
            objUnderTest.initialize();
            expect(objUnderTest.dataLoader.type).toBe("mainthread");

        });

        it("initialize method should use worker when useSameThreadToLoad is false", function ()
        {
            config.useSameThreadToLoad = false;
            config.getUrl = function ()
            {
                return "testUrl";
            }
            objUnderTest.initialize();
            expect(objUnderTest.dataLoader.type).toBe("usingwebworker");
            expect(workerScript).toBe("s/worker.js");
            expect(urltoload.url).toBe("testUrl");

        });
        it("reset module shoud re-initilize the data", function ()
        {
            dataloaderInitilized = false;
            dataloaderResetCalled = false;
            objUnderTest.resetData({resetData: true});
            expect(dataloaderResetCalled).toBeTruthy();
            expect(dataloaderInitilized).toBeTruthy();
        });
        it("fetchData method should call execute method on dataloader", function ()
        {
            config.useSameThreadToLoad = true;
            var params = {
                method: "getItems",
                data: {objectId: "testObj1"}
            };
            var callback = function ()
            {

            };
            dataloadExecuteMethodCalled = false;
            objUnderTest.fetchData(params, callback);
            expect(dataloadExecuteMethodCalled).toBeTruthy();
            expect(dataloadExecuteMethodArguments.method).toBe(params.method);
            expect(dataloadExecuteMethodArguments.parameters).toBe(objUnderTest.params);
            expect(typeof dataloadExecuteMethodArguments.success).toBe('function');
        });

        it("fetchData method should not initiate another call if one is already in progress", function ()
        {
            // Arrange
            config.useSameThreadToLoad = true;
            var params = {
                method: "getItems",
                data: {objectId: "testObj1"}
            };
            var callback = function ()
            {

            };
            dataloadExecuteMethodCalled = false;
            objUnderTest.inProgress = true;
            expect(objUnderTest.requestQueue.length).toBe(0);

            // Act
            objUnderTest.fetchData(params, callback);

            // Assert
            expect(dataloadExecuteMethodCalled).toBeFalsy();
            expect(objUnderTest.requestQueue.length).toBe(1);
            var requestQueueElement = objUnderTest.requestQueue[0];
            expect(JSON.stringify(requestQueueElement.params)).toBe(JSON.stringify(params));
            expect(requestQueueElement.cb).toBe(callback);
        });

    });

}, function (error)
{
    describe("DesignObjectsCollection tests", function ()
    {
        it("design object collection module failed to load", function ()
        {
            expect(true).toBeFalsy();
        });
    });
})
