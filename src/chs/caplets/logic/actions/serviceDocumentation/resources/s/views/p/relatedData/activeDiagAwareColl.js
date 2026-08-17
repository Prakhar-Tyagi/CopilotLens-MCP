define([], function ()
{
    return function (config)
    {
        return {
            type: config.type,
            category: config.category,
            inactiveDiagramCSS: "panelitem_hide",
            filter: function (items)
            {
                var activeSystem = require("models/selectedSystem");
                return this.markObjectsThatAreNotPresentInActiveDiagram(items, activeSystem);
            },
            markInActive: function (object)
            {
                object.isActive = this.inactiveDiagramCSS;
                return object;

            },
            existsInActiveDiagram: function (object)
            {
                var diaIds = object.diagramUids;
                return diaIds.indexOf(this.activeDiagram) < 0;
            },

            markObjectsThatAreNotPresentInActiveDiagram: function (items, activeSystem)
            {
                items = items || [];
                this.activeDiagram = activeSystem.get("diagramId") || "";
                if (this.activeDiagram) {
                    var that = this;
                    items = _.map(items, function (object)
                    {
                        if (that.existsInActiveDiagram(object)) {
                            return that.markInActive(object);
                        }
                        return object;
                    });
                }
                return items;
            }
        };
    }
});
