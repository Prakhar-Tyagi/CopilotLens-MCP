/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global define, mentor, renderer, $, setTimeout, require, Utils*/
define(["backbone", "underscore", "jquery", "models/selectedSystem", "views/contentpanel/toolbar/contentToolBar",
            "currentPackage", "internalLinkHandler"],
        function (Backbone, _, $, selectedSystem, Toolbar, currentPackage, internalLinkHandler) {
            "use strict";

            var DiagnosticsPanel = Backbone.View.extend({
                events: {
                    "click .closeBtn": "close",
                    "click .popOutBtn": "onPopoutButtonClick",
                    "click .goto": "goto"
                },

                type: mentor.publisher.contentType.DIAGNOSTIC,

                close: function () {
                    selectedSystem.set("diagnostic", "", {silent: true});

                    this.undelegateEvents();
                    this.$el.html('');
                },

                onPopoutButtonClick: function (event) {
                    event.stopPropagation();

                    var diagnostic = selectedSystem.get("diagnostic");
                    if (!diagnostic) {
                        return;
                    }

                    var packageTitle = encodeURIComponent(currentPackage.get("title"));
                    var diagnosticTitle = encodeURIComponent(Utils.translate(diagnostic.title));

                    var diagnosticURL = "popout.html?project=" + packageTitle + "&view=diagnostic&viewName=" +
                            diagnosticTitle;
                    var effSetter = require("filehandlers/effectivitySetter");
                    mentor.publisher.popoutHandler.openPopout(
                            effSetter.addEffAndProjectIdInURLs(diagnosticURL)
                    );
                },

                goto: function (event) {
                    var tableTop,
                            procedure = $(event.currentTarget).attr('data-goto'),
                            that = this,
                            trTop;

                    tableTop = that.$("#" + procedure).closest("table").offset().top;
                    trTop = that.$("#" + procedure).closest("tr").offset().top;

                    that.$("#systemDiagnosticLoadArea").scrollTop(trTop - tableTop + 74);

                    event.preventDefault();
                },

                initialize: function () {
                    currentPackage.on("change:language", this.render, this);
                    currentPackage.on("change:vin", this.render, this);

                    selectedSystem.on("change:optionExpression", this.render, this);
                    selectedSystem.on("change:diagnostic", this.render, this);
                },

                clearContainer: function () {
                    this.setElement(this.container);
                    this.$el.html('');
                },

                render: function () {
                    var toolbar, template, that = this;

                    var diagnostic = selectedSystem.get("diagnostic");
                    if (diagnostic) {
                        that.clearContainer();

                        mentor.publisher.contentArea.closeExistingPanel({
                                    type: that.type,
                                    systemId: selectedSystem.get("systemId")
                                },
                                that
                        );

                        var diagnosticWithMissingAttrs = _.extend({folder: undefined}, diagnostic);

                        //compile template
                        template = _.template(that.templateHTML)(diagnosticWithMissingAttrs);

                        //create toolbar
                        toolbar = new Toolbar();
                        that.$el.append(
                                toolbar.render({
                                    type: that.type,
                                    title: diagnostic.title
                                }).$el
                        );

                        //add content
                        that.$el.append(template);
                        setTimeout(function () {
                            internalLinkHandler.addMouseEventListener(that.container);
                            mentor.publisher.contentArea.layoutContentPanel({
                                type: that.type,
                                title: diagnostic.title,
                                systemId: selectedSystem.get("systemId"),
                                id: diagnostic.id,
                            });
                            //$(".popOutBtn", that.$el).hide();
                        }, 10);
                    }
                    else {
                        that.$el.html("");
                    }

                    return that;
                }
            });

            return new DiagnosticsPanel();
        });


