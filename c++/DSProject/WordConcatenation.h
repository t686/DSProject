#ifndef WORDCONCATENATION
#define WORDCONCATENATION

#include <QString>
#include <QThread>
#include <vector>
#include <fstream>
#include "globals.h"
#include "Client.h"

class WordConcatenation : public QObject{

public:
	WordConcatenation();

	bool concatString();
	bool checkAddedWords();
	void clearList();
	QStringList getAddedStrings();
	void setWordSet(QStringList);

private:
	QStringList rndWordSet;
    QStringList addedStrings;

	QString getRndString();

	bool loadFile();
};

#endif //WORDCONCATENATION
