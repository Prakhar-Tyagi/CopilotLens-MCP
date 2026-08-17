
describe("optionFilterPanelTest", function(){

    beforeEach(function(){
        $('body').append("<div id='vinTextHolder'><input id='vinFilterText' disabled value=''></div>");
    });

    it("should apply VIN options and vin number in the vin text field", function(){
        var event = {
            detail : {
                vinOptions : "op1",
                vinNumber : "1234"
            }
        };
        mentor.publisher.optionFilterPanel.changeVINFilterText(event);
        var inputBox = $("#vinFilterText");
        expect($(inputBox).val()).toBe('1234:op1');
    });

    it("should disable VIN input text after VIN is applied", function(){
        var event = {
            detail : {
                vinOptions : "op1",
                vinNumber: ""
            }
        };
        mentor.publisher.optionFilterPanel.changeVINFilterText(event);
        var inputBox = $("#vinFilterText");
        expect($(inputBox).val()).toBe(':op1');
        expect($(inputBox).attr("disabled")).toBeTruthy();
        expect($(inputBox).attr("readonly")).toBe("readonly")
    });

    it("VIN text box should enable tooltip when VIN is applied", function(){
        var event = {
            detail : {
                vinOptions : "op1,op2,op3,op4",
                vinNumber: ""
            }
        };
        mentor.publisher.optionFilterPanel.changeVINFilterText(event);
        var inputBox = $("#vinTextHolder");
        expect($(inputBox).attr("title")).toBe(":op1,op2,op3,op4");

    });

    it("VIN text box should add place holder text when VIN is applied", function(){
        var event = {
            detail : {
                vinOptions : "",
                vinNumber: ""
            }
        };
        mentor.publisher.optionFilterPanel.changeVINFilterText(event);
        var inputBox = $("#vinTextHolder");
        expect($(inputBox).attr("title")).toBe("");
    });

    it("should add place holder text when VIN is applied", function(){
        expect(mentor.publisher.optionFilterPanel.setVINFilterBoxText()).toEqual($("#vinFilterText"));
    });

    it("should be able to set VIN Value in VIN filter view", function(){
        mentor.publisher.dataLoader.loadOptionFilterInfo=function () {return {vin: false, config: false}};
        spyOn(mentor.publisher.eventDispatcher, "attachEventListener");
        mentor.publisher.optionFilterPanel.VINFilterView({});
        mentor.publisher.dataLoader.loadOptionFilterInfo=function () {return {vin: true, config: true}};
        mentor.publisher.optionFilterPanel.VINFilterView({});
        expect(mentor.publisher.eventDispatcher.attachEventListener).toHaveBeenCalled();
    });

    afterEach(function(){
        $("#vinFilterText").remove();
    });


});