/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, �SISW�), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer�s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/** globals createContext, describe, it */
(function(){
    "use strict";

    var context,
        Packages,
        packages,
        Package,
        SubPackages,
        subPackages,
        stubs,
        UserSession,
        activeSession,
        ViewModelUnderTest;

    activeSession = new Backbone.Model();
    UserSession = {
        getActiveSession: function(){
            return activeSession;
        },
        kSelectedProjectProperty: 'selected-project',
        kSelectedPackageProperty: 'selected-Package'
    };

    packages = [{
        name: 'sedan'
    }];

    var subPackage1 = {
        name: 'sedan',
        prefix: 'g',
        start: '10',
        end: '15'
    },
    subPackage2 = {
        name: 'sedan',
        prefix: 'a',
        start: '100',
        end: '150'
    },
    subPackage3 = {
        name: 'ab initio',
        prefix: 'f',
        start: '10',
        end: '20'
    },
    subPackage4 = {
        name: 'ab initio',
        prefix: 'f',
        start: '1',
        end: '15'
    };

    subPackages = [subPackage1, subPackage2, subPackage3, subPackage4]
    SubPackages = Backbone.Collection.extend({
        fetch: function(){
            this.reset(subPackages);
        }
    })

    Package = Backbone.Model.extend({
        initialize: function(){
            this.subPackages = new SubPackages(subPackages);
        }
    })

    Packages = Backbone.Collection.extend({
        model: Package,
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

    context(['viewModels/PackagesViewModel'], function (PackagesViewModel){
        ViewModelUnderTest = PackagesViewModel;
        describe("packagesViewModelWithEffectivityTest", function() {
            var previousTraslated;
            beforeEach(function (){
                previousTraslated = Utils.translate;
                Utils.translate = function (value)
                {
                    return value + "_traslated";
                }
                activeSession.set(UserSession.kSelectedProjectProperty, new Backbone.Model({
                    id: 'id'
                }));

                activeSession.set(UserSession.kSelectedPackageProperty, new Backbone.Model({
                    id: mentor.publisher.packectInfo.packageId
                }));
            });

            afterEach(function (){
                Utils.translate = previousTraslated;
            });

            it('should filter subpackages with given SearchText - package Name partial match', function(){
                var event = new CustomEvent('dummyEvent', {
                    detail: {
                        searchText: 'ab'
                    }
                })

                var model = new ViewModelUnderTest();
                var isHighlightExactMatchTriggered;
                model.on('highlightExactMatch', function(){
                    isHighlightExactMatchTriggered = true;
                });
                model.filterPackages(event);

                var filteredModels = model.subPackages.models;

                var expectedFilteredModels = [subPackage3, subPackage4];
                expect(filteredModels.length).toBe(2);
                expect(isHighlightExactMatchTriggered).toBeTruthy();
                expect(JSON.stringify(filteredModels.sort())).toBe(JSON.stringify(expectedFilteredModels.sort()));
            });

            it('should filter subpackages with given SearchText - package Name full match', function(){
                var event = new CustomEvent('dummyEvent', {
                    detail: {
                        searchText: 'ab initio'
                    }
                })

                var model = new ViewModelUnderTest();
                model.filterPackages(event);

                var filteredModels = model.subPackages.models;

                var expectedFilteredModels = [subPackage3, subPackage4];
                expect(filteredModels.length).toBe(2);
                expect(JSON.stringify(filteredModels.sort())).toBe(JSON.stringify(expectedFilteredModels.sort()));
            });

            it('should filter subpackages with given SearchText - range match', function(){
                var event = new CustomEvent('dummyEvent', {
                    detail: {
                        searchText: 'f9'
                    }
                })

                var model = new ViewModelUnderTest();
                model.filterPackages(event);

                var filteredModels = model.subPackages.models;

                var expectedFilteredModels = [subPackage4];
                expect(filteredModels.length).toBe(1);
                expect(JSON.stringify(filteredModels.sort())).toBe(JSON.stringify(expectedFilteredModels.sort()));
            });

            it('should filter subpackages with given SearchText - range match with overlapping effectivity', function(){
                var event = new CustomEvent('dummyEvent', {
                    detail: {
                        searchText: 'f11'
                    }
                })

                var model = new ViewModelUnderTest();
                model.filterPackages(event);

                var filteredModels = model.subPackages.models;

                var expectedFilteredModels = [subPackage3, subPackage4];
                expect(filteredModels.length).toBe(2);
                expect(JSON.stringify(filteredModels.sort())).toBe(JSON.stringify(expectedFilteredModels.sort()));
            });

            it('should filter subpackages with given SearchText - No match', function(){
                var event = new CustomEvent('dummyEvent', {
                    detail: {
                        searchText: 'z9'
                    }
                })

                var model = new ViewModelUnderTest();
                model.filterPackages(event);

                var filteredModels = model.subPackages.models;

                var expectedFilteredModels = [];
                expect(filteredModels.length).toBe(0);
                expect(JSON.stringify(filteredModels.sort())).toBe(JSON.stringify(expectedFilteredModels.sort()));
            });

            it('empty searchText resets selectedPackage from ActiveSession', function(){
                var event = new CustomEvent('dummyEvent', {
                    detail: {
                        searchText: ''
                    }
                })

                var model = new ViewModelUnderTest();
                expect(UserSession.getActiveSession().get(UserSession.kSelectedPackageProperty)).toBeTruthy();

                model.filterPackages(event);

                var filteredModels = model.subPackages.models;
                var expectedFilteredModels = [subPackage1, subPackage2, subPackage3, subPackage4];

                expect(filteredModels.length).toBe(4);
                expect(JSON.stringify(filteredModels.sort())).toBe(JSON.stringify(expectedFilteredModels.sort()));
                expect(UserSession.getActiveSession().get(UserSession.kSelectedPackageProperty)).toBeFalsy();
            });
        });
    });
})();