import os
import sys
import time
from datetime import datetime
import psycopg2

from langchain_experimental.text_splitter import SemanticChunker
from langchain_community.embeddings import HuggingFaceEmbeddings

def main():
    if (len(sys.argv) < 2):
        print('file path is a required argument')
        sys.exit(1)

    file_path = sys.argv[1]
    file_name = os.path.basename(file_path)
    input_text = read_file(file_path)

    # === create our document chunker ===
    model_name = "BAAI/bge-large-en-v1.5"
    encode_kwargs = {'normalize_embeddings': True}
    text_splitter = SemanticChunker(
            HuggingFaceEmbeddings(
                model_name=model_name,
                encode_kwargs=encode_kwargs
            ),
            # https://github.com/langchain-ai/langchain/discussions/18802#discussioncomment-9571544
            # This pushes it towards smaller chunks because our doc store does no like some of the large
            # chunks this was producing
            breakpoint_threshold_type="percentile",
            breakpoint_threshold_amount=95
    )

    start = time.perf_counter()

    chunks = text_splitter.create_documents([input_text])

    end = time.perf_counter()

    print(f'Time taken to chunk: {end - start}')
    print(f"Number of Chunks: {len(chunks)}")

    persist_chunks_to_db(file_name, chunks, input_text)


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


def persist_chunks_to_db(source_name, chunks, full_text):
    try:
        # Database configuration - replace with your actual values
        conn = psycopg2.connect(
            dbname="spring-ai-demo-db",
            user="postgres",
            password="xxx",
            host="localhost",
            port="5436"
        )

        cursor = conn.cursor()

        # Current timestamp for created_at and updated_at
        current_time = datetime.now()

        insert_document_sql = """
        INSERT INTO document_import (source_name, non_chunked_content, metadata, created_at, updated_at)
        VALUES(%s, %s, %s, %s, %s)
        RETURNING id
        """

        cursor.execute(insert_document_sql, (source_name, full_text, None, current_time, current_time))
        document_id = cursor.fetchone()[0]  # Get the returned ID

        for chunk in chunks:
            content = chunk.page_content
            metadata = None
            status = "NOT_PROCESSED"

            insert_chunk_query = """
            INSERT INTO document_import_chunk
               (document_import_id, source_name, content, metadata, status, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s)
           """

            cursor.execute(
                insert_chunk_query,
                (document_id, source_name, content, metadata, status, current_time, current_time)
            )

        conn.commit()
        print(f"Successfully inserted {len(chunks)} chunks into the database.")

    except Exception as e:
        print(f"Database error: {e}")
        if conn:
            conn.rollback()
    finally:
        if cursor:
            cursor.close()
        if conn:
            conn.close()


if __name__ == "__main__":
    main()

