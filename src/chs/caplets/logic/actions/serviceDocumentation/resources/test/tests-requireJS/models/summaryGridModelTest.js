require(['backbone', "SummaryGridModel"],
        function (Backbone, SummaryGridModel)
        {
            describe("SummaryGridModelTest", function ()
            {
                "use strict"
                var summary = {}, rows = [], row1 = {
                    Name: {value: "name1"},
                    ShortDescription:{value:"sd1"},
                    wires: {value: "wire1"},
                    weight: {value: "weight1"}
                }, row2 = {
                    Name: {value: "name2"},
                    ShortDescription:{value:"sd2"},
                    wires: {value: "wire2"},
                    weight: {value: "weight2"}
                };

                beforeEach(function ()
                {
                    rows.push(row1);
                    rows.push(row2);
                    summary.data = {};
                    summary.layout = {};
                    summary.layout.attributes = {};
                    summary.layout.attributes["column-names"] = ["Name", "ShortDescription", "wires", "weight"];
                    summary.data.entries = rows;
                });

                afterEach(function ()
                {
                    rows = [];
                    summary = {};
                });

                it("show correct number of rows and columns", function ()
                {
                    var summaryModel = new SummaryGridModel({
                        tableData: summary
                    });
                    expect(summaryModel.get('tableData').items.length).toBe(2);
                    expect(summaryModel.get('tableData').cols.length).toBe(2);
                });

                it("show correct column names", function ()
                {
                    var summaryModel = new SummaryGridModel({
                        tableData: summary
                    });
                    expect(summaryModel.get('tableData').cols[0]).toBe("name1");
                    expect(summaryModel.get('tableData').cols[0]).toBe("name2");
                });

                it("show correct data in rows", function ()
                {
                    var summaryModel = new SummaryGridModel({
                        tableData: summary
                    });
                    expect(summaryModel.get('tableData').items[0]["name1"].value).toBe("wires:wire1");
                    expect(summaryModel.get('tableData').items[0]["name2"].value).toBe("wires:wire2");

                    expect(summaryModel.get('tableData').items[1]["name1"].value).toBe("weight:weight1");
                    expect(summaryModel.get('tableData').items[1]["name2"].value).toBe("weight:weight2");
                });
            });
        });
