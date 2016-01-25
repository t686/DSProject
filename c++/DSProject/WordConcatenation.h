#ifndef WORDCONCATENATION
#define WORDCONCATENATION

#include <QString>
#include <QThread>
#include <vector>
#include "globals.h"
#include "globalclient.h"

class WordConcatenation : public QObject{

public:
	WordConcatenation();

	bool concatString();
	bool checkAddedWords();
	void clearList();
    std::vector<QString> getAddedStrings();
    void setWordSet(std::vector<QString> wordSet);

private:
    std::vector<QString> rndWordSet;
    std::vector<QString> addedStrings;

	QString getRndString();
};

#endif //WORDCONCATENATION
