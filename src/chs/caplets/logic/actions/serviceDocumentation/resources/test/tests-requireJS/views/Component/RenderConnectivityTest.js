(function () {
    "use strict";

    var context, stubs;

    var IndeterminateProgressDialog = Backbone.View.extend({
        show: function () {

        }
    });

    var languageTranslator = {
        currentLanguage: 'en',
        localize: function (key) {
            const translationData = {
                'TroubleshootingPanel.GenerateDiagram.Progress.Title': 'Generating Diagram',
                'TroubleshootingPanel.GenerateDiagram.Progress.Message': 'Please wait while the diagram is being generated.',
                'Cancel': 'Cancel',
                'TroubleshootingPanel.GenerateDiagram.Progress.ErrorGuidance': 'Error Guidance',
                'TroubleshootingPanel.GenerateDiagram.Progress.ErrorImplication': 'Error Implication'
            };
            return translationData[key] || `Untranslated: ${key}`;
        }
    };

    var mentor = {
        publisher: {
            languageTranslator: languageTranslator,
            project: {
                getId: function () {
                    return 'project123';
                }
            },
            eventDispatcher: {
                dispatchEvent: function () { }
            }
        }
    };

    // Mock RenderConnectivity module
    var RenderConnectivity = {
        displayConnectivity: function (connectivityFile, popOut, flushRenderConnectivity, titleToShow, connectivityUID, designID, isFullInstance) {
            var dialog = new IndeterminateProgressDialog({
                title: mentor.publisher.languageTranslator.localize(
                        'TroubleshootingPanel.GenerateDiagram.Progress.Title'),
                message: mentor.publisher.languageTranslator.localize(
                        'TroubleshootingPanel.GenerateDiagram.Progress.Message'),
                cancel: mentor.publisher.languageTranslator.localize('Cancel'),
                guidance: mentor.publisher.languageTranslator.localize(
                        'TroubleshootingPanel.GenerateDiagram.Progress.ErrorGuidance'),
                implication: mentor.publisher.languageTranslator.localize(
                        'TroubleshootingPanel.GenerateDiagram.Progress.ErrorImplication'),
                onCancelFn: function () {
                    cancelled = true;
                },
            });
            dialog.show();
        }
    };

    stubs = {
        jquery: $,
        underscore: _,
        backbone: Backbone,
        "IndeterminateProgressDialog": IndeterminateProgressDialog,
        "mentor": mentor,
        "RenderConnectivity": RenderConnectivity
    };

    context = createContext(stubs);

    context(["RenderConnectivity"], function (RenderConnectivity) {
        describe("RenderConnectivityTest", function () {
            var dialogShowSpy;

            beforeEach(function () {
                dialogShowSpy = jasmine.createSpy('show');
                IndeterminateProgressDialog.prototype.show = dialogShowSpy;
            });

            it("should show the dialog when displayConnectivity is called", function () {

                const connectivityFile = 'sample.connectivity';
                const popOut = false;
                const flushRenderConnectivity = true;
                const titleToShow = 'Build';
                const connectivityUID = 'uid123';
                const designID = 'designId456';
                const isFullInstance = false;


                RenderConnectivity.displayConnectivity(connectivityFile, popOut, flushRenderConnectivity, titleToShow, connectivityUID, designID, isFullInstance);

                expect(dialogShowSpy).toHaveBeenCalled();
            });
            it("should translate the title correctly", function () {
                expect(languageTranslator.localize('TroubleshootingPanel.GenerateDiagram.Progress.Title')).toBe("Generating Diagram");
            });
        });
    });
})();
