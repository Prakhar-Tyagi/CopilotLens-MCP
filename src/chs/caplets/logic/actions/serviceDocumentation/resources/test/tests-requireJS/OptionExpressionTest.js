/**
 * Created with IntelliJ IDEA.
 * User: mukumar
 * Date: 10/10/12
 * Time: 11:50 AM
 * To change this template use File | Settings | File Templates.
 */
/*global $,  assertTrue*/
describe("OptionExpressionTest", function () {
    beforeEach(function () {
        "use strict";
        this.optionExpressionFilter = new OptionExpressionFilter();
    });

    afterEach(function () {
        "use strict";
    });

    it("test option expression that contains 2 && operators", function () {
        "use strict";
        var result = this.optionExpressionFilter.optionExpressionEvaluation('o2 && o3 && o5', 'o2,o3,o5');
        // assertTrue("Option expression 'o2 && o3 && o5' evaluates to false against 'o2,o3,o5' options ", result);
        expect(result).toBeTruthy();
    });

    it("test 'op1 && op2 || ( !o4 && o6 )' against 'op1,op2'", function () {
        "use strict";
        var result = this.optionExpressionFilter.optionExpressionEvaluation("op1 && op2 || ( !o4 && o6 )", 'op1,op2')
        // assertTrue("Option expression 'op1 && op2 || ( !o4 && o6 )' against 'op1,op2' failed. ", result);
        expect(result).toBeTruthy();
    });

    it("test '!op1 ' against 'o6'", function () {
        "use strict";
        var result = this.optionExpressionFilter.optionExpressionEvaluation('!op1 ', 'o6');
        // assertTrue("Option expression '!op1 ', 'o6' failed. ", result);
        expect(result).toBeTruthy();
    });

    it("test '!(p1)&&(p2)&&!(p3)&&(!p5)' against 'p2", function () {
        "use strict";
        var result = this.optionExpressionFilter.optionExpressionEvaluation('!(p1)&&(p2)&&!(p3)&&(!p5)', 'p2');
        // assertTrue("Option expression '!(p1)&&(p2)&&!(p3)&&(!p5)' against 'p2 failed. ", result);
        expect(result).toBeTruthy();
    });

    it("test '!op1 || ( (op1 || op3) && ( op4 || op5))' against 'op1,o6'", function () {
        "use strict";
        var result = this.optionExpressionFilter.optionExpressionEvaluation('!op1 || ( (op1 || op3) && ( op4 || op5))',
                'op1,o6');
        // assertTrue("Option expression '!op1 || ( (op1 || op3) && ( op4 || op5))' against 'op1,o6' failed. ", !result);
        expect(result).toBeFalsy();
    });

    it("test '!op1 || ( (op1 || op3) && ( op4 || op5))' against 'op1,op5'", function () {
        "use strict";
        var result = this.optionExpressionFilter.optionExpressionEvaluation('!op1 || ( (op1 || op3) && ( op4 || op5))',
                'op1,op5');
        // assertTrue("Option expression '!op1 || ( (op1 || op3) && ( op4 || op5))' against 'op1,op5' failed. ", result);
        expect(result).toBeTruthy();
    });

    it("test '!op1 || ( (op1 || op3) && ( op4 || op5))' against 'op1,op4'", function () {
        "use strict";
        var result = this.optionExpressionFilter.optionExpressionEvaluation('!op1 || ( (op1 || op3) && ( op4 || op5))',
                'op1,op4');
        // assertTrue("Option expression '!op1 || ( (op1 || op3) && ( op4 || op5))' against 'op1,op4' failed. ", result);
        expect(result).toBeTruthy();
    });

    it("test '!op1 || ( (op1 || op3) && ( op4 || op5))' against 'op3,o2'", function () {
        "use strict";
        var result = this.optionExpressionFilter.optionExpressionEvaluation('!op1 || ( (op1 || op3) && ( op4 || op5))',
                'op3,o2');
        // assertTrue("Option expression '!op1 || ( (op1 || op3) && ( op4 || op5))' against 'op3,o2' failed. ", result);
        expect(result).toBeTruthy();
    });

    it("test 'A || B && C' against 'A'", function () {
        "use strict";
        var result = this.optionExpressionFilter.optionExpressionEvaluation('A || B && C', 'A');
        // assertTrue("Option expression 'A || B && C' against 'A' failed. ", result);
        expect(result).toBeTruthy();
    });

    it("test 'AB11' against 'AB'", function () {
        "use strict";
        var result = this.optionExpressionFilter.optionExpressionEvaluation('AB11', 'AB');
        // assertTrue("Option expression 'AB11' against 'AB' failed. ", !result);
        expect(result).toBeFalsy();
    });

    it("test 'AB' against 'AB11'", function () {
        "use strict";
        var result = this.optionExpressionFilter.optionExpressionEvaluation('AB', 'AB11');
        // assertTrue("Option expression 'AB' against 'AB11' failed. ", !result);
        expect(result).toBeFalsy();
    });

    it("test 'AB || AB 12 || BC' against 'AB12'", function () {
        "use strict";
        var result = this.optionExpressionFilter.optionExpressionEvaluation('AB || AB 12 || BC', 'AB12');
        // assertTrue("Option expression 'AB || AB 12 || BC' against 'AB12' failed. ", !result);
        expect(result).toBeFalsy();
    });

    it("test  'AB || AB 12 || BC' against 'AB 12' against 'AB12'", function () {
        "use strict";
        var result = this.optionExpressionFilter.optionExpressionEvaluation('AB || AB 12 || BC', 'AB 12');
        // assertTrue("Option expression 'AB || AB 12 || BC' against 'AB 12' failed. ", result);
        expect(result).toBeTruthy();
    });

    it("test  ' AB ||   AB 12 || B C' against 'AB 12'", function () {
        "use strict";
        var result = this.optionExpressionFilter.optionExpressionEvaluation(' AB ||   AB 12 || B C', 'AB 12');
        // assertTrue("Option expression ' AB ||   AB 12 || B C' against 'AB 12' failed. ", result);
        expect(result).toBeTruthy();
    });

    it("test  ' !AB ||   (AB && BC)' against 'AB'", function () {
        "use strict";
        var result = this.optionExpressionFilter.optionExpressionEvaluation(' !AB ||   (AB && BC)', 'AB');
        // assertTrue("Option expression ' !AB ||   (AB && BC)' against 'AB' failed. ", !result);
        expect(result).toBeFalsy();
    });

    it("test  ' !AB || AB12 && AB 12 ||  (AB && BC)' against 'AB, AB 12'", function () {
        "use strict";
        var result = this.optionExpressionFilter.optionExpressionEvaluation(' !AB || AB12 && AB 12 ||  (AB && BC)',
                'AB, AB 12');
        // assertTrue("Option expression ' !AB || AB12 && AB 12 ||  (AB && BC)' against 'AB, AB 12' failed. ", !result);
        expect(result).toBeFalsy();
    });

    it("test  ' !AB || AB12 && AB 12 ||  (AB && BC)' against 'AB, AB 12, BC'", function () {
        "use strict";
        var result = this.optionExpressionFilter.optionExpressionEvaluation(' !AB || AB12 && AB 12 ||  (AB && BC)',
                'AB, AB 12, BC');
        // assertTrue("Option expression ' !AB || AB12 && AB 12 ||  (AB && BC)' against 'AB, AB 12, BC' failed. ", result);
        expect(result).toBeTruthy();
    });

    it("test  ' !AB && AB12 || AB 12 ||  (AB && BC)', ' AB 12'", function () {
        "use strict";
        var result = this.optionExpressionFilter.optionExpressionEvaluation(' !AB && AB12 || AB 12 ||  (AB && BC)',
                ' AB 12');
        // assertTrue("Option expression ' !AB && AB12 || AB 12 ||  (AB && BC)', ' AB 12' failed. ", result);
        expect(result).toBeTruthy();
    });

    it("test  ' !AB && AB12 || AB 12 ||  (AB && BC)', ' AB12'", function () {
        "use strict";
        var result = this.optionExpressionFilter.optionExpressionEvaluation(' !AB && AB12 || AB 12 ||  (AB && BC)',
                ' AB12');
        // assertTrue("Option expression ' !AB && AB12 || AB 12 ||  (AB && BC)', ' AB12'failed. ", result);
        expect(result).toBeTruthy();
    });

    it("test  ' AB && AB12', ' AB 12,A B'", function () {
        "use strict";
        var result = this.optionExpressionFilter.optionExpressionEvaluation(' AB && AB12', ' AB 12,A B');
        // assertTrue("Option expression ' AB && AB12', ' AB 12,A B' failed. ", !result);
        expect(result).toBeFalsy();
    });

    it("test  ' AB && AB12', ' AB 12,A B,BC, CA'", function () {
        "use strict";
        var result = this.optionExpressionFilter.optionExpressionEvaluation(' AB && AB12', ' AB 12,A B,BC, CA');
        // assertTrue("Option expression ' AB && AB12', ' AB 12,A B,BC, CA' failed. ", !result);
        expect(result).toBeFalsy();
    });

    it("test  ' AB || AB12', ' AB 12,A B,AB , A'", function () {
        "use strict";
        var result = this.optionExpressionFilter.optionExpressionEvaluation(' AB || AB12', ' AB 12,A B,AB , A');
        // assertTrue("Option expression ' AB || AB12', ' AB 12,A B,AB , A' failed. ", result);
        expect(result).toBeTruthy();
    });

    it("test ' A B && AB12', ' AB 12,A B,AB , A'", function () {
        "use strict";
        var result = this.optionExpressionFilter.optionExpressionEvaluation(' A B && AB12', ' AB 12,A B,AB , A');
        // assertTrue("Option expression ' A B && AB12', ' AB 12,A B,AB , A' failed. ", !result);
        expect(result).toBeFalsy();
    });

    it("test ' A B && AB12', ' AB12,A B,AB , A'", function () {
        "use strict";
        var result = this.optionExpressionFilter.optionExpressionEvaluation(' A B && AB12', ' AB12,A B,AB , A');
        // assertTrue("Option expression ' A B && AB12', ' AB12,A B,AB , A' failed. ", result);
        expect(result).toBeTruthy();
    });

    it("test ' A B && AB', ' A,  B, 1AB, A B 1 2'", function () {
        "use strict";
        var result = this.optionExpressionFilter.optionExpressionEvaluation(' A B && AB', ' A,  B, 1AB, A B 1 2');
        // assertTrue("Option expression' A B && AB', ' A,  B, 1AB, A B 1 2' failed. ", !result);
        expect(result).toBeFalsy();
    });

    it("test ' A B && AB', ' A,  B'", function () {
        "use strict";
        var result = this.optionExpressionFilter.optionExpressionEvaluation(' A B && AB', ' A,  B');
        // assertTrue("Option expression ' A B && AB', ' A,  B' failed. ", !result);
        expect(result).toBeFalsy();
    });
});
