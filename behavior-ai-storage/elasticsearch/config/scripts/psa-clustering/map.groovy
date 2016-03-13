userVisit = [
        id: doc['clientip.raw'].value + doc['agent.raw'].value,
        timestamp: doc['timestamp'].value,
        path: [doc['path_category.raw'].value]
];
for (user in _agg.users) {
    if (user.id == userVisit.id) {
        user.path.add(userVisit.path[0]); return;
    }
};
_agg.users.add(userVisit);