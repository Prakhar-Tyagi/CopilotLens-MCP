/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2023 Siemens
 */

describe("Utilities Test", function () {

    it("hashcode should generate unique values", function () {

        var input1 = "14-GND17_A-GND17_B ?????.svg";
        var input2 = "14-GND17_B-GND17_A ?????.svg";
        console.log(input1.hashCode())
        console.log(input2.hashCode())

        expect(input1.hashCode()).not.toEqual(input2.hashCode());
    });

    it("hashcode should be same for same strings", function () {

        var input1 = "acducts.svg";
        var input2 = "acducts.svg";

        expect(input1.hashCode()).toEqual(input2.hashCode());
    });

});

