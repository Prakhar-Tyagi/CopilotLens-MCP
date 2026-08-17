/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

define(
        [],
        function () {
            return function (columnIndex, ascending) {
                const CHECKBOX_COLUMN_INDEX = 1, FAULT_CODE_COLUMN_INDEX = 2, DESCRIPTION_COLUMN_INDEX = 3;
                return function (a, b) {
                    var result = 0;
                    switch (columnIndex) {
                        case FAULT_CODE_COLUMN_INDEX:
                            result = Utils.alphaNumericCompareFn(a.code, b.code);
                            break;
                        case DESCRIPTION_COLUMN_INDEX:
                            result = Utils.alphaNumericCompareFn(a.description, b.description);
                            break;
                        case CHECKBOX_COLUMN_INDEX:
                            result = Utils.alphaNumericCompareFn(b.checkBoxState, a.checkBoxState);
                            break;
                    }
                    if (result === 0) {
                        if (a.id < b.id) {
                            result = -1;
                        }
                        else if (a.id > b.id) {
                            result = 1;
                        }
                    }

                    return ascending ? result : -result;
                }
            }
        }
)