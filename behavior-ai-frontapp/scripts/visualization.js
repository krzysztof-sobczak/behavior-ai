// VISUALIZATION
// ------------------------------------------

var stageWidth = 1600;
var stageHeight = 12000;
var stageSvg = null;

function visualizationInit() {
    var margin = {top: 20, right: 500, bottom: 0, left: 20};

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
    var timeframesData = [];
    var timeframesCount = data.length - 1;
    var clusterList = [];
    data.forEach(function (timeframeData) {
        timeframeData['timeframe_start'] = new Date(timeframeData['key']);
        timeframeData['timeframe_end'] = new Date((timeframeData['key'] + intervalSeconds));
        timeframesData.push(timeframeData);
        timeframeData['behaviors']['value']['clusters'].forEach(function (cluster) {
            var timeframe = [
                timeframeData['timeframe_start'],
                timeframeData['timeframe_end'],
                cluster['size'],
                timeframeData['behaviors']['value']['clusters_users_count']
            ];
            var representant = cluster['representants'][0];
            if (clusterList.hasOwnProperty(representant.pathHash)) {
                clusterList[representant.pathHash].timeframes.push(timeframe)
            } else {
                clusterList[representant.pathHash] = {
                    "timeframes": [
                        timeframe
                    ],
                    "path": representant['path'],
                    "name": representant['path'].join(', '),
                    "anomaly_level": 0,
                }
            }
        });
    });
    // fill clusters between timeframes
    data.forEach(function (timeframeData) {
        timeframeData['timeframe_start'] = new Date(timeframeData['key']);
        timeframeData['timeframe_end'] = new Date((timeframeData['key'] + intervalSeconds));
        timeframeData['behaviors']['value']['clusters'].forEach(function (timeFrameCluster) {
        var representant = timeFrameCluster['representants'][0];
        var path = representant['path'];
        var name = path.join(', ');
        for (var key in clusterList) {
            var timeframe = [
                timeframeData['timeframe_start'],
                timeframeData['timeframe_end'],
                timeFrameCluster['size'],
                timeframeData['behaviors']['value']['clusters_users_count']
            ];
            cluster = clusterList[key];
            var modifier = 0;
            if(path.length > cluster.path.length && name.indexOf(cluster.name) !== -1) {
                  modifier = timeframe[2];
            }
            var timeFrameIndex = null;
            for (var timeframeKey in cluster.timeframes) {
                if(cluster.timeframes[timeframeKey][0].getTime() == timeframe[0].getTime() && cluster.timeframes[timeframeKey][1].getTime() == timeframe[1].getTime()) {
                    timeFrameIndex = timeframeKey;
                    break;
                }
            }
            if(timeFrameIndex == null) {
                timeframe[2] = modifier;
//                            console.log("[new timeframe] boost " + cluster.name + " with " + name + " by " + timeframe[2] + " on " + timeframe[0]);
                clusterList[key].timeframes.push(timeframe);
                clusterList[key].timeframes.sort(function(a,b) {return (a[0] > b[0]) ? 1 : ((b[0] > a[0]) ? -1 : 0);} );
            } else {
//                            console.log("[existing timeframe] boost " + cluster.name + "("+cluster.timeframes[timeFrameIndex][2]+") with " + name + " by " + timeframe[2] + " on " + timeframe[0]);
                cluster.timeframes[timeFrameIndex][2] += modifier;
            }
        }
        });
    });
    clusterList = clusterList.sort(function(a, b) {
        var aSum = a.timeframes.reduce( function(prev, next){
           return prev + next[2];
        }, 0);
        var bSum = b.timeframes.reduce( function(prev, next){
           return prev + next[2];
        }, 0);
        return parseFloat(aSum) - parseFloat(bSum);
    });
    data = [];
    var mainAnomaly = null;
    var mainAnomalyScore = 0;
    for (var key in clusterList) {
        if (!clusterList.hasOwnProperty(key)) continue;
        var clusterTimeframes = [];
        var found = false;
        for (var timeframeKey in clusterList[key].timeframes) {
            var timeframe = clusterList[key].timeframes[timeframeKey];
            var percentValue = Math.round((timeframe[2]/timeframe[3])*10000)/100;
            if(percentValue > 1) {
                clusterTimeframes.push(timeframe);
                found = true;
            }
        }
        if(found) {
            var windowSize = 3;
            var trainingDataLength = 7;
            var trainingWindowsCount = trainingDataLength - windowSize + 1;
            var trainingWindowStart = 0;
            clusterList[key]['training'] = 0;
            for(i = trainingWindowStart; i <= trainingWindowsCount; i++) {
                var start = i;
                var end = i + windowSize;
                var windowLength = end - start;
                var currentWindow = clusterList[key].timeframes.slice(start,end);
                var windowPercentValue = 0;
                for(var timeframeKey in currentWindow) {
                    var timeframe = currentWindow[timeframeKey];
                    var timeframePercentValue = Math.round((timeframe[2]/timeframe[3])*10000)/100;
                    windowPercentValue += (timeframePercentValue >= 1) ? timeframePercentValue : 0;
                }
                windowMeanPercentValue = windowPercentValue / windowLength;
                clusterList[key]['training'] += windowMeanPercentValue;
//                console.log(clusterList[key].name + " ("+start+","+end+") -> "+windowMeanPercentValue);
            }
            clusterList[key]['training'] = clusterList[key]['training'] / trainingWindowsCount;

            var testDataLength = 5;
            var testWindowsCount = testDataLength - windowSize + 1;
            var testWindowStart = trainingWindowsCount;
            clusterList[key]['test'] = 0;
            var testWindowMaxDelta = 0;
            var testWindowMinDelta = 1000000;
            var testBestWindowValue = 0;
            clusterList[key]['anomaly.window'] = 0;
            clusterList[key]['anomaly.window.size'] = windowSize;
            for(i = testWindowStart; i <= clusterList[key].timeframes.length - windowSize; i++) {
                var start = i;
                var end = i + windowSize;
                var windowLength = end - start;
                var currentWindow = clusterList[key].timeframes.slice(start,end);
                var windowPercentValue = 0;
                for(var timeframeKey in currentWindow) {
                    var timeframe = currentWindow[timeframeKey];
                    var timeframePercentValue = Math.round((timeframe[2]/timeframe[3])*10000)/100;
                    windowPercentValue += (timeframePercentValue >= 1) ? timeframePercentValue : 0;
                }
                windowMeanPercentValue = windowPercentValue / windowLength;
                var windowDelta = Math.max(clusterList[key].training, windowMeanPercentValue) / Math.min(clusterList[key].training, windowMeanPercentValue);
                if(windowDelta > testWindowMaxDelta) {
                    clusterList[key]['anomaly.window'] = i;
                    clusterList[key]['anomaly.window.delta'] = windowDelta;
                    testWindowMaxDelta = windowDelta;
                }
                if(windowDelta < testWindowMinDelta) {
//                    clusterList[key]['anomaly.window'] = i;
//                    clusterList[key]['anomaly.window.delta'] = windowDelta;
                    testWindowMinDelta = windowDelta;
                    testBestWindowValue = windowMeanPercentValue;
                }
                clusterList[key]['test'] += windowMeanPercentValue;
            }
//            clusterList[key]['test'] = clusterList[key]['test'] / testWindowsCount;
            clusterList[key]['test'] = testBestWindowValue;

            var delta = clusterList[key].training - clusterList[key].test;
            var biggerValue = Math.max(clusterList[key].training, clusterList[key].test);
            var smallerValue = Math.min(clusterList[key].training, clusterList[key].test);
            if(smallerValue == 0) {
                var relativeDelta = delta / 2;
            } else {
                var relativeDelta = biggerValue / smallerValue;
            }

            if(relativeDelta > 2.5) {
//                console.log(clusterList[key].name + ': ' + clusterList[key].training + ' vs ' + clusterList[key].test + " rel=" + relativeDelta);
                if(relativeDelta < 3) {
                    clusterList[key].anomaly_level = 1;
                } else if(relativeDelta < 3.5) {
                    clusterList[key].anomaly_level = 2;
                } else if(relativeDelta < 4) {
                    clusterList[key].anomaly_level = 3;
                } else if(relativeDelta < 4.5) {
                    clusterList[key].anomaly_level = 4;
                } else if(relativeDelta < 5) {
                    clusterList[key].anomaly_level = 5;
                } else {
                    clusterList[key].anomaly_level = 6;
                }
                if(relativeDelta > mainAnomalyScore) {
                    mainAnomaly = clusterList[key].name;
                    mainAnomalyScore = relativeDelta;
                }
            }
            clusterList[key].timeframes = clusterTimeframes;
            data.push(clusterList[key]);
        }
    }
//    console.log("Main anomaly: " + mainAnomaly + ' with ' + mainAnomalyScore);
    clusterList = null;
    console.log("Number of clusters: "+data.length);

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
//    console.log(xMin);
//    console.log(xMax);

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

    var g = stageSvg.append("g").attr("class", "journal");
    g.append("rect")
        .attr("y", 0)
        .attr("x", xScale(timeframesData[7]['timeframe_start']) - 30)
        .attr("class", "test-section")
        .attr('width', xScale(timeframesData[9]['timeframe_start']) - xScale(timeframesData[7]['timeframe_start'])+60)
        .attr('height', stageHeight)
        .style("fill", 'rgb(150,10,128)')
        .style('opacity', 0.03);

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
                return d[3];
            })])
            .range([6, 14]);

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
                return (Math.round((d[2]/d[3])*10000)/100)+"%";
            })
            .style("fill", function (d) {
                return c(j);
            })
            .style("display", "none");

        g.append("text")
            .attr("y", j * 30 + 25)
            .attr("x", stageWidth + 50)
            .attr("class", "label")
            .text(truncate(data[j]['name'], 60, "..."))
            .style("fill", function (d) {
                return c(j);
            })
            .on("mouseover", mouseover)
            .on("mouseout", mouseout);

        var anomalyLevel = data[j]['anomaly_level'];
        if(anomalyLevel > 0) {
            g.append("rect")
                .attr("y", j * 30 + 8)
                .attr("x", -30)
                .attr("class", "anomaly-row")
                .attr('width', stageWidth+60)
                .attr('height', 25)
                .style("fill", function (d) {
                    return c(j);
                })
                .style('opacity', (anomalyLevel)*0.1)

            g.append("rect")
                .attr("y", 0)
                .attr("x", xScale(timeframesData[data[j]['anomaly.window']]['timeframe_start']) - 30)
                .attr("class", "anomaly-window")
                .attr('width', xScale(timeframesData[data[j]['anomaly.window']+data[j]['anomaly.window.size']-1]['timeframe_start']) - xScale(timeframesData[data[j]['anomaly.window']]['timeframe_start'])+60)
                .attr('height', stageHeight)
                .style("fill", 'rgb(150,10,128)')
                .style('opacity', 0.3)
                .style('display','none');
        }
    }
    ;

    function mouseover(p) {
        var g = d3.select(this).node().parentNode;
        d3.select(g).selectAll("circle").style("display", "none");
        d3.select(g).selectAll(".anomaly-window").style("display", "block");
//        d3.select(g).selectAll("rect").style("display", "none");
        d3.select(g).selectAll("text.value").style("display", "block");
    }

    function mouseout(p) {
        var g = d3.select(this).node().parentNode;
        d3.select(g).selectAll("circle").style("display", "block");
        d3.select(g).selectAll(".anomaly-window").style("display", "none");
//        d3.select(g).selectAll("rect").style("display", "block");
        d3.select(g).selectAll("text.value").style("display", "none");
    }
};