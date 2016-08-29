String id = null
if (doc.containsKey('clientip.raw')) {
    id = doc['clientip.raw'].value + doc['agent.raw'].value
    id = id.bytes.encodeBase64().toString()
}

if (doc.containsKey('tracking_id.raw')) {
    id = doc['tracking_id.raw'].value
}

String pathCategory = null
if (doc.containsKey('path_category.raw')) {
    pathCategory = doc['path_category.raw'].value
}

userVisit = [
        id:id,
        path: [
            [
                item: pathCategory,
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