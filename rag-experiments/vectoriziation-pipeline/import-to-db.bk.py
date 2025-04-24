import sys
import time

from langchain_experimental.text_splitter import SemanticChunker
from langchain_community.embeddings import HuggingFaceEmbeddings

from langchain_community.vectorstores import FAISS


def main():
    if (len(sys.argv) < 1):
        print('file path is a required argument')
        sys.exit(1)

    file_path = sys.argv[1]
    input_text = read_file(file_path)

    # === create our document chunker ===
    model_name = "BAAI/bge-large-en-v1.5"
    encode_kwargs = {'normalize_embeddings': True}
    text_splitter = SemanticChunker(
            HuggingFaceEmbeddings(
                model_name=model_name,
                encode_kwargs=encode_kwargs))

    start = time.perf_counter()

    chunks = text_splitter.create_documents([input_text])

    end = time.perf_counter()

    print(f'Time taken to chunk: {end - start}')
    print(f"Number of Chuunks: {len(chunks)}")

    # === Embed the documents
    embeddings = HuggingFaceEmbeddings(
            model_name="sentence-transformers/all-MiniLM-l6-v2")

    # '!!! WE'RE NOT DOING ANYTHING WITH chunk_embedding, do we need this, does adding docs
    # to vector db automatically convert them?
    start = time.perf_counter()
    chunk_embedding = embeddings.embed_documents(
            [chunk.page_content for chunk in chunks])
    end = time.perf_counter()

    print(f'Time taken to embed: {end - start}')
    print(f"Dymention of the embedding: {len(chunk_embedding[0])}")

    db = FAISS.from_documents(chunks, embeddings)
    # Check the number of chunks that have been indexed
    print(f"Number of chunks indexed: {db.index.ntotal}")

    # === Save For Later ===
    #   we can load and save to the db with
    #   db.save_local(folder_path)/FAISS.load_local(folder_path)
    timestamp = int(time.time())
    db_file_path = f"./output/db_{timestamp}"
    db.save_local(db_file_path)


def read_file(filename):
    try:
        with open(filename, "r") as file:
            return file.read()
    except FileNotFoundError:
        print(f"The file '{filename}' does not exist.")
        sys.exit(1)
    except Exception as e:
        print(f"An unexpected exception occurred: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()

