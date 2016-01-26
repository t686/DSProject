#include "Bully.h"

Bully::Bully() : QObject(){
	
}

bool Bully::startElection(int ownPort, QList<QVariant> connectedNodes) {
	QString response;

    std::cout << std::endl << "node " << ownPort << " starts election process!" << std::endl;
    foreach(QVariant node, connectedNodes) {
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
	Client* xmlRpcClient = new Client();
    xmlRpcClient->setHostAndPort(node);
	params.append(ownPort);

    xmlRpcClient->execute("rpcElectionRequest", params);

    while (xmlRpcClient->isWaiting())
        QCoreApplication::processEvents();

    QString response = xmlRpcClient->response().toString();

	if (response.isEmpty() || response.isNull()) {
		return "Lost";
	}

	return response;
}

bool Bully::signOffDisconnectedNode(QString node) {
	QList<QVariant> params;

	Client* xmlRpcClient = new Client();
    xmlRpcClient->setHostAndPort(Client::nodeIPnPort);
	params.append(node);

    xmlRpcClient->execute("signOff", params);
    while (xmlRpcClient->isWaiting())
        QCoreApplication::processEvents();

    return xmlRpcClient->response().toBool();
}

int Bully::extractPortFromIPnPort(QString nodeIPnPort) {
	return nodeIPnPort.right(nodeIPnPort.length() - nodeIPnPort.indexOf(":") - 1).toInt();
}
