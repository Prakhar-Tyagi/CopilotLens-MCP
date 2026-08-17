/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/* globals createContext, describe, it, beforeEach, afterEach, expect, Backbone, mentor */
(function ()
{
    'use strict';

    var context,
        currentPackage = new Backbone.Model(),
        preferences = new Backbone.Model(),
        publisher,
        stubs;

    stubs = {
        currentPackage: currentPackage,
        preferences: preferences
    };
    context = createContext(stubs);

    context(
        ['titleupdater'],
        function (titleupdater)
        {
            describe('titleupdater', function ()
            {
                beforeEach(function ()
                {
                    document.title = "";
                    currentPackage.set('title', 'dummy');
                    publisher = mentor.publisher;
                    preferences.set('language', 'en');
                });

                it('should update for package selection screen', function ()
                {
                    titleupdater.startUpdateTitleForPackageSelectionScreen();

                    expect(document.title).toBe('ApplicationTitle');
                });

                it('should update for package  screen', function ()
                {
                    titleupdater.startUpdatingTitleForPackageScreen();

                    expect(document.title).toBe('dummy | ApplicationTitle');
                });

                it('should update when language changes', function ()
                {
                    mentor.publisher = {
                        languageTranslator: {
                            localize: function (text)
                            {
                                return text + "_" + preferences.get('language');
                            }
                        },
                        languageDataLoader: {
                            getLanguageDictionary: function ()
                            {
                                return {
                                    dummy: 'dummy'
                                };
                            },
                            getCurrentLanguage: function ()
                            {
                                return preferences.get('language');
                            }
                        }
                    };

                    titleupdater.startUpdatingTitleForPackageScreen();
                    expect(document.title).toBe('dummy | ApplicationTitle_en');

                    preferences.set('language', 'fr');
                    expect(document.title).toBe('dummy | ApplicationTitle_fr');
                });

                afterEach(function ()
                {
                    mentor.publisher = publisher;
                });
            });
        },
        function (err)
        {

        }
    );
})();