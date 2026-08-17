/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, �SISW�), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer�s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/** globals createContext, describe, it */
(function ()
{
    'use strict';

    var context,
        kSelectedProjectProperty,
        Packages,
        packages,
        stubs,
        UserSession,
        activeSession;

    kSelectedProjectProperty = "selected-project";
    activeSession = new Backbone.Model();
    UserSession = {
        getActiveSession: function ()
        {
            return activeSession;
        },
        kSelectedProjectProperty: kSelectedProjectProperty
    };

    packages = [];
    Packages = Backbone.Collection.extend({
        fetch: function (opts)
        {
            this.reset(packages);
        },

        containSubPackages: function(){
            var packageArray = this.models;
            return !!(packageArray && packageArray[0] && packageArray[0].subPackages &&
                    packageArray[0].subPackages.length > 0);
        }
    });

    stubs = {
        jquery: $,
        underscore: _,
        backbone: Backbone,
        UserSession: UserSession,
        Packages: Packages
    };
    context = createContext(stubs);

    function pushPackagesWithNames (names) {
        names.forEach(function (name) {
            packages.push({
                name: name
            });
        });
    }

    context(
        ['viewModels/PackagesViewModel'],
        function (PackagesViewModel)
        {
            describe("PackagesViewModel", function ()
            {
                var previousTraslated;
                beforeEach(function ()
                {
                    packages.length = 0;
                    activeSession.set(kSelectedProjectProperty, new Backbone.Model({
                        id: 'id'
                    }));
                    previousTraslated = Utils.translate;
                    Utils.translate = function (value)
                    {
                        return value + "_traslated";
                    }
                });

                afterEach(function (){
                    Utils.translate = previousTraslated;
                });

                it('should fetch packages when initialized', function ()
                {
                    pushPackagesWithNames(['sedan', 'af one']);

                    var model = new PackagesViewModel();

                    expect(model.packages.toJSON()).toEqual(packages);
                });

                it('should fetch packages when selected project is changed', function ()
                {
                    var model = new PackagesViewModel();
                    expect(model.packages.toJSON()).toEqual([]);

                    pushPackagesWithNames(['sedan', 'af one']);
                    expect(model.packages.toJSON()).not.toEqual(packages);

                    activeSession.trigger("change:" + kSelectedProjectProperty);
                    expect(model.packages.toJSON()).toEqual(packages);
                });

                it('should return JSON representation when toJSON is invoked', function ()
                {
                    pushPackagesWithNames(['sedan', 'af one']);
                    var model = new PackagesViewModel();

                    var actual = model.toJSON().packages.map(function (model) {
                        return model.toJSON();
                    });
                    expect(actual).toEqual(packages);
                });

                it('should perform localized sort when invoked', function ()
                {
                    pushPackagesWithNames(['sedan', 'af one']);
                    var model = new PackagesViewModel();
                    model.localizedSort();

                    expect(model.packages.toJSON()).toEqual(packages.reverse());
                });
            });
        },
        function (err)
        {
            describe("PackagesViewModel", function ()
            {
                it("should load", function ()
                {
                    expect(err).toBeUndefined();
                });
            });
        }
    )
})();