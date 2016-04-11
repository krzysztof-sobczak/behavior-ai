// VISUALIZATION
// ------------------------------------------

var stageWidth = 600;
var stageHeight = 650;
var stageSvg = null;

function visualizationInit() {
    var margin = {top: 20, right: 200, bottom: 0, left: 20};

    stageSvg = d3.select("body").append("svg")
        .attr("width", stageWidth + margin.left + margin.right)
        .attr("height", stageHeight + margin.top + margin.bottom)
        .style("margin-left", margin.left + "px")
        .append("g")
        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");
}

var visualize = function visualize(interval, data) {

    var intervalSeconds = interval * 60 * 1000;

    function truncate(str, maxLength, suffix) {
        if (str.length > maxLength) {
            str = str.substring(0, maxLength + 1);
            str = str.substring(0, Math.min(str.length, str.lastIndexOf(" ")));
            str = str + suffix;
        }
        return str;
    }

    data = data['aggregations']['timeframes']['buckets'];
    var timeframesCount = data.length;
    var clusterList = [];
    data.forEach(function (timeframeData) {
        timeframeData['timeframe_start'] = new Date(timeframeData['key']);
        timeframeData['timeframe_end'] = new Date((timeframeData['key'] + intervalSeconds));
        timeframeData['behaviors']['value']['clusters'].forEach(function (cluster) {
            var timeframe = [
                timeframeData['timeframe_start'],
                timeframeData['timeframe_end'],
                cluster['size']
            ];
            var representant = cluster['representants'][0];
            if (clusterList.hasOwnProperty(representant.pathHash)) {
                clusterList[representant.pathHash].timeframes.push(timeframe)
            } else {
                clusterList[representant.pathHash] = {
                    "timeframes": [
                        timeframe
                    ],
                    "name": representant['path'].join(', ')
                }
            }
        });
    });
    data = [];
    for (var key in clusterList) {
        if (!clusterList.hasOwnProperty(key)) continue;
        data.push(clusterList[key]);
    }
    clusterList = null;

    var xMin = d3.min(data, function (cluster) {
        return d3.min(cluster['timeframes'], function (timeframe) {
            return timeframe[0];
        });
    });
    var xMax = d3.max(data, function (cluster) {
        return d3.max(cluster['timeframes'], function (timeframe) {
            return timeframe[0];
        });
    });
    console.log(xMin);
    console.log(xMax);

    var xScale = d3.time.scale()
        .domain([xMin, xMax])
        .range([0, stageWidth]);

    var timeFormat = d3.time.format("%m/%d %H:%M");
    xAxis = d3.svg.axis()
        .scale(xScale)
        .orient("top")
        .ticks(timeframesCount)
        .tickPadding(5)
        .tickFormat(timeFormat);

    stageSvg.append("g")
        .attr("class", "x axis")
        .attr("transform", "translate(0," + 0 + ")")
        .call(xAxis);

    var c = d3.scale.category20c();

    for (var j = 0; j < data.length; j++) {
        var g = stageSvg.append("g").attr("class", "journal");

        var circles = g.selectAll("circle")
            .data(data[j]['timeframes'])
            .enter()
            .append("circle");

        var text = g.selectAll("text")
            .data(data[j]['timeframes'])
            .enter()
            .append("text");

        var rScale = d3.scale.linear()
            .domain([0, d3.max(data[j]['timeframes'], function (d) {
                return d[2];
            })])
            .range([2, 12]);

        circles
            .attr("cx", function (d, i) {
                return xScale(d[0]);
            })
            .attr("cy", j * 30 + 20)
            .attr("r", function (d) {
                return rScale(d[2]);
            })
            .style("fill", function (d) {
                return c(j);
            });

        text
            .attr("y", j * 30 + 25)
            .attr("x", function (d, i) {
                return xScale(d[0]) - 5;
            })
            .attr("class", "value")
            .text(function (d) {
                return d[2];
            })
            .style("fill", function (d) {
                return c(j);
            })
            .style("display", "none");

        g.append("text")
            .attr("y", j * 30 + 25)
            .attr("x", stageWidth + 20)
            .attr("class", "label")
            .text(truncate(data[j]['name'], 30, "..."))
            .style("fill", function (d) {
                return c(j);
            })
            .on("mouseover", mouseover)
            .on("mouseout", mouseout);
    }
    ;

    function mouseover(p) {
        var g = d3.select(this).node().parentNode;
        d3.select(g).selectAll("circle").style("display", "none");
        d3.select(g).selectAll("text.value").style("display", "block");
    }

    function mouseout(p) {
        var g = d3.select(this).node().parentNode;
        d3.select(g).selectAll("circle").style("display", "block");
        d3.select(g).selectAll("text.value").style("display", "none");
    }
};