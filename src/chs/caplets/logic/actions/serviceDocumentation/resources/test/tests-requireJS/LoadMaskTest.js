describe("Loading mask utility tests", function () {
    var objectUnderTest;
    beforeEach(function () {
        objectUnderTest = LoadingMaskCreator();
        $('body').append("<div id=\"SvgContainer\" class=\"Container\">");
        $('body').append("<div id=\"LoadMask\" class=\"LoadMask\">");
        $('body').append("<div id=\"LoadSVGMask\" class=\"LoadMask\">");
    })
    it("should remove existing mask before loading new", function () {

        expect($("#LoadMask").length).toBe(1);
        objectUnderTest.showLoadingRing = function () {
            //do nothing
        }
        expect(objectUnderTest).toBeTruthy();
        objectUnderTest.addLoadMask();
        expect($("#LoadMask").length).toBe(0);
    });
    it("should remove loading ring after package load", function () {
        objectUnderTest.removeLoadingRingAfterPackageLoad();
    });
    it("should also remove existing SVG mask before loading new", function () {

        expect($("#LoadSVGMask").length).toBe(1);
        objectUnderTest.showLoadingRing = function () {
            //do nothing
        }
        expect(objectUnderTest).toBeTruthy();
        objectUnderTest.addLoadMask();
        expect($("#LoadSVGMask").length).toBe(0);
    });

    it("should show loading ring", function () {
        objectUnderTest.showLoadingRing('SvgContainer');
        expect($("#LoadMask").height()).toBe(0);
        expect($("#LoadMask").width()).toBe(896);
    });

    it("should show add loading ring on package open", function () {
        $('body').append("<div id=\"eualContainer\" class=\"eualContainer\">");
        spyOn(objectUnderTest, 'removeLoadMask').andCallThrough();
        objectUnderTest.addLoadingRingOnPackageOpen();
        expect(objectUnderTest.removeLoadMask).toHaveBeenCalled();
    });

    it("should remove loading ring after package load", function () {
        spyOn(LoadMask, "removeLoadMask").andCallThrough();
        objectUnderTest.removeLoadingRingAfterPackageLoad();
        expect(LoadMask.removeLoadMask).toHaveBeenCalled();
    });

    it("should load SVG mask", function () {
        console.log(mentor.publisher.contentPanel.containerSelector);
        $('body').append("<div id=\"detail\" class=\"container\">");
        spyOn(objectUnderTest, 'removeLoadMask');
        objectUnderTest.LoadSVGMask('#SvgContainer');
        expect(objectUnderTest.removeLoadMask).toHaveBeenCalled();
    });

    it("should add error mask class", function () {
        objectUnderTest.addErrorMaskClass();
        expect($("#LoadMask").hasClass("LoadErrorMask")).toBeTruthy();
    });

    afterEach(function () {
        $('body').html("");
    })
});