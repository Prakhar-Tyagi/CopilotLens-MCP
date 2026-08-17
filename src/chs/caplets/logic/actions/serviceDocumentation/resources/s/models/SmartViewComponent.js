/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
define(["jquery", "underscore", "backbone"],
        function ($, _, Backbone)
        {
            'use strict'

            var Model, errHandler = function (err)
            {
                var failedId = err.requireModules && err.requireModules[0];
                window.console && window.console.log("Failed to load module " + failedId);
                window.console && window.console.log(err);
            };

            Model = Backbone.Model.extend({
                load: function ()
                {
                    var container,
                            templateLocation,
                            that = this,
                            viewLocation,
                            id;

                    if (that.get('view')) {
                        return;
                    }

                    container = that.get('container');
                    viewLocation = that.get('viewLocation');
                    id = that.get('id');
                    templateLocation = that.get('templateLocation');
                    var modulesToLoad = [viewLocation];
                    if (templateLocation !== "text!") {
                        var modulesToLoad = [viewLocation, templateLocation];
                    }
                    require(modulesToLoad, function (view, template)
                    {
                        var viewObj = view;
                        if (view && view.isViewFactory) {
                            viewObj = new view(id);
                        }
                        viewObj.templateHTML = template;
                        viewObj.container = container;
                        viewObj.id = id;
                        that.set('view', viewObj);
                    }, errHandler);
                }
            });

            return Model;
        }
);