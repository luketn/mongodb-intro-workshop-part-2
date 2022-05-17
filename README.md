# mongodb-performance-workshop
Follow on to mongodb-intro-workshop.

## Examining Performance

### Tools

... todo - list of tools for examining performance in mongo ...


Discuss mongodb performance.  

Indexes: How is an index stored - b-trees, size, in-memory vs disk, number of indexes, query planner.  

Sorting. In memory vs indexed.  
https://www.mongodb.com/docs/manual/tutorial/sort-results-with-indexes/?jmp=university

Using explain(), e.g.  
```
db.someCollection.find({some: 'thing'}).sort({some:-1}).limit(5).explain('executionStats');
```

Query planner selection process and execution - examine what the engine does on a local copy of the DB by watching the log output with a higher log level:
```
db.setLogLevel(5)
db.someCollection.find({"some" : "thing"})
db.setLogLevel(-1)
```


Take a large dataset and query it in optimal and non-optimal ways. Show the effects of different indexes. Discuss the importance of hot indexes being kept in RAM (smaller indexes are more likely to fit in RAM and stay in RAM).

Collection design: document ref / sub document, memory vs disk, cache. Think about the client and query patterns. Ideal scenario is the most common queries return a single document or a small set of data from multiple documents with a single query (as opposed to requiring multiple searches / lookups).  

Memory memory memory - Mongo loves it! Hot indexes must be in memory. 

Use some of the lessons from this;
https://university.mongodb.com/mercury/M201
