import './VectorSearch.css'
import {useState} from "react";
import DocumentList from "../../components/DocumentList/DocumentList.tsx";
import {useGetAllImportedDocumentsQuery} from "../../api/apiBase.tsx";

export default function VectorSearch() {
  const [searchValue, setSearchValue] = useState("")

  const { data, error, isLoading } = useGetAllImportedDocumentsQuery();

  if (!isLoading && !error) {
    console.log('!!! much data: ', data);
  }

  return <main className="vector-search-page">
    {!isLoading && <DocumentList /> }

    <section className="search-controls">
      <input
        className="post-input vector-search-input"
        placeholder="Search"
        value={searchValue}
        onChange={(e) => setSearchValue(e.target.value)}
      />

      <button className="search-button" onClick={onSearchClicked}>Go</button>
    </section>
  </main>

  function onSearchClicked() {

  }
}
