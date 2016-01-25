#include "WordConcatenation.h"

WordConcatenation::WordConcatenation() : QObject(){

}

bool WordConcatenation::concatString() {
	QList<QVariant> params;
	QString rndString = getRndString();

    glbClient::client->setHostAndPort(Client::getFullAddress(Client::urlFormatter(glb::host)));

	std::cout << std::endl << "[WordConcat] Requesting host std::string";
	params.append(QVariant());
    glbClient::client->execute("rpcRequestString", params);

    while (glbClient::client->isWaiting())
        QCoreApplication::processEvents();

    QString hostString = glbClient::client->response().toString();

	hostString.append(rndString);
	params.append(hostString);
	std::cout << std::endl << "[WordConcat] Sending new std::string to host";
    glbClient::client->execute("rpcOverrideString", params);

    while (glbClient::client->isWaiting())
        QCoreApplication::processEvents();

    return glbClient::client->response().toBool();
}

bool WordConcatenation::checkAddedWords(){
	QList<QVariant> params;
	params.append(QVariant());
    glbClient::client->execute("rpcRequestString", params);

    while (glbClient::client->isWaiting())
        QCoreApplication::processEvents();

    QString hostString = glbClient::client->response().toString();

	foreach(QString addedWord,addedStrings) {
		if (hostString.contains(addedWord)) continue;

        std::cout << std::endl << "[WordConcat] The host strong does not contain " << addedWord.toStdString();
		return false;
	}
	std::cout << std::endl << "[WordConcat] concatenation complete";
	return true;
}

void WordConcatenation::clearList() {
	addedStrings.clear();
}

std::vector<QString> WordConcatenation::getAddedStrings() {
	return addedStrings;
}

QString WordConcatenation::getRndString() {
	return rndWordSet[rand()%rndWordSet.size()];
}

void WordConcatenation::setWordSet(std::vector<QString> wordSet) {
	rndWordSet = wordSet;
}
