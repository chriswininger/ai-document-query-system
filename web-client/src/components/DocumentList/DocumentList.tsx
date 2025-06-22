import './DocumentList.css';
import {DocumentImport, useGetAllImportedDocumentsQuery} from "../../api/apiBase.tsx";
import {FetchBaseQueryError} from "@reduxjs/toolkit/query";
import {SerializedError} from "@reduxjs/toolkit";

export default function DocumentList(
  { selectedDocuments, onDocumentSelected }: { selectedDocuments: DocumentImport[], onDocumentSelected: (document: DocumentImport, value: boolean) => void}
) {
  const { data, error, isLoading } = useGetAllImportedDocumentsQuery();

  return (
    <section className="document-list">
      <fieldset>
        <legend>Included Documents:</legend>

        { isLoading
          ? <p role="status">Loading...</p>
          : !error
            ? <Documents documents={data ?? []} selectedDocuments={selectedDocuments} onDocumentSelected={onDocumentSelected} />
            : <ErrorMessage error={error} />
        }
      </fieldset>
    </section>);
}

function Documents(
  { documents, selectedDocuments, onDocumentSelected }: { documents: DocumentImport[], selectedDocuments: DocumentImport[], onDocumentSelected: (document: DocumentImport, value: boolean) => void}
) {
  return documents.map(document => (
          <div title={`id: ${document.id} -- '${document.sourceName}'`} key={document.id} className="document-list-item">
            <input
              type="checkbox"
              id={document.id + ''}
              name={document.sourceName}
              value={document.sourceName}
              checked={!!selectedDocuments.find(d => d.id === document.id)}
              onChange={e => onDocumentSelected(document, e.target.checked) }
            />
            <label htmlFor={document.id + ''}>{document.sourceName}</label>
          </div>
        ))
}

function ErrorMessage({ error }: { error: FetchBaseQueryError | SerializedError}) {
  console.error(error)

  return  <p className="error" role="alert">
    Error loading documents
  </p>
}
