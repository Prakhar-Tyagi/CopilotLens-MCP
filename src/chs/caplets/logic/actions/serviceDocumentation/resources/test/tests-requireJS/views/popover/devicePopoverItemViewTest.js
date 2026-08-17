(function ()
{
    "use strict";
    var mockModel = new (Backbone.Model.extend())(), context, stubs, xrefContent;

    stubs = {
        currentPackage: mockModel,
        jquery: $,
        underscore: _,
        backbone: Backbone,
        DevicesCollection: new (Backbone.Collection.extend())(),
        fileDisplayHandler: {
            display: function (content)
            {
                this.content = content;
            }
        }
    };
    var data = [];
    data.push({id: "id", mainText: "mainText", subText: "subText"});
    stubs.DevicesCollection.set(data);
	stubs.DevicesCollection.getModels = function(){
		return [];
	};
    context = createContext(stubs);

    context(['views/p/relatedData/relatedDataPopoverView'], function (PopoverItemView)
    {
        describe("devicePopoverItemViewTest", function ()
        {
            it("should be able to load PopoverItemViewTest Module", function ()
            {
                expect(PopoverItemView).toBeDefined();
            });

            // TODO: test does not do anything. Element '.collapseAll' already has css display attribute set to none.
            xit("should not show collapse button when user hover the mouse over a sub item", function ()
            {
                PopoverItemView.container = "body";
                PopoverItemView.getTotalPages = function(){
					return 1;
				};
				PopoverItemView.getItems = function() {
					return [];
				}
                PopoverItemView.templateHTML =
                    '<div class="listItem " data-id="UID72a064-14243a5d1c4-9f0abf07d11e05c307e624d5ef59466c" style=""><span class="mainText">C-61276</span><span class="collapseAll" title="Click To Collapse" style="display: none;">[-]</span><br><span class="subText">DOOR-RtFt</span></div>';
				PopoverItemView.render();
				$(".mainText").trigger("mouseover");
				expect($(".collapseAll").css("display")).toBe("none");	
            });
        });
    });
})();

