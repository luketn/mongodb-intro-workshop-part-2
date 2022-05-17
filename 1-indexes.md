### Indexes Overview
What's a MongoDB index? A b-tree in which the leaf nodes are also like a sorted linked-list which can be traversed in order.
![image](https://user-images.githubusercontent.com/1756555/168875467-a6eaed01-5f3c-49ce-9d7d-5a3fdff8959a.png)
Ref: https://www.dbkoda.com/blog/2017/10/06/Effective-MongoDB-indexing

#### Simple (single-field) Indexes
The simplest, and fastest, index is on a single scalar value. e.g.  
```
{
  "simpleStringField": 1
}
```

What's great about these, is that they are very compact, have no complexity regarding sorting and can are extremely fast to query.

When to use: 
- when you have a common query which uses only a single field


#### Compound Indexes
A compound index is an index over multiple scalar value fields. e.g.   
```
{
  "companyId": 1,
  "createdDate": -1,
  "name": 1
}
```

A compound index is very powerful - but also carries some complexity and risks. 

Using a compound index you can perform both query filtering and sorting using multiple fields. The order of each field in the index is very 
significant though, and can cause the index to be useful or not. 

One of the worst performing operations in MongoDB is an in-memory sort. In the example index above, note that the createdDate is the second 
field in the index. Because of the index traversal possible on the leaf nodes of the tree, a $gt (greater than) or an exact date query can 
both be used on the date to filter to index keys in which to search for a name value. 

When to use:
- when you have multiple fields to query
- when you have one or more fields to query, followed by a field to sort on


#### Multi-Key Indexes
A multi-key index is an index on an array field (and possibly more scalar value fields as a compound multi-key index).

e.g. document:
```
{
  "names": [  
    "Gawain",
    "Luke",
    "Thompson"
  ]
}
```

index:
```
{
  "names": 1
}
```

Compound indexes may have at most one array field included in the index. This is intended to limit the blow out of the index size, due to 
the size of the index being the product of the two array lengths.

When to use:
- Sparingly!
- When you have an embedded array of scalar values, or an array of subdocuments from which you want to index a particular field
- When you want to manually tokenize another field(s) and create a searchable array of values from it

WARNING: Be very careful with this - you can easily wind up creating huge indexes, which may remove or reduce the value of the index in
the first place if it is so large it doesn't stay in memory and takes a long time to load from disk and then search through.


#### Partial indexes
A partial index is simply an index which does not create an entry for every document in the collection.
Ref: https://www.mongodb.com/docs/manual/core/index-partial/?jmp=university

e.g.
```
{
  "grade": 1,
},
{
  "partialFilterExpression": {grade: {$gt: 3}}
}
```

If an index is not partial, every document in the collection will have at least one corresponding key in the index (even if the value of 
the indexed field is missing).

With a partial index, you can specify a filter which excludes certain documents from the index. This is great because it reduces the size
of the index, and makes the index both more likely to be resident in memory as well as faster to search. 

There is a potential risk with partial indexes though, that the index would be ignored in your search. This is because the filter _must_
include the index filter predicate in order for the index planner to select that index for the search. 
i.e. in the example above, the query must state $gt: 3 for the index to be used:
```
db.col.find({grade: {$gt: 3}})
```



