/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global require, describe, it, expect, beforeEach, Backbone, afterEach, createContext*/
(function () {
    var viewUnderTest,
            context,
            router,
            preference,
            UserSession,
            activeSession,
            packagesViewModel,
            Packages,
            Package,
            packages,
            subPackages,
            currentPackage;

    activeSession = new Backbone.Model();
    currentPackage = new Backbone.Model();
    UserSession = {
        getActiveSession: function () {
            return activeSession;
        },
        kSelectedProjectProperty: 'selected-project',
        kSelectedPackageProperty: 'selected-Package'
    };

    router = {
        loadProject: function () {

        }
    };

    preference = Backbone.Model.extend();

    var subPackage1 = {
                name: 'sedan',
                prefix: 'g',
                start: '10',
                end: '15',
                range: 'g10-g15'
            },
            subPackage2 = {
                name: 'sedan',
                prefix: 'a',
                start: '100',
                end: '150',
                range: 'a100-a150'
            },
            subPackage3 = {
                name: 'ab initio',
                prefix: 'f',
                start: '10',
                end: '20',
                range: 'f10-f20'
            },
            subPackage4 = {
                name: 'ab initio',
                prefix: 'f',
                start: '1',
                end: '15',
                range: 'f1-f15'
            };

    subPackages = [subPackage1, subPackage2, subPackage3, subPackage4]

    Package = Backbone.Model.extend({
        initialize: function () {
            this.subPackages = new Backbone.Collection(subPackages);
        }
    });

    var package1 = {
                name: 'package1',
                id: 'package-id1'
            },
            package2 = {
                name: 'package2',
                id: 'package-id2'
            };

    packages = [package1, package2];

    Packages = Backbone.Collection.extend({
        model: Package,
        fetch: function (opts) {
            this.reset(packages);
        },

        containSubPackages: function () {
            var packageArray = this.models;
            return !!(packageArray && packageArray[0] && packageArray[0].subPackages &&
                    packageArray[0].subPackages.length > 0);
        }
    });

    packagesViewModel = Backbone.Model.extend({
        initialize: function () {
            this.packages = new Packages(packages);
            this.subPackages = new Backbone.Collection(subPackages);
        }
    });

    var preferenceInstance = new preference();

    stubs = {
        jquery: $,
        underscore: _,
        backbone: Backbone,
        UserSession: UserSession,
        preferences: preferenceInstance,
        router: router,
        PackagesViewModel: packagesViewModel,
        currentPackage:currentPackage
    };
    context = createContext(stubs);

    context(['views/packagesView'], function (packagesView) {
        viewUnderTest = packagesView;
        describe('PackagesViewTest', function() {
            beforeEach(function () {
                viewUnderTest.$el.html('<div class="package-cell" ' +
                        'data-name="packet1" ' +
                        'data-prefix="f" ' +
                        'data-start="1" ' +
                        'data-end="10" ' +
                        'data-range="f1-f10"></div>');

            });

            afterEach(function () {
                viewUnderTest.$el.html('');
            });

            it('should load the packageView', function () {
                expect(viewUnderTest).toBeDefined();
            });

            it('should highlight element when match found - range match', function () {
                viewUnderTest.highlightExactMatch({searchText: 'f2'});
                expect(viewUnderTest.$el.find('.package-cell').hasClass('highlight')).toBeTruthy();
            });

            it('should highlight element when match found - packet Name complete match', function () {
                var orgMethod = Utils.translate;
                Utils.translate = function (name) {
                    return name;
                };
                viewUnderTest.highlightExactMatch({searchText: 'packet1'});
                expect(viewUnderTest.$el.find('.package-cell').hasClass('highlight')).toBeTruthy();
                Utils.translate = orgMethod;
            });

            it('should not highlight element when partial match found - packet Name partial match', function () {
                viewUnderTest.highlightExactMatch({searchText: 'packet'});
                expect(viewUnderTest.$el.find('.package-cell').hasClass('highlight')).toBeFalsy();
            });

            it('shouldResetIdWhenEffRangeIsNotSameForSamePackageId', function () {
                var idAttr, idvalue, configValue;
                viewUnderTest.shouldResetIdWhenEffRangeIsNotSameForSamePackageId("eff1", "eff2", {
                    set: function (id, value, config) {
                        idAttr = id;
                        idvalue = value;
                        configValue = config;
                    }
                });
                expect(idAttr).toBe('id');
                expect(idvalue).toBe('');
                expect(configValue.silent).toBeTruthy();
            });

            it('should open package when highlighted package is clicked again', function () {
                // Arrange
                var orgMethod = Utils.translate;
                Utils.translate = function (name) {
                    return name;
                };
                var projectOpened, originalLoadProject;
                originalLoadProject = mentor.publisher.router && mentor.publisher.router.loadProject;

                if(!mentor.publisher.router) {
                    mentor.publisher.router = {};
                }

                mentor.publisher.router.loadProject = function () {
                    projectOpened = true;
                }

                // Act
                viewUnderTest.highlightExactMatch({searchText: 'packet1'});

                // Assert
                expect(viewUnderTest.$el.find('.package-cell').hasClass('highlight')).toBeTruthy();

                // Act
                var element = viewUnderTest.$el.find('.package-cell.highlight');
                // todo: Should have used jquery click, but code has check whether the click event is code generated
                viewUnderTest.handleCellClick({
                    currentTarget: element, originalEvent: 'click', stopPropagation: function () {
                    }
                });

                //Assert
                expect(projectOpened).toBeTruthy();

                mentor.publisher.router.loadProject = originalLoadProject;
                Utils.translate = orgMethod;

            });

            it('should be able to render', function () {
                viewUnderTest.initialize();
                viewUnderTest.model={
                    localizedSort: function () {},
                    packages: {
                        containSubPackages: function () {
                            return true;
                        }
                    },
                    subPackages: [],
                    toJSON: function () {
                        return '';
                    }
                };
                viewUnderTest.templateHTML = '<div class="package-cell"></div>';
                var selectedPackage= {
                    name: 'package1',
                    get: function (param) {
                        return '';
                    }
                }
                UserSession.getActiveSession= function () {
                    return {
                        get: function () {return selectedPackage},
                    }
                }
                spyOn(UserSession, 'getActiveSession').andCallThrough();
                viewUnderTest.render();
                expect(UserSession.getActiveSession).toHaveBeenCalled();
            });

        });

    }, function (err) {
        describe('PackagesViewTest-loading-failed', function(){
            it('view loading failed', function () {
                expect(err).toBeFalsy();
            });
        })
    });
})();