/**
 * Created by kayyagar on 22-01-2016.
 */
require(['backbone', "SelectedObjectsStore","SVGTransformModel"], function (Backbone, store,SVGTransformModel)
{
    describe("SelectedObjectsStoreTest", function ()
    {
        "use strict"

        beforeEach(function ()
        {
            // store.removeContainer(new Backbone.Model({svgContainerId: 'svg1'}));
            // store.removeContainer(new Backbone.Model({svgContainerId: 'svg2'}));
        });

        xit("store should add new objects for a container", function ()
        {
            var objs;
            store.addObjectsForContainer(new SVGTransformModel({svgContainerId: 'svg1', root: 'root'}), ['1', '2'], false);
            objs = store.getSelectedObjectForContainer('svg1');
            expect(objs.length).toBe(2);
            store.addObjectsForContainer(new SVGTransformModel({svgContainerId: 'svg2', root: 'root'}), ['1', '2', '3'],
                    false);
            objs = store.getSelectedObjectForContainer('svg2');
            expect(objs.length).toBe(3);
        });
        xit("store should add new objects for a container on top of existing objects when not resetting earlier highlights",
                function ()
                {
                    var objs;
                    store.addObjectsForContainer(new Backbone.Model({svgContainerId: 'svg1', root: 'root'}), ['1', '2'],
                            true);
                    objs = store.getSelectedObjectForContainer('svg1');
                    expect(objs.length).toBe(2);
                    store.addObjectsForContainer(new Backbone.Model({svgContainerId: 'svg1', root: 'root'}),
                            ['3', '4', '5'], true);
                    objs = store.getSelectedObjectForContainer('svg1');
                    expect(objs.length).toBe(5);
                }
        );
        xit("store should be upto date with changes to svg transform model",
                function ()
                {
                    var model = new Backbone.Model({svgContainerId: 'svg1', root: 'root'});
                    store.addObjectsForContainer(model, ['1', '2'], true);
                    model.set({svgContainerId: 'svg2'});
                    expect(store.getSelectedObjectForContainer('svg2').length).toBe(2);
                    expect(store.getSelectedObjectForContainer('svg1').length).toBe(0);
                }
        );
        xit("remove data related to a container", function ()
        {
            var objs;
            store.addObjectsForContainer(new Backbone.Model({svgContainerId: 'svg1', root: 'root'}), ['1', '2'], true);
            objs = store.getSelectedObjectForContainer('svg1');
            expect(objs.length).toBe(2);
            store.removeContainer(new Backbone.Model({svgContainerId: 'svg1'}));
            objs = store.getSelectedObjectForContainer('svg1');
            expect(objs.length).toBe(0);
        });
    });
});