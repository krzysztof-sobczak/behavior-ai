String id = doc['clientip.raw'].value + doc['agent.raw'].value
id = id.bytes.encodeBase64().toString()
userVisit = [
        id:id,
        path: [
            [
                item: doc['path_category.raw'].value,
                timestamp: doc['timestamp'].value
            ]
        ]
];
for (user in _agg.users) {
    if (user.id == userVisit.id) {
        user.path.add(userVisit.path[0]); return;
    }
};
_agg.users.add(userVisit);