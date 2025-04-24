import pymorphy2
import nltk
import csv
import os
import translators as ts

# this seems to be needed to make nltk work
# these will be cached so once it happened once on a system
# it won't re-download them
# https://stackoverflow.com/questions/73744658/resource-punkt-not-found-please-use-the-nltk-downloader-to-obtain-the-resource
nltk.download('punkt')
nltk.download('wordnet')
nltk.download('omw-1.4')
nltk.download('punkt_tab')

def main():
    input_file = './data/harry_potter_book_1.txt'

    # cache of work done in earlier runs so we don't have to re-translate
    cache = read_csv_file('output/cache.csv.bk')

    # === Get Initial List of Word From Book ===
    print(f"reading in data '{input_file}")
    input_data = read_file(input_file)

    tokens = nltk.word_tokenize(input_data, language='russian')
    sentences = nltk.sent_tokenize(input_data, language='russian')
    sentences = [s.lower() for s in sentences]

    print(f'read {len(tokens)} tokens')

    unique_words_sorted_by_popularity = to_unique_words(tokens, sentences)
    print(f'unique_words_sorted_by_popularity: {len(unique_words_sorted_by_popularity)}')

    save_to_csv(
        './output/' + input_name_to_output_name(input_file, '_raw_unique_words.csv'),
        unique_words_sorted_by_popularity
    )

    # === Normalize the Word Into Nominative Case ===
    # TODO: We could also probably save tags, for example it could be interesting to focus on verbs vs nouns
    morph = pymorphy2.MorphAnalyzer()
    normalized_words = normalize_values(unique_words_sorted_by_popularity)

    save_to_csv(
        './output/' + input_name_to_output_name(input_file, '_nominative_case_unique_words.csv'),
        normalized_words
    )

    # === Translate the Words ===
    print('being translated')
    with_translations = add_translation(normalized_words, cache)
    print('done translating')

    save_to_csv(
        './output/' + input_name_to_output_name(input_file, '_final_output.csv'),
        with_translations
    )

    print('done')

def read_file(filename):
    try:
        with open(filename, "r") as file:
            return file.read()
    except FileNotFoundError:
        print(f"The file '{filename}' does not exist.")
    except Exception as e:
        print(f"An unexpected exception occurred: {e}")

def read_csv_file(filename):
    try:
        with open(filename, 'r') as file:
            return list(csv.DictReader(file))
    except Exception as e:
        print(f"An unexpected exception occurred: {e}")


def to_unique_words(tokens, sentences):
    words = {}

    tokens = [t.lower() for t in tokens]
    tokens = filter_words_less_than(tokens, 4)
    tokens = filter_names(tokens)

    for word in tokens:
        if word not in words:
            words[word] = 1
        else:
            words[word] += 1

    return sorted(
        [{ 'word': w, 'count': words[w], 'sentence': find_sentences_with_word(sentences, w)[0] } for w in words],
        key=lambda w: w['count'],
        reverse=True
    )

def save_to_csv(filename, output_dict):
    if len(output_dict) == 0:
        print("can't save an empty dictionary")
    else:
        print(f'saving "{filename}"')
        with open(filename, 'w', newline='') as csvfile:
            # Determine the field names from the keys of the first dictionary
            fieldnames = output_dict[0].keys()
            writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
            writer.writeheader()
            writer.writerows(output_dict)

def input_name_to_output_name(input_file, new_suffix):
    filename = os.path.basename(input_file)
    base_name = os.path.splitext(filename)[0]

    return base_name + new_suffix

def filter_words_less_than(words, n = 4):
    return filter(lambda w: len(w) >= n, words)

def filter_word_objs_less_than(words, key='normalized', n = 4):
    return filter(lambda obj: len(obj[key]) >= n, words)

def filter_names(words):
    # Гарри appears 1366, Рон 321, Гермиона 221
    names = [
        'огрид', 'огрида', 'огриду', 'огридом',
        'страунс', 'страунса', 'страунсом', 'страунсу', 'страунсе',
        'гермиона','гермионой', 'гермионы', 'гермионе', 'гермиону',
        'гриффиндора', 'гриффиндор', 'гриффиндору', 'гриффиндоре',
        'думбльдора', 'думбльдору', 'думбльдором', 'думбльдоре',
        'гриффиндорской', 'гриффиндорскую', 'гриффиндорский', 'гриффиндорского', 'гриффиндорские',
        'гарри',
        'рон', 'рона',
        'дудли',
        'думбльдор',
        'вернон',
        'макгонаголл',
        'поттер',
        'малфой',
        'петуния',
        'дурслей',
        'перси',
        'филча',
        'филч',
        'злей',
        'невилл',
        'уизли',
        'хогварц', ' хогварце', 'хогварца',
        'trademarks', 'isbn', '978-1-78110-188-9', 'related', 'indicia', 'characters', 'names'
    ]

    return filter(lambda w: w not in names, words)

def consolidate_normalized_word(normalized_words_raw):
    unique_entries = {}

    for entry in normalized_words_raw:
        key = entry['normalized']
        if key in unique_entries:
            unique_entries[key]['count'] += entry['count']
            unique_entries[key]['word'] += ';; ' + entry['word']
            unique_entries[key]['sentence'] += ';; ' + entry['sentence']
            unique_entries[key]['tags'] += ';;' + entry['tags']
        else:
            unique_entries[key] = entry

    new_list = [unique_entries[key] for key in unique_entries]

    return sorted(
        new_list,
        key=lambda w: w['count'],
        reverse=True
    )

def add_translation(normalized_words, cache):
    with_translation = []
    cnt = 0

    size = len(normalized_words)
    for entry in normalized_words:
        word = entry['normalized']

        cached_translation = find_translation_in_cache(cache, word)

        if cached_translation is not None and cached_translation != 'ERROR':
            with_translation.append({**entry, 'translation': cached_translation})
        else:
            try:
                translation = translate_from_russian_to_english(word)
                with_translation.append({ **entry, 'translation': translation })
                cnt += 1

                print(f"translated {cnt}/{size}")
            except Exception as ex:
                print(f'Error translating word "{word}" at count: ${cnt}: "${ex}"')
                with_translation.append({**entry, 'translation': 'ERROR' })

    return  with_translation

def translate_from_russian_to_english(word):
    return ts.server.bing(word, from_language='ru', to_language='en', professional_field='general')

def find_translation_in_cache(cache, word_to_find):
    for entry in cache:
        if entry['normalized'] == word_to_find:
            return entry['translation']

    return None

def find_sentences_with_word (sentences, word):
    return list(filter(lambda sentence: word in sentence, sentences))

def normalize_values(unique_words_sorted_by_popularity):
    morph = pymorphy2.MorphAnalyzer()
    normalized_words = []
    for entry in unique_words_sorted_by_popularity:
        info = morph.parse(entry['word'])[0]

        tags = [info.tag.POS, info.tag.aspect, info.tag.gender]
        tags = list(filter(lambda tag: tag is not None, tags))

        normalized_words.append({
            **entry,
            'normalized': info.normal_form,
            'tags': ','.join(tags)
        })

    normalized_words = list(filter_word_objs_less_than(normalized_words))

    return consolidate_normalized_word(normalized_words)

if __name__ == '__main__':
    main()
