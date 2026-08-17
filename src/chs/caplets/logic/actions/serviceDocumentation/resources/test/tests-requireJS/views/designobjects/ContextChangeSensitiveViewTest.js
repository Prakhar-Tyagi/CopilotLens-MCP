/**
 * Created by mukumar on 08-02-2017.
 */
require(["views/component/ContextChangeSensitiveView", "currentPackage"],
        function (View, currentPackage)
        {
            describe("ContextChangeSensitiveView tests", function ()
            {
                var objUnderTest,
                        viewReset,
                        viewRerendered,
                        config;

                beforeEach(function ()
                {
                    config = {
                        applyOptionFilter: true,
                        applyPackageChange: true,
                        applyLanguageChange: true,
                        applySearchFilter: true
                    };
                    objUnderTest = View(config);
                    objUnderTest.resetView = function ()
                    {
                        viewReset = true;
                    }

                    objUnderTest.reRender = function ()
                    {
                        viewRerendered = true;
                    }
                    objUnderTest.initialize();
                });
                afterEach(function ()
                {
                    viewReset = false;
                    viewRerendered = false;
                });
                it("should be able to load module", function ()
                {
                    expect(View).toBeDefined();
                });
                function triggerEventAndValidateState(eventName)
                {
                    viewReset = false;
                    viewRerendered = false;
                    currentPackage.trigger(eventName);
                    expect(viewReset).toBeTruthy();
                    expect(viewRerendered).toBeTruthy();
                }

                it("context change event should  reset and render the view", function ()
                {
                    triggerEventAndValidateState("change:id");
                    triggerEventAndValidateState("change:vin");
                    triggerEventAndValidateState("change:searchText");
                    triggerEventAndValidateState("change:language");
                });

                it("context change event should not reset and render the view when its not configured for the change",
                        function ()
                        {
                            objUnderTest = View();
                            var viewReset = false;
                            objUnderTest.resetView = function ()
                            {
                                viewReset = true;
                            }
                            viewReset = false;

                            currentPackage.trigger("change:vin");
                            expect(viewReset).toBeFalsy();

                        });

            });

        }, function (err)
        {
            describe("ContextChangeSensitiveView tests", function ()
            {
                it("failed to load module", function ()
                {
                    console.log(err);
                    expect(true).toBeFalsy();
                });
            });
        });