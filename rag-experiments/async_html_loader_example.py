# simple example from chapter 3, loading data from wikipedia

from langchain_community.document_loaders import AsyncHtmlLoader
from langchain_community.document_transformers import Html2TextTransformer
from langchain_text_splitters import CharacterTextSplitter
from langchain_text_splitters import HTMLHeaderTextSplitter
from langchain_experimental.text_splitter import SemanticChunker
# from langchain_openai.embeddings import OpenAIEmbeddings
from langchain_community.embeddings import HuggingFaceEmbeddings
from langchain.embeddings import OllamaEmbeddings
import time

url="https://en.wikipedia.org/wiki/2023_Cricket_World_Cup"

loader = AsyncHtmlLoader(url)

data = loader.load()

# dump raw data
print(data)

html2text = Html2TextTransformer()
data_transformed = html2text.transform_documents(data)

print(data_transformed[0].page_content)

## CHUNKING EXAMPLES (Fixed Size)

# Chunk By Character (in this case splitting on newlines)
# we could instead set sparators=["\n\n", "\n", ".", " "]
# it would help ensure that we keep chunks closer to the specified size
# by letting it to try to split on each character starting with the left most
# and moving right
text_splitter = CharacterTextSplitter(
  separator="\n",
  chunk_size=1000, # num characters in each chunk
  chunk_overlap=200, # num overlapping between chunks
)

chunks = text_splitter.create_documents([data_transformed[0].page_content])

print(f"The number of chunks created: {len(chunks)}")

print(chunks[4].page_content)
print("->")
print(chunks[5].page_content)

## Chuncking Exmaples (Token based)

# "Split by Token" method. In Split by Token method, the splitting still happens based on a character, but the size of the chunk and the overlap is determined by the number of tokens instead of number of characters.

# Different LLMs use different methods for creating these tokens. OpenAI uses a tokenizer called tiktoken for GPT3.5, GPT4 models, Llama2 by Meta uses LLamaTokenizer that is available in the transformers library by HuggingFace. You can also explore other tokenizers on HuggingFace. NLTK, spaCy are some other popular libraries that can be used as tokenizers.

# To use the split by token method, you can use specific methods within the RecursiveCharacterTextSplitter and CharacterTextSplitter classes, like RecursiveCharacterTextSplitter.from_tiktoken_encoder (encoding="cl100k_base", chunk_size=100, chunk_overlap=10) for creating chunks of 100 tokens with an overlap of 10 tokens using OpenAI’s tiktoken tokenizer or CharacterTextSplitter.from_huggingface_tokenizer(tokenizer, chunk_size=100, chunk_overlap=10)for creating the same sized chunk using another tokenizer from HuggingFace.

#The limitation of Fixed Size Chunking is that it doesn’t consider the semantic integrity of the text. In other words, the meaning of the text is ignored. It works best in scenarios where data is inherently uniform like genetic sequences, service manuals or uniformly structured reports like survey responses.

#https://livebook.manning.com/book/a-simple-guide-to-retrieval-augmented-generation/chapter-3/v-6/84

## Specialized Chunking

# you may want to consider the document format, specialized methods like these exist
#   * MarkdownHeaderTextSplitter
#   * HTMLHeaderTextSplitter
#   * RecursiveJsonSplitter 

headers_to_split_on = [
  ('h1', 'Header 1'),
  ('h2', 'Header 2'),
  ('h3', 'Header 3'),
  ('h4', 'Header GPT4'),
]

html_splitter = HTMLHeaderTextSplitter(headers_to_split_on=headers_to_split_on)

html_header_splits = html_splitter.split_text_from_url(url)

print(html_header_splits[2].page_content)

## Semnatic Chunking

# Usese embedding to find semantic similarity
# It first cerates groups of three sentences then merges groups that are similar in menaing
#
# In langchain this provided by the experimental langchain_experimental.text_splitter library

print("!!! SEMANTIC CHUNKING")


# get all the text from the document (we could maybe also just get the raw, html, but that's not just the data
# object we have to pull out the contents
text="\n".join([x.page_content for x in data_transformed])

## Hugging Face (Commenting out for now but works)
#text_splitter = SemanticChunker(HuggingFaceEmbeddings())

#documents = text_splitter.create_documents([text])

#for doc in documents:
#  print(doc.page_content)
#  print("---")
#

# r1 version (requires ollama installed on localhost)
embedding_function = OllamaEmbeddings(model="deepseek-r1:8b")
text_splitter = SemanticChunker(embedding_function)

start = time.perf_counter()

documents = text_splitter.create_documents([text])

end = time.perf_counter()
elapsed = end - start


print(f'Time taken: {elapsed}')

# Print the resulting chunks
for doc in documents:
    print(doc.page_content)
    print("---")

