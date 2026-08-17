require
(
        ["views/designobjects/loader/mainthread"],
        function (Mainthread)
        {
            var objectUnderTest,
                    methodArguments,
                    dataLoaderDidReset;

            function createMockDataLoader()
            {
                var config = {
                    dataLoaderFactory: function ()
                    {
                        return {
                            init: function (messageArr)
                            {
                                methodArguments.push(messageArr);
                            },
                            reset: function ()
                            {
                                methodArguments.push("reset");
                                dataLoaderDidReset = true;
                            },
                            getItems: function (methodName, messageArr, callback)
                            {
                                methodArguments.push(messageArr);
                                var sampleResult = "data"
                                callback(sampleResult);
                            }
                        }
                    }
                }
                return config;
            };

            describe("mainthread test specs", function ()
            {

                beforeEach(function ()
                {
                    dataLoaderDidReset = false;
                    methodArguments = [];
                    var config = createMockDataLoader();
                    objectUnderTest = Mainthread(config);
                    objectUnderTest.url = "testURL";

                });
                it("should be able to load mainthread module", function ()
                {
                    expect(Mainthread).toBeDefined();
                });

                it("init method should initialize data loader by sending init message", function ()
                {
                    objectUnderTest.initializeDataLoader();

                    expect(methodArguments[0][0]).toBe('init');
                    expect(methodArguments[0][1]).toBe('testURL?packageId=12da'); // mentor.publisher.packetInfo.packageId = '12da'
                });

                it("init method should set ajax method on data loader", function ()
                {
                    expect(objectUnderTest.dataLoader.ajax).toBeUndefined();
                    objectUnderTest.initializeDataLoader();
                    expect(objectUnderTest.dataLoader.ajax).toBeDefined();
                });

                it("reset method should re-initialize data loader", function ()
                {
                    objectUnderTest.reset();
                    expect(methodArguments[0]).toBe('reset');
                    expect(methodArguments[1][0]).toBe('init');
                    expect(methodArguments[1][1]).toBe('testURL?packageId=12da');

                });

                it("execute method should call appropriate method in data loader with correct parameters", function ()
                {
                    var config = {};
                    var ounderT = Object.create(objectUnderTest);
                    var onmessageCalled;
                    var isWaiting = true;
                    ounderT.onmessage = function (data)
                    {
                        expect(data).toBe('data');
                        onmessageCalled = true;

                    }
                    config.method = "getItems";
                    config.timeout = 0;
                    config.success = function ()
                    {

                    };
                    config.parameters = ["testParameters"];
                    runs(function() {
                        ounderT.execute(config);
                        setTimeout(function() {
                            isWaiting = false;
                        }, 100);
                    });

                    waitsFor(function() {
                        return !isWaiting;
                    }, 2000);

                    runs(function() {
                        expect(onmessageCalled).toBeTruthy();
                        expect(methodArguments[0][0]).toBe('testParameters');
                    });
                });

                it("onmessage method should call success callback when data is fetched without any error", function ()
                {
                    var config = {};
                    var ounderT = Object.create(objectUnderTest);
                    var successCallbackcalled;
                    ounderT.parameters = {};
                    ounderT.parameters.success = function (data)
                    {
                        expect(data).toBe('loadedData');
                        successCallbackcalled = true;

                    }

                    ounderT.parameters.method = "getItems";
                    ounderT.onmessage(["getItems", "loadedData"]);
                    expect(successCallbackcalled).toBeTruthy();

                });

                it("onmessage method should call error callback when data fetching failed", function ()
                {
                    var config = {};
                    var ounderT = Object.create(objectUnderTest);
                    var errorCallbackcalled;
                    ounderT.parameters = {};
                    ounderT.parameters.error = function ()
                    {

                        errorCallbackcalled = true;

                    }
                    ounderT.parameters.method = "getItems";
                    ounderT.onmessage([]);
                    expect(errorCallbackcalled).toBeTruthy();

                });

            });
        },
        function (error)
        {
            describe("Failed to load mainthread.js module", function ()
            {
                expect(error).toBeFalsy();
                expect(true).toBeFalsy();
            });
        }
);