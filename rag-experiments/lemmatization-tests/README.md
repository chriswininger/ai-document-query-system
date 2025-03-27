# Convert Book To Nominative Vocabulary List

This isn't exactly RAG, it goes back to my attempts
to find the nominal form of Russian Words.

My goal was to be able to feed in Russian novel and have it find the nominative
forms of the most commonly used words and then auto translate them, such that I can
feed them into my digital flag card system

I will eventually move this over to a proper place in another project, but this was
a quick and dirty place to put it

## key words to help me find this later :-)

* Russian, Vocabulary, Lemmatization, nominative case, translation, pymorph2,
  nltk, NLP

## Notes

* I've placed data to ingest in the data folder but am not checking it in, because copyrights :-)

I'm going to play with https://pymorphy2.readthedocs.io/en/stable/user/guide.html

`pip install pymorphy2`

also playing with `pip install nltk`

`python -m nltk.downloader popular`
`python -m nltk.downloader punkt-tab`
nltk.download('punkt_tab')