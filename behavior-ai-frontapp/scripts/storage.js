// ELASTICSEARCH
// -------------------------

var storageHost = 'http://storage.behaviorai.docker:9200';
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
        requestTimeout: 100000,
        index: 'logstash-2015.06.03',
        body: {
            "size": 0,
            "query": {
                "match_all": {}
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