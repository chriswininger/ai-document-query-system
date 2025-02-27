# embeddings can be used for text search, finding document chunks
# closest to the query

#Your application use case may determine your choice of embeddings. The MTEB leaderboard scores each of the embeddings models across seven use cases – Classification, Clustering, Pair Classification, Reranking, Retrieval, Semantic Text Similarity and Summarization. At the time of writing this book, SFR-Embedding-Mistral model developed by Salesforce performs the best for retrieval tasks.

from langchain_community.document_loaders import AsyncHtmlLoader
from langchain_community.document_transformers import Html2TextTransformer
from langchain_experimental.text_splitter import SemanticChunker
from langchain_community.embeddings import HuggingFaceEmbeddings

import time

url = "https://en.wikipedia.org/wiki/2023_Cricket_World_Cup"

loader = AsyncHtmlLoader(url)

data = loader.load()

# dump raw data
print(data)

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

