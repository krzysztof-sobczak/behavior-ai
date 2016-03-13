ips = [];
for (a in _aggs) {
    for (t in a.transactions) {
        ips.add(t)
    }
}; return ips