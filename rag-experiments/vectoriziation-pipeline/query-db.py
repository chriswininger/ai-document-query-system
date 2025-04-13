import sys

from langchain_community.embeddings import HuggingFaceEmbeddings
from langchain_community.vectorstores import FAISS

def main():
    if len(sys.argv) < 1:
        print('you must provide a path to the vector database')
        sys.exit(1) 

    embeddings = HuggingFaceEmbeddings(
            model_name="sentence-transformers/all-MiniLM-l6-v2")

    path_to_db = sys.argv[1]
    db = FAISS.load_local(path_to_db, embeddings, allow_dangerous_deserialization=True)

    query = "Whrite a bash script to loop through files in a folder?"
    docs_matching = db.similarity_search(query)

    print(f"Num matching docs: {len(docs_matching)}")
    print("Top Matching Doc")
    print(docs_matching[1].page_content)


if __name__ == "__main__":
    main()
