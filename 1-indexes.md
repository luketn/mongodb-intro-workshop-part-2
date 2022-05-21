### Indexes Overview
What's a MongoDB index? A b-tree in which the leaf nodes are also like a sorted linked-list which can be traversed in order.
![image](https://user-images.githubusercontent.com/1756555/168875467-a6eaed01-5f3c-49ce-9d7d-5a3fdff8959a.png)
Ref: https://www.dbkoda.com/blog/2017/10/06/Effective-MongoDB-indexing

### Sample Data
You can insert some sample data for playing with from this file: [fruit.json](./data/fruit.json)  
```
db.fruit.insert(... paste data here ...);
```

### Equality Sort Range (ESR) Rule
The ESR rule is about composing indexes. You should always order the fields in an index in that order so that the index can be used efficiently for querying as well as sorting and range queries. 

#### Simple (single-field) Indexes
The simplest, and fastest, index is on a single scalar value. e.g.  
```
db.fruit.createIndex({name:1}, {name: 'simple_index'})
db.fruit.find({name: "Apple"}).explain('executionStats')
```

What's great about these, is that they are very compact, have no complexity regarding sorting and can are extremely fast to query and to update.

When to use: 
- when you have a common query which uses only a single field
- when performance for writes is crucial


#### Compound Indexes
A compound index is an index over multiple scalar value fields. e.g.   
```
db.fruit.createIndex({supplierId: 1, name: 1}, {name: 'compound_index'});
db.fruit.find({supplierId:0}).sort({name: 1}).explain('executionStats');
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

#### Covered Queries
A super-charge to performance can be achieved by returning only fields within the index from the query. 
For example:
```
db.fruit.find({supplierId:0}, {supplierId: 1, name: 1, _id: 0}).sort({name: 1}).explain('executionStats');
```

For very commonly performed queries, it's even worth creating an index specifically designed for covered queries, that includes all the fields you need.
For example if you have a page on a website, and some list which always needs to be fetched live to display on it a covered query may be worth creating. If the index is a compound index though, all of the 'when to use' caveats apply here too, so you may want to use this sparingly.


#### Multi-Key Indexes
A multi-key index is an index on an array field (and possibly more scalar value fields as a compound multi-key index).

index:
```
{
  "keys": 1
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
  "price": 1,
},
{
  "partialFilterExpression": {price: {$gt: 3}}
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
db.col.find({price: {$gt: 3}})
```

#### Text Indexes
A text index tokenises a string field into words using the unicode word delimiters. 
Ref: https://www.mongodb.com/docs/manual/core/index-text/

This is similar to the multi-key index, in that the words split from the original string field are essentially turned into an array of strings (lower-cased).

The search does a logical OR on the words queried with the words in the index.

e.g. 
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

When to use:
- When you have a simple text search to implement
- Sparingly! (see comments on compound and multi-key indexes)

For more advanced (and better) results on Atlas you can use the Atlas Search (Lucene indexed) feature which is awesome and has lots of good capabilities:
https://www.mongodb.com/atlas/search


#### Wildcard Indexes
A wildcard index allows you to create an index for dynamically changing data where you don't know the schema upfront. 

There are a couple of pretty compelling use-cases for this.

One worth mentioning is when using the attribute pattern. The attribute pattern is where you have a document with properties that are key/value attributes you don't know the keys of upfront like this:
```
{
  "key1": 123,
  "key2": 321
}
```

If you want to allow queries on _one_ of these keys efficiently, you can use a wildcard index:  
```
db.fruits.createIndex({'$**': 1})
```

It is effectively like creating a single-field index on each of the dynamic attribute properties on the document (or a subset of the fields in the document).

Use with extreme caution, as this could easily create massive indexes.
