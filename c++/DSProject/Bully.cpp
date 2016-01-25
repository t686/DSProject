#include "Bully.h"

Bully::Bully() : QObject(){

}

bool Bully::startElection(int ownPort, QList<QVariant> connectedNodes) {
	QString response;

    std::cout << std::endl << "node " << ownPort << " starts election process!";
    foreach (QVariant node, connectedNodes) {
		if (extractPortFromIPnPort(node.toString()) <= ownPort) continue;
		response = messageNode(ownPort, node.toString());

		if (response == "Continue"){
			//TODO: implement
		}
		else if (response == "Stop"){
			std::cout << std::endl << "[Bully] " << node.toString().toStdString() << " is taking over the election process";
			return false;
		}
		else if (response == "Lost"){
			std::cout << std::endl << "[Bully] " << node.toString().toStdString() << " is not answering therefore will be disconnected";
			signOffDisconnectedNode(node.toString());
		}
		else
            std::cout << std::endl << "Something went wrong!";
	}
	return true;
}

QString Bully::messageNode(int ownPort, QString node) {

	QList<QVariant> params;
    std::cout << std::endl << "sending election message to: " << node.toStdString();
    glbClient::client->setHost(Client::getFullAddress(Client::urlFormatter(node)));
	params.append(ownPort);

    glbClient::client->execute("rpcElectionRequest", params);

    while (glbClient::client->isWaiting())
        QCoreApplication::processEvents();

    QString response = glbClient::client->response().toString();

	if (response.isEmpty()) {
		return "Lost";
	}

	return response;
}

bool Bully::signOffDisconnectedNode(QString node) {
	QList<QVariant> params;

    glbClient::client->setHost(Client::getFullAddress(Client::getFullAddress(Client::urlFormatter(Client::nodeIPnPort))));
	params.append(node);

    glbClient::client->execute("signOff", params);
    while (glbClient::client->isWaiting())
        QCoreApplication::processEvents();

    return glbClient::client->response().toBool();
}

int Bully::extractPortFromIPnPort(QString nodeIPnPort) {
	int port;
	QString portString = nodeIPnPort.right(nodeIPnPort.length() - nodeIPnPort.indexOf(":"));
	port = portString.toInt();

	return port;
}
