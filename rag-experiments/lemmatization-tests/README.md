# Convert Book To Nominative Vocabulary List

This isn't exactly RAG, it goes back to my attempts
to find the nominal form of Russian Words.

My goal was to be able to feed in Russian novel and have it find the nominative
forms of the most commonly used words and then auto translate them, such that I can
feed them into my digital flag card system

I will eventually move this over to a proper place in another project, but this was
a quick and dirty place to put it

## keywords to help me find this later :-)

* Russian, Vocabulary, Lemmatization, nominative case, translation, pymorph2,
  nltk, NLP

## Notes

* I've placed data to ingest in the data folder but am not checking it in, because copyrights :-)
* For my own reference the raw text versions of books I am using are stored on the thumb drive connected to the network
  router as well as on the Thinkpad machine, look there if you can't find them :-)
* This is the project where I was doing similar experiments a while ago https://github.com/chriswininger/russianflashcards
  I will probably eventually move stuff there though I suspect I may not care to re-use much of what's
  there

### Libraries Used
* [pymorph2](https://pymorphy2.readthedocs.io/en/stable/user/guide.html): This is the best library
  I've found for getting the nominative case of Russian words. All the docs are in Russian so you know its
  good `pip install pymorphy2`
* Also using nltk `pip install nltk`
