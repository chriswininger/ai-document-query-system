from huggingface_hub import snapshot_download


def main():
    model_name = 'all-MiniLM-L6-v2'
    model_id = f"sentence-transformers/{model_name}"
    output_dir = f'./downloaded-embeddings/{model_name}/'

    print(f"downloading {model_name} to {output_dir}")

    snapshot_download(repo_id=model_id, local_dir=output_dir)

    print('done')


if __name__ == '__main__':
    main()
