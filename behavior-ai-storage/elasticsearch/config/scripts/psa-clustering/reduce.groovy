// make one list of user sessions
users = []; for (a in _aggs) {
    users = users + a
};

// create distance matrix
// using PSA algorithm on user.path

// perform hierarchical clustering using distance matrix
clusters = [];

// find cluster representants
// representant is a centroid of cluster
// in distance matrix of cluster we sum-up each row
// and find user with minimal value

// mocked clusters
clusters.add([size:20, representant: users[0]]);
clusters.add([size:12, representant: users[1]]);
clusters.add([size:7, representant: users[2]]);
return clusters;