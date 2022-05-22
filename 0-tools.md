# Tools for MongoDB Performance
There are two broad categories of performance tools. If you're using a local cluster vs using an Atlas cloud or on-prem Enterprise cluster.

## Local Tools

### explain()
The main tool I use for performance optimization (both locally and remotely) is the explain() function.

Using explain(), e.g.  
```
db.someCollection.find({some: 'thing'}).sort({some:-1}).limit(5).explain('executionStats');
```

What are we looking for here? A low number for Query Targeting (index / documents scanned divided by documents returned). This equates to how much work was done potentially unnecessarily if we had an index to speed up access for the query.

Note that we should only optimize first the things which are very frequently done and done over lots of data. For example for a very small dataset of 100 documents, there may be a small benefit in an index as the whole collection might be loaded into RAM and scanned so quickly.


## Cluster Tools

### Logs
Logs are really important in MongoDB - especially for diagnosing problems after the fact. By default, Mongo records any query that takes longer than 100ms in the logs. These should be rare. 

You can also dial up the level of logging (at least locally). For example you can watch the query planner selection process and execution - examine what the engine does on a local copy of the DB by watching the log output with a higher log level:
```
db.setLogLevel(5)
db.someCollection.find({"some" : "thing"})
db.setLogLevel(-1)
```

### MongoDB Atlas GUI
The MongoDB Atlas GUI has comprehensive UI tools to examine performance at runtime of your database.

These tools can be vital to understand how the system behaves under load. 

I tend to use a load test on my actual services 


### Remote Monitoring GUI
You can also get a light version of the Atlas GUI tools for any local cluster just by running this command:
```
db.enableFreeMonitoring()
```
<img width="1440" alt="image" src="https://user-images.githubusercontent.com/1756555/169685457-8b7a3337-38ca-4f5d-afda-e183149d5b3b.png">

