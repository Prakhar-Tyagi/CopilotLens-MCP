/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

define(
        [
            'backbone',
            'underscore',
            'text!templates/troubleshoot/faultCodeSelection.html',
            'preferences',
            'collections/faults',
            'select2',
            'currentPackage',
        ],
        function (
                Backbone,
                _,
                html,
                preferences,
                faults,
                select2,
                selectedPackage
        ) {
            return Backbone.View.extend({

                events: {
                    'click #fault-code-selection-header': 'toggleSection',
                },

                initialize(options)
                {
                    this.faultCodesModel = options.faultCodesModel;
                    this.faultCodesModel.on('didClearCodes deleteRow', this.configureDataForSelection, this);
                    selectedPackage.on('change:language', this.render, this);
                },

                render()
                {
                    this.$el.html(_.template(html)());
                    this.registerSelectEventsForFaultSelection();
                    this.configureDataForSelection();
                    return this;
                },

                addClearSearchIconEvent()
                {
                    const inputField = $('input.select2-search__field');
                    const inputValue = inputField.val() || '';
                    const iconToShow = inputValue.length > 0 ? 'fault-code-selection-clear-icon' :
                            'fault-code-selection-search-icon';
                    const iconToHide = inputValue.length > 0 ? 'fault-code-selection-search-icon' :
                            'fault-code-selection-clear-icon';

                    let deleteIcon = inputField.parent('span').addClass('deleteIcon');
                    if (deleteIcon.length === 0) {
                        inputField.wrap('<span class="deleteIcon"></span>')
                                .after(`<span id="${iconToHide}"></span>`)
                                .after(`<span id="${iconToShow}"></span>`);
                        deleteIcon = inputField.parent();
                    }

                    deleteIcon.find(`#${iconToShow}`).toggle(inputValue.length === 0);
                    deleteIcon.find(`#${iconToHide}`).toggle(inputValue.length > 0);

                    inputField.on('keyup', function () {
                        const updatedInputValue = inputField.val() || ''; // Add check for undefined value
                        deleteIcon.find(`#${iconToShow}`).toggle(updatedInputValue.length === 0);
                        deleteIcon.find(`#${iconToHide}`).toggle(updatedInputValue.length > 0);
                    });

                    this.clickClearEvent();
                },

                clickClearEvent()
                {
                    $('input.select2-search__field').parent('span').on('click', '#fault-code-selection-clear-icon',
                            function () {
                                $('input.select2-search__field').val('');
                            });
                },

                toggleSection(evt)
                {
                    const $ele = $(evt.target).parent().find('.orient-inner');
                    const isExpanded = $ele.hasClass('expanded');

                    $ele.toggleClass('expanded', !isExpanded);
                    $ele.toggleClass('collapsed', isExpanded);

                    $('.select2-container').toggle(!isExpanded);
                },

                registerSelectEventsForFaultSelection()
                {
                    const faultCodeSelection = $('.faultCodeSelection');

                    // Defining separate functions for each event listener
                    const onSelect = (e) => {
                        const index = e.params.data.id;
                        const code = faults.findById(index).get("code");
                        this.faultCodesModel.add(code);
                        this.addClearSearchIconEvent();
                        $('#commonFaultCodeContainer input').select();
                        $('li.select2-search.select2-search--inline').css('width', '100%');
                        $('.select2-search__field').css('width', '100%');
                    };

                    const onUnselect = (e) => {
                        const index = e.params.data.id;
                        const code = faults.findById(index).get("code");
                        this.faultCodesModel.remove(code);
                        this.addClearSearchIconEvent();
                    };

                    const onOpening = () => {
                        $('#troubleshootingPanel').css('overflow', 'hidden');
                        $('.faultRow').animate({height: "255px"}, 200);
                    };

                    const onOpen = () => {
                        $(document.body).on('mousedown.fault', function (e) {
                            var $target = $(e.target);
                            var $select = $target.closest('.select2');

                            var $all = $('.select2.select2-container--open');

                            $all.each(function () {
                                if (this == $select[0]) {
                                    return;
                                }
                            });
                            if ($target.closest('#troubleshootingPanel').length !== 0) {
                                $target.trigger(jQuery.Event('click', {triggeredByCode: true}));
                            }
                        });
                    };

                    const onClose = () => {
                        this.configureDataForSelection();
                        this.addClearSearchIconEvent();
                        $('.faultRow').animate({height: "85px"}, 200, () => {
                            $('.faultRow').css("height", "auto");
                        });
                        $('#troubleshootingPanel').css('overflow', 'auto');
                        setTimeout(() => {
                            $(document.body).off('mousedown.fault');
                        }, 0)
                    };

                    // Assigning each listener to the event using jQuery on() method
                    faultCodeSelection.on('select2:select', onSelect.bind(this));
                    faultCodeSelection.on('select2:unselect', onUnselect.bind(this));
                    faultCodeSelection.on('select2:opening', onOpening);
                    faultCodeSelection.on('select2:open', onOpen);
                    faultCodeSelection.on('select2:close', onClose.bind(this));
                },

                formatSearchResult(result)
                {
                    if (!result.id) {
                        return result.text;
                    }

                    const query = $('.select2-search__field').val();
                    const str = result.text;
                    const regex = new RegExp(query, 'i');
                    const indexQuery = str.toLowerCase().indexOf(query.toLowerCase());

                    const highlightText = str.substring(indexQuery, indexQuery + query.length);
                    let newStr = str;
                    if (highlightText.length > 0) {
                        newStr = str.replace(regex,
                                `<span class="results-options-highlighted-text">${highlightText}</span>`);
                    }

                    return $('<span>').html(newStr);
                },

                configureDataForSelection()
                {
                    const faultCodeSelection = $('.faultCodeSelection');
                    if (faultCodeSelection.hasClass("select2-hidden-accessible")) {
                        faultCodeSelection.empty();
                    }

                    faultCodeSelection.select2 && faultCodeSelection.select2({
                        closeOnSelect: false,
                        placeholder: mentor.publisher.languageTranslator.localize(
                                'TroubleshootingPanel.FaultSelection.PlaceHolder'),
                        allowHtml: true,
                        templateResult: this.formatSearchResult,
                        allowClear: true,
                        width: '100%',
                        dropdownParent: this.$('.select2-dropdown-class'),
                        dropdownAutoWidth: true,
                        data: this.getSelectionData(),
                        language: {
                            noResults: function () {
                                return mentor.publisher.languageTranslator.localize(
                                        'TroubleshootingPanel.FaultSelection.NoResults');
                            }
                        }
                    });

                    $('li.select2-search.select2-search--inline, .select2-search__field').css('width', '100%');
                    this.addClearSearchIconEvent();
                    $('.select2-search__field').attr('placeholder', mentor.publisher.languageTranslator.localize(
                            'TroubleshootingPanel.FaultSelection.PlaceHolder'));
                },

                getSelectionData()
                {
                    const {faultCodesModel} = this;

                    return faults.map(fault => {
                        const {code, description} = fault.toJSON();
                        const name = Utils.translatePlainText(description);
                        const selected = faultCodesModel.isActive(code) || faultCodesModel.isPassive(code);
                        const text = name ? `${code} - ${name}` : code;

                        return {id: fault.get('index'), text, name, selected};
                    });
                }

            });
        }
);


