#include "WordConcatenation.h"

WordConcatenation::WordConcatenation() : QObject(){
	if (!loadFile()) std::cout << std::endl << "File not found!" << std::endl;
	else std::cout << std::endl << "File loaded successfully!" << std::endl;
}

bool WordConcatenation::concatString() {
	QList<QVariant> params;
	QString rndString = getRndString();

	Client* xmlRpcClient = new Client();
	xmlRpcClient->setHostAndPort(glb::host + QString(":%1").arg(glb::port));

	xmlRpcClient->execute("Node.rpcRequestString", params);

	while (xmlRpcClient->isWaiting())
        QCoreApplication::processEvents();

	QString hostString = xmlRpcClient->response().toString();

	hostString.append(rndString);
	addedStrings.append(rndString);
	std::cout << std::endl << rndString.toStdString();
	params.append(hostString);

	Client* xmlRpcClient2 = new Client();
	xmlRpcClient2->setHostAndPort(glb::host + QString(":%1").arg(glb::port));
	xmlRpcClient2->execute("Node.rpcOverrideString", params);

	while (xmlRpcClient2->isWaiting())
        QCoreApplication::processEvents();

    return xmlRpcClient2->response().toBool();
}

bool WordConcatenation::checkAddedWords(){
	QList<QVariant> params;

	Client* xmlRpcClient = new Client();
	xmlRpcClient->setHostAndPort(glb::host + QString(":%1").arg(glb::port));
    xmlRpcClient->execute("Node.rpcRequestString", params);

    while (xmlRpcClient->isWaiting())
        QCoreApplication::processEvents();

    QString hostString = xmlRpcClient->response().toString();

	foreach(QString addedWord,addedStrings) {
		if (hostString.contains(addedWord)) continue;

		std::cout << std::endl << "[WordConcat] The host string does not contain " << addedWord.toStdString() << std::endl;
		return false;
	}
	std::cout << std::endl << "[WordConcat] concatenation complete" << std::endl;
	return true;
}

void WordConcatenation::clearList() {
	addedStrings.clear();
}

QStringList WordConcatenation::getAddedStrings() {
	return addedStrings;
}

QString WordConcatenation::getRndString() {
	return rndWordSet[rand()%rndWordSet.size()];
}

void WordConcatenation::setWordSet(QStringList wordSet) {
	rndWordSet = wordSet;
}

bool WordConcatenation::loadFile() {

	std::ifstream file("wordList.txt");

	if (file.fail()){
		std::cout << std::endl << "Couldn't read wordList.txt" << std::endl;
		return false;
	}

	std::string word;
	while (std::getline(file, word))
		rndWordSet.push_back(QString::fromStdString(word));

	file.close();

	return true;
}