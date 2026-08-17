(function () {
    var Model = Backbone.Model.extend();
    var contentToTest;
    var stubs = {
        "fileDisplayHandler": {
            display: function (content)
            {
                contentToTest = content;
            }
        },
        jquery : $,
        underscore : _,
        backbone : Backbone,
        PackagesInSession: new Backbone.Model()
    };

    var context = createContext(stubs);
    var orig_getPluginType;

    /*
    * Data for following test comes from main-test.js (mentor.publisher.project#getByType())
    * */
    context(['routers/projectDataRouter', 'fileDisplayHandler'], function(systemUnderTest, displayHandler) {

        describe("ProjectDataRouterTest", function() {
            beforeEach(function() {
                orig_getPluginType = getPluginType;
                contentToTest = null;
            });

            afterEach(function() {
                getPluginType = orig_getPluginType;
            });

            it("PDF content should invoke fileDisplayHandler with custom content type", function() {
                getPluginType = function(path) {
                    if (path.indexOf('.pdf') !== -1) {
                        return 'application/pdf';
                    }
                    else if (path.indexOf('.svg') !== -1) {
                        return "image/svg+xml";
                    }
                    else {
                        return "text/html";
                    }
                }

                runs(function() {
                    systemUnderTest.openComponent({
                        componentType: 'locationviews',
                        contentType: 'locationviews',
                        componentName: "Test Pdf"
                    });
                });

                waitsFor(function(){
                    return !!contentToTest;
                }, 1000);

                runs(function() {
                    expect(contentToTest.type).toBe(mentor.publisher.contentType.CUSTOM_VIEW);
                    expect(contentToTest.id).toBe("Test Pdf");
                });
            });

            it("SVG content should invoke fileDisplayHandler with location view content type", function() {
                runs(function() {
                    systemUnderTest.openComponent({
                        componentType: 'locationviews',
                        contentType: 'locationviews',
                        componentName: "customFIle"
                    });
                });

                waitsFor(function(){
                    return !!contentToTest;
                }, 1000);

                runs(function() {
                    expect(contentToTest.type).toBe(mentor.publisher.contentType.LOCATION_VIEWS);
                    expect(contentToTest.id).toBe("customFIle");
                });
            });

            it("fileHandler should get invoked with actual model id and type", function() {
                runs(function() {
                    systemUnderTest.openComponent({
                        componentType: 'locationviews',
                        componentName: "DTC047"
                    });
                });

                waitsFor(function(){
                    return !!contentToTest;
                }, 1000);

                runs(function() {
                    expect(contentToTest.type).toBe(mentor.publisher.contentType.DIAGNOSTIC);
                    expect(contentToTest.id).toBe("UIDd23b1d-15167448921-2e9e9f7c017ee2a1645a236d182fb28c");
                });

            });
        })
    }, function (err) {
        describe("ProjectDataRouterTest - module load Error", function ()
        {
            it("Module load failed", function ()
            {
                console.log(err.message + "::\n" + err.stack);
                expect(false).toBeTruthy();
            });
        });
    })
})();