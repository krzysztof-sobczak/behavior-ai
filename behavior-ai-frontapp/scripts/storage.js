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

        // undocumented params are appended to the query string
        hello: "elasticsearch"
    }, function (error) {
        if (error) {
            console.error('elasticsearch cluster is down!');
        } else {
            console.log('All is well');
        }
    });
}

function storageSearch(interval, path_limit, shard_size, treshold, callback) {
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
                              "gte": "2016-08-28 09:00:00",
                                "lte": "2016-08-31 12:00:00",
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
        console.log(response);
        callback(interval, response);
    }, function (err) {
        console.trace(err.message);
    });
}