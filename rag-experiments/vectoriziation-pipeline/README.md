Vectorization Pipeline
=======================

This library is intended to handle the initial intake of documents, saving them into the document_import table
of the database (scheme managed by ai-document-query-system-backend flyWheel migrations).

Currently, the main function of this module `import-to-db.py` performs the semantic chunking. Eventually I'd like
to move this into Java, but I was unable to identify a good package to make the semantic chunking step as simple as
it is Python.

Initially I also planned to handle embedding/storage into the vector db here, but for compatability reasons it seems,
at the moment simpler to deal with that Java.

## Import Books

* First import them to text using calibre
* Then hand them off to import-to-db.py

## Import Webpages

* First convert them to text using https://docs.crawl4ai.com/core/cli/
* Then hand them off to import-to-db.py

### Example

```
crwl https://jimlongsgarden.blogspot.com/2012/09/homemade-hot-sauce.html >> ./vectoriziation-pipeline/input-data/jimlongsgarden_home_made_hot_sauce_full_scrape.json
cat ./jimlongsgarden_home_made_hot_sauce_full_scrape.json | jq -r .markdown.raw_markdown >> ./jimlongsgarden_home_made_hot_sauce_markdown.md
```

Note: There's lots of good metadata in the json file that we probably should not throw away I'm just trying to keep it
simple for now, likely I should have a seprate script of code path for importing crwl json output that passes through
the metadata
