require
(
        ["views/designobjects/loader/webworker"],
        function (Webworker)
        {
            var objectUnderTest,
                    methodArguments,
                    dataLoaderDidReset;

            function createMockDataLoader(workerscript)
            {

                this.workerscript = workerscript;
                this.postMessage = function (messageArr)
                {
                    methodArguments.push(messageArr);
                };

                this.init = function (messageArr)
                {
                    methodArguments.push(messageArr);
                };

                this.reset = function ()
                {
                    methodArguments.push("reset");
                    dataLoaderDidReset = true;
                };

                this.getItems = function (methodName, messageArr, callback)
                {
                    methodArguments.push(messageArr);
                    var sampleResult = "data"
                    callback(sampleResult);
                };
            }

            describe("webworker test specs",
                    function ()
                    {

                        beforeEach(function ()
                        {
                            dataLoaderDidReset = false;
                            methodArguments = [];

                            objectUnderTest = Webworker(config);

                            var config = {
                                Worker: createMockDataLoader,
                                url: "testURL\\wires.json",
                                onerror: function ()
                                {

                                },
                                onmessage: function ()
                                {

                                }
                            };
                            objectUnderTest.initialize(config, "workerscript.js");

                        });
                        it("should be able to load webworker module",
                                function ()
                                {
                                    expect(Webworker).toBeDefined();
                                });

                        it("init method should initialize data loader by sending init message",
                                function ()
                                {
                                    expect(methodArguments[0][0]).toBe('init');
                                    expect(methodArguments[0][1]).toBe('testURL/wires.json?packageId=12da');
                                });

                        it("webworker should use workerscript to perform tasks",
                                function ()
                                {
                                    expect(objectUnderTest.dataLoader.workerscript).toBe('workerscript.js');
                                });

                        it("reset method should re-initialize data loader",
                                function ()
                                {
                                    expect(methodArguments.length).toBe(1);
                                    objectUnderTest.reset();
                                    expect(methodArguments[0][0]).toBe('init');
                                    expect(methodArguments[0][1]).toBe('testURL/wires.json?packageId=12da');
                                    expect(methodArguments[1][0]).toBe('reset');

                                });

                        it("execute method should call appropriate method in data loader with correct parameters",
                                function ()
                                {
                                    var config = {};
                                    var ounderT = Object.create(objectUnderTest);
                                    config.method = "getItems";
                                    config.success = function ()
                                    {

                                    };
                                    config.parameters = "arguments"
                                    ounderT.execute(config);
                                    expect(methodArguments[1][0]).toBe('getItems');
                                    expect(methodArguments[1][1]).toBe(config.parameters);
                                    expect(ounderT.parameters.success).toBe(config.success);
                                });
                        it("onmessage method should call success callback when data is fetched without any error",
                                function ()
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
                                    ounderT.onmessage({data: ["getItems", "loadedData"]});
                                    expect(successCallbackcalled).toBeTruthy();

                                });

                        it("onmessage method should call error callback when data fetching failed",
                                function ()
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
                                    ounderT.onmessage({data: []});
                                    expect(errorCallbackcalled).toBeTruthy();

                                });
                        afterEach(function ()
                        {
                            methodArguments = [];
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
)
;
