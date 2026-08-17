/**
 * Created by kayyagar on 22-01-2016.
 */
require(['backbone', "SVGTransformModel", "ZoomAndPanModule", "ZoomToolBarModel", "PersistenceModelFactory"],
        function (Backbone, SVGTransformModel, zoomAndPanModule, zoomToolBarModel, factory)
        {
            describe("SVGTransformModelTest", function ()
            {
                "use strict"
                var originalFit, originalPanToMiddle, originalBringToFront, originalCreateCompatibleModel, TestableSVGTransformModel;

                beforeEach(function ()
                {
                    originalFit = zoomAndPanModule.fit;
                    originalPanToMiddle = zoomAndPanModule.panToMiddle;
                    originalBringToFront = zoomAndPanModule.bringToFront;
                    originalCreateCompatibleModel = factory.createCompatibleModel;
                    mentor.publisher.constants.PositiveZoomPerUnit = 1.164993050750713;
                    mentor.publisher.constants.NegetiveZoomPerUnit = 0.858374218932557;
                });

                afterEach(function ()
                {
                    zoomAndPanModule.fit = originalFit;
                    zoomAndPanModule.panToMiddle = originalPanToMiddle;
                    zoomAndPanModule.bringToFront = originalBringToFront;
                    factory.createCompatibleModel = originalCreateCompatibleModel;
                });

                it("model should fit by calling zoom and pan modules fit", function ()
                {
                    var called = false;
                    zoomAndPanModule.fit = function ()
                    {
                        called = true;
                    };
                    (new SVGTransformModel()).fit();
                    expect(called).toBe(true);
                });

                it("model should fit locked view by fetching model from the persistence and calling zoom and pan modules fit",
                        function ()
                        {
                            var called = false;
                            zoomAndPanModule.fit = function ()
                            {
                                called = true;
                            };
                            TestableSVGTransformModel = SVGTransformModel.extend({
                                fetch: function (options)
                                {
                                    this.set({'scale': 1.5, 'svgContainerId': 'testContainer'});
                                    options.success();
                                }
                            });
                            var model = new TestableSVGTransformModel();
                            zoomToolBarModel.set('testContainer', new Backbone.Model());
                            (model).fitLockedView();
                            expect(called).toBe(true);
                            expect(model.get('scale')).toBe(1.5);
                            expect(model.get('zoomScale')).toBe(2.145935547331392);
                            expect(zoomToolBarModel.get('testContainer').get('currentZoomLevel')).toBe(150);
                            expect(zoomToolBarModel.get('testContainer').get('lockState')).toBe(true);
                            expect(zoomToolBarModel.get('testContainer').get('lockedZoomLevel')).toBe(150);
                        });

                it("model should pan to middle by fetching model from the persistence and calling zoom and pan modules pan to middle",
                        function ()
                        {
                            var called = false;
                            zoomAndPanModule.panToMiddle = function ()
                            {
                                called = true;
                            };
                            TestableSVGTransformModel = SVGTransformModel.extend({
                                fetch: function (options)
                                {
                                    this.set({'scale': 1.5, 'svgContainerId': 'testContainer'});
                                    options.success();
                                }
                            });
                            var model = new TestableSVGTransformModel();
                            zoomToolBarModel.set('testContainer', new Backbone.Model());
                            (model).panToMiddle([]);
                            expect(called).toBe(true);
                            expect(model.get('scale')).toBe(1.5);
                            expect(model.get('zoomScale')).toBe(2.145935547331392);
                            expect(zoomToolBarModel.get('testContainer').get('currentZoomLevel')).toBe(150);
                            expect(zoomToolBarModel.get('testContainer').get('lockState')).toBe(true);
                            expect(zoomToolBarModel.get('testContainer').get('lockedZoomLevel')).toBe(150);
                        });

                it("model should save the model to the persistence by saving id and scale",
                        function ()
                        {
                            TestableSVGTransformModel = SVGTransformModel.extend({
                                save: function (attributes)
                                {
                                    this.set(attributes);
                                }
                            });
                            var model = new TestableSVGTransformModel({
                                'svgContainerId': 'testContainer',
                                'id': 'testContainer'
                            });
                            zoomToolBarModel.set('testContainer', new Backbone.Model());
                            zoomToolBarModel.get('testContainer').set('currentZoomLevel', 170);
                            (model).saveZoomLevel();
                            expect(model.get('id')).toBe('testContainer');
                            expect(model.get('scale')).toBe(1.7);
                            expect(zoomToolBarModel.get('testContainer').get('currentZoomLevel')).toBe(170);
                            expect(zoomToolBarModel.get('testContainer').get('lockState')).toBe(true);
                            expect(zoomToolBarModel.get('testContainer').get('lockedZoomLevel')).toBe(170);
                        });
            });
        });