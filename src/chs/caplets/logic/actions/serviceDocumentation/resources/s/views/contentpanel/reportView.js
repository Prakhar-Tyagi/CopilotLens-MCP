/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 *//*global define, mentor, setTimeout, document, require, $, Utils*/
define(
    ["backbone", "underscore", "views/contentpanel/designReportPanel", "preferences", "models/selectedSystem"],
    function (Backbone, underscore, DesignReportPanel, preferences, selectedSystem)
    {
        "use strict";
		return DesignReportPanel.extend({
			isActive: false,
			init: function ()
			{
				selectedSystem.on("change:" + this.getContentType(), function ()
				{
					this.render();
				}, this);
				preferences.on("change:language", this.updateTitle, this);
				preferences.on("change:language", this.reRender, this);
				this.isActive = false;
			},
			close: function ()
			{
				DesignReportPanel.prototype.close.call(this);
				this.isActive = false;
			},
			reRender: function ()
			{
				if (this.selectedSystem.get("harness") && this.selectedSystem.get("harness").get("path")) {
                    this.renderGlobalReport && this.renderGlobalReport();
				}
				else {
					this.render();
				}
			},

			isPanelActive: function ()
			{
				return selectedSystem.get(this.getContentType());
			},

			showDiagramBtn: function ()
			{
				this.isDiagramOpen = false;
				if (this.toolBar && this.toolBar.layoutButtons) {

					this.toolBar.layoutButtons.enableDocumentSets([".diagrams-button"]);
					this.deActivateInValidDocuments();
				}
			},
			hideDiagramBtn: function ()
			{
				this.isDiagramOpen = true;
				this.deActivateInValidDocuments();
			},
			getContentType: function ()
			{
				return mentor.publisher.contentType.SYSTEM_REPORT;
			},

			resetModel: function ()
			{
				var type = this.getContentType();
				this.selectedSystem.set(type, "", {silent: true});
			},

			getDocumentContainer: function ()
			{
				var conatainer = this.container;
				return $(conatainer + " .panel_content").attr('id');
			},

			showPopout: function (event)
			{
				var report, type;
				type = this.getContentType();
				if (this.selectedSystem.get(type)) {
					report = this.selectedSystem.get(type);
					mentor.publisher.popoutHandler.openPopout("popout.html#/" + (type || "").toLowerCase() + "/" +
					this.selectedSystem.get("harnessLayoutId") + "/" + report.get("mainText") + "/" +
					this.currentPackage.get("id").replace("\\", "/"));
				}
			},

			processReport: function (containerId, systemId)
			{
				var that = this;
				this.systemReportLoadFinished(this.getDocumentContainer(), systemId);
				this.applyLanguageFilterOnReport(containerId);
				setTimeout(function ()
				{
					that.deActivateInValidDocuments();
				}, 10);

			},
			getReport: function ()
			{
				var system = mentor.publisher.project.get(this.selectedSystem.get("systemId")), report;
				this.system = system;
				report = system.get(this.selectedSystem.get("reportId"));
				this.report = report;
				return report || {};
			},

			isReportActive: function ()
			{
				return this.isActive;
			},

			deActivateInValidDocuments: function ()
			{
				if (this.toolBar) {

					this.toolBar.layoutButtons.disableDocumentSets(['.related-data-button', ".renderConnectivityBtn"]);
					if (!this.isDocumentTypeActive("diagrams") || this.isDiagramOpen) {
						this.toolBar.layoutButtons.disableDocumentSets([".diagrams-button"]);
					}

					if (!this.isDocumentTypeActive("reports")) {
						this.toolBar.layoutButtons.disableDocumentSets([".reports-button"]);
					}
				}
			}, getContentPanelData: function (type, that, title, path)
            {
                return {
                    type: type,
                    systemId: that.selectedSystem.get("systemId"),
                    title: title,
                    path: path
                };
            }, loadReportHTML: function (pathPrepend, path, that, type, title)
			{
				this.loadModule()(["text!" + pathPrepend + Utils.prepareFilePath(path)],
						function (html)
						{
							var template, toolbar;
							that.setElement(that.container);
							if (!that.isActive) {

								mentor.publisher.contentArea.closeExistingPanel({
									type: type,
									systemId: that.selectedSystem.get("systemId")
								}, that);
							}

							that.renderToolBarAndContent({
										type: type,
										isSystem: true,
										title: title,
										systemId: that.selectedSystem.get("systemId")
									},
									html,
									that.selectedSystem);
							setTimeout(function ()
							{

								mentor.publisher.contentArea.layoutContentPanel(that.getContentPanelData(type, that,
                                                title, path),

										that.processReport(that.getDocumentContainer(),
												that.selectedSystem.get("systemId")));
								that.notify();

							}, 10);
						});
			}, render: function ()
			{
				var report, that = this, path, title, type = this.getContentType();
				if (this.selectedSystem &&
						(this.selectedSystem.get(type))) {
					report = this.getDocument();
					path = report.get("path");
					title = report.get("mainText");
					if (path) {
						this.loadReportHTML("../", path, that, type, title);
					}

					return this;
				}
			},
			notify: function ()
			{
				DesignReportPanel.prototype.notify.call(this);
				this.isActive = true;
			}

		});
    }
);
