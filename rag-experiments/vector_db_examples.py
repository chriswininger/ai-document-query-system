# To store embeddings a new kind of database called Vector Database has gained popularity.

# Apart from efficiently storing high dimensional vector data, modern vector databases offer traditional features like scalability, security, multi-tenancy, versioning & management, etc. However, vector databases are unique in offering similarity search based on Euclidian Distance or Cosine Similarity. They also employ specialized indexing techniques.

# propelled by the growth in demand for storing vector data, all major database providers have added vector indexing capability. We can categorize the popular vector databases available today into six broad categories.

# Vector Indices: These are libraries that focus on the core features of indexing and search. They do not support data management, query processing, interfaces etc. They can be considered a bare bones vector database. Examples of vector indices are Facebook AI Similarity Search (FAISS), Non-Metric Space Library (NMSLIB), Approximate Nearest Neighbors Oh Yeah (ANNOY), Scalable Nearest Neighbors (ScaNN), etc.

# Specialized Vector Databases: These databases are focused on the core feature of high-dimensional vector support, indexing, search, and retrieval, like vector indices, but also offer database features like data management, extensibility, security, scalability, non-vector data support etc. Examples of specialized vector DBs are Pinecone, ChromaDB, Milvus, Qdrant, Weaviate, Vald, LanceDB, Vespa, Marqo, etc.

# Search Platforms: Solr, Elastic Search, Open Search, Apache Lucene etc. are traditional text search platforms and engines built for full text search. They have now added vector similarity search capabilities to their existing search capabilities.

# Vector Capabilities for SQL Databases with: Azure SQL, Postgres SQL(pgvector), SingleStore, CloudSQL, etc. are traditional SQL databases that have now added vector data handling capabilities

# Vector Capabilities for NoSQL Databases: Like SQL DBs, NoSQL DBs like MongoDB have also added vector search capabilities.

# Graph Databases with Vector Capabilities: Graph DBs like Neo4j adding vector capabilities has also opened new possibilities.

from langchain_community.document_loaders import AsyncHtmlLoader
from langchain_community.document_transformers import Html2TextTransformer
from langchain_experimental.text_splitter import SemanticChunker
from langchain_community.embeddings import HuggingFaceEmbeddings
from langchain_community.vectorstores import FAISS

import time

url = "https://en.wikipedia.org/wiki/2023_Cricket_World_Cup"

loader = AsyncHtmlLoader(url)

data = loader.load()

html2text = Html2TextTransformer()
data_transformed = html2text.transform_documents(data)

text=data_transformed[0].page_content
model_name = "BAAI/bge-large-en-v1.5"
encode_kwargs = {'normalize_embeddings': True}
text_splitter = SemanticChunker(
        HuggingFaceEmbeddings(
            model_name=model_name,
            encode_kwargs=encode_kwargs))

start = time.perf_counter()

chunks = text_splitter.create_documents([text])

end = time.perf_counter()

print(f'Time taken to chunk: {end - start}')
print(f"Number of Chuunks: {len(chunks)}")

embeddings = HuggingFaceEmbeddings(
        model_name="sentence-transformers/all-MiniLM-l6-v2")

start = time.perf_counter()
chunk_embedding = embeddings.embed_documents(
        [chunk.page_content for chunk in chunks])
end = time.perf_counter()

print(f'Time taken to embed: {end - start}')
print(f"Dymention of the embedding: {len(chunk_embedding[0])}")


db=FAISS.from_documents(chunks,embeddings)
# Check the number of chunks that have been indexed
print(f"Number of chunks indexed: {db.index.ntotal}")

# we can load and save to the db with  db.save_local(folder_path)/FAISS.load_local(folder_path)
query = "Who won the 2023 Cricket World Cup?"
docs_matching = db.similarity_search(query)

print("Top Matching Doc")
print(docs_matching[1].page_content)
