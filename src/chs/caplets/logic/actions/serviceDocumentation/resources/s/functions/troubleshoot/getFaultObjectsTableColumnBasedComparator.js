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
                // Devices ? Inlines ? Grounds ? Wire ? Rest.
                const TYPE_COLUMN_INDEX = 1, COMPONENT_COLUMN_INDEX = 2, DESCRIPTION_COLUMN_INDEX = 3,
                        FAULT_CODES_COLUMN_INDEX = 4;
                return function (a, b) {
                    var result = 0;
                    switch (columnIndex) {
                        case FAULT_CODES_COLUMN_INDEX:
                            if (a.codes.length > b.codes.length) {
                                result = 1;
                            }
                            else if (a.codes.length < b.codes.length) {
                                result = -1;
                            }
                            else {
                                var index = 0;
                                while (index < a.codes.length) {
                                    result = Utils.alphaNumericCompareFn(a.codes[index][0], b.codes[index][0]);
                                    if (result != 0) {
                                        break;
                                    }
                                    index += 1;
                                }
                                if (result === 0) {
                                    result = Utils.alphaNumericCompareFn(a.type, b.type);
                                }
                            }
                            break;
                        case DESCRIPTION_COLUMN_INDEX:
                            result = Utils.alphaNumericCompareFn(a.column3Value, b.column3Value);
                            break;
                        case COMPONENT_COLUMN_INDEX:
                            result = Utils.alphaNumericCompareFn(a.column2Value, b.column2Value);
                            break;
                        case TYPE_COLUMN_INDEX:
                            result = Utils.alphaNumericCompareFn(a.type, b.type);
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