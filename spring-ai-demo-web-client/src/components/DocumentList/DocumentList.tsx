import './DocumentList.css';

export default function DocumentList() {
  return (
    <section className="document-list">
      <fieldset>
        <legend>Included Documents:</legend>
        <div>
          <input type="checkbox" id="coding" name="interest" value="coding"/>
          <label htmlFor="coding">Coding</label>
        </div>
        <div>
          <input type="checkbox" id="music" name="interest" value="music"/>
          <label htmlFor="music">Music</label>
        </div>
      </fieldset>
    </section>);
}
