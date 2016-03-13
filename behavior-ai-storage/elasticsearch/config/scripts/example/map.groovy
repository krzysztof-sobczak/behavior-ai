if (doc['path_category.raw'].value == "INBOX") {
    _agg.transactions.add(doc['clientip.raw'].value)
}