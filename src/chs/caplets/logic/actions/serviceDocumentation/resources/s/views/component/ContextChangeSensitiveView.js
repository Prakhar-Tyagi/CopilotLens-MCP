define(["views/component/ContextChangeDelegator"], function (ContextChangeDelegator)
{
    return function (config)
    {
        return _.extend(ContextChangeDelegator(), {
            delegate: this,
            optionFilterApplied: function ()
            {
                if (config.applyOptionFilter) {
                    this.resetAndReRenderView();
                }
            },

            packageChanged: function ()
            {
                if (config.applyPackageChange) {
                    this.resetAndReRenderView({resetData: true});
                }
            },
            languageChanged: function ()
            {
                if (config.applyLanguageChange) {
                    this.resetAndReRenderView();
                }
            },
            searchTextApplied: function ()
            {
                if (config.applySearchFilter) {
                    this.resetAndReRenderView();
                }
            },
            resetAndReRenderView: function (config)
            {
                this.resetView(config);
                this.reRender();
            },

        });
    }
});