// ELASTICSEARCH
// -------------------------

var storageHost = 'http://localhost:9200';
var storageClient = null;

function storageInit() {
    storageClient = elasticsearch.Client({
        hosts: storageHost
    });

    storageClient.ping({
        requestTimeout: 30000,
        hello: "elasticsearch"
    }, function (error) {
        if (error) {
            console.error('elasticsearch cluster is down!');
        } else {
            console.log('Storage client connected.');
        }
    });
}

function storageSearch(start_time, end_time, path_limit, shard_size, treshold, callback) {
    var timeframesCount = 10;
    var interval = (Date.parse(end_time) - Date.parse(start_time)) / 1000 / 60 / timeframesCount;
    console.log('Performing analysis ...');
    storageClient.search({
        requestTimeout: 700000,
        index: 'logstash-*',
        body: {
            "size": 0,
            "query": {
                "filtered": {
                  "filter": {
                    "bool": {
                      "must": [
                        {
                           "range": {
                              "timestamp": {
                                "gte": start_time,
                                "lte": end_time,
                                "format": "yyyy-MM-dd HH:mm:ss"
                              }
                            }
                        }
                      ]
                    }
                  }
                }
              },
            "aggs": {
                "timeframes": {
                    "date_histogram": {
                        "field": "timestamp",
                        "interval": interval + "m"
                    },
                    "aggs": {
                        "behaviors": {
                            "scripted_metric": {
                                "init_script": {
                                    "lang": "groovy",
                                    "file": "psa-clustering_init"
                                },
                                "map_script": {
                                    "lang": "groovy",
                                    "file": "psa-clustering_map"
                                },
                                "combine_script": {
                                    "lang": "groovy",
                                    "file": "psa-clustering_combine"
                                },
                                "reduce_script": {
                                    "lang": "groovy",
                                    "file": "psa-clustering_reduce",
                                    "params": {
                                        "path_limit": path_limit,
                                        "shard_size": shard_size,
                                        "treshold": treshold
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }).then(function (response) {
        console.log("Analysis took: "+response.took);
//        console.log(response);
        console.log('Processing analysis results ...');
        callback(interval, response);
    }, function (err) {
        console.trace(err.message);
    });
}