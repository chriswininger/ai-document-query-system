# All these symantic chunkers are pretty slow on local (though I haven't tested with my good GPU yet
# It might be worth seeing what cost and speed of a service would be for a one off project
from langchain_community.document_loaders import AsyncHtmlLoader
from langchain_community.document_transformers import Html2TextTransformer
from langchain_experimental.text_splitter import SemanticChunker
from langchain_community.embeddings import HuggingFaceEmbeddings

url="https://en.wikipedia.org/wiki/2023_Cricket_World_Cup"

loader = AsyncHtmlLoader(url)

data = loader.load()

# dump raw data
print(data)

html2text = Html2TextTransformer()
data_transformed = html2text.transform_documents(data)

text=data_transformed[0].page_content
model_name = "BAAI/bge-large-en-v1.5"
encode_kwargs = {'normalize_embeddings': True}
text_splitter = SemanticChunker(HuggingFaceEmbeddings(model_name=model_name, encode_kwargs=encode_kwargs))
documents = text_splitter.create_documents([text])

print("Number of Documents: " + len(documents))
for doc in documents:
  print(doc.page_content)
  print("---")