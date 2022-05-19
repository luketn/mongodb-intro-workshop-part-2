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

What's great about these, is that they are very compact, have no complexity regarding sorting and can are extremely fast to query and to update.

When to use: 
- when you have a common query which uses only a single field
- when performance for writes is crucial


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

Using a compound index you can perform both query filtering and sorting using multiple fields. The order of each field in the index is very significant though, and can cause the index to be useful or not. 

One of the worst performing operations in MongoDB is an in-memory sort. In the example index above, note that the createdDate is the second field in the index. Because of the index traversal possible on the leaf nodes of the tree, a $gt (greater than) or an exact date query can both be used on the date to filter to index keys in which to search for a name value. 

Because the b-tree in a compound index can be very deep, it can also slow down insert, update and delete operations which all have to modify the tree. Especially when the tree needs to be rebalanced and split these operations take time.

Be careful about having too many compound indexes on a single collection. They are expensive and cause a lot of work for the database.

When to use:
- when you have multiple fields to query
- when you have one or more fields to query, followed by a field to sort on
- when write performance is less critical than read performance (e.g. when you read more often than write)
- when you have plenty of overhead in memory (allowing the index to be fully loaded into RAM) and plenty of overhead in CPU (allowing complex operations to be performed on the indexes - sorting and rebalancing trees)


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
- The same concerns as compound indexes also apply

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

#### Text Indexes
A text index tokenises a string field into words using the unicode word delimiters. 
Ref: https://www.mongodb.com/docs/manual/core/index-text/

This is similar to the multi-key index, in that the words split from the original string field are essentially turned into an array of strings (lower-cased).

The search does a logical OR on the words queried with the words in the index.

e.g. 
Create these documents:
```
db.fruits.insert({fruit: "Apple", description:"Crisp, crunchy and delicious"})
db.fruits.insert({fruit: "Orange", description:"Tart, juicy and delicious"})
```
Create this index:
```
db.fruits.createIndex({description: 'text'}, {name: 'text_search'})
```
Query it like this:
```
db.fruits.find({$text: {$search: "Juicy"}})
```
And if you want ordered by rank:
```
db.fruits.find({$text: {$search: "juicy delicious"}}, {score: {$meta: "textScore"}}).sort({score: {$meta: "textScore"}})
```





