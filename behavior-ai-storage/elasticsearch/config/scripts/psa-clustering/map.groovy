String id = doc['clientip.raw'].value + doc['agent.raw'].value
id = id.bytes.encodeBase64().toString()
userVisit = [
        id:id,
        timestamp: doc['timestamp'].value,
        path: [doc['path_category.raw'].value]
];
for (user in _agg.users) {
    if (user.id == userVisit.id) {
        user.path.add(userVisit.path[0]); return;
    }
};
_agg.users.add(userVisit);