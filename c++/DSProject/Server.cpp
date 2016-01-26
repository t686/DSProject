#include "Server.h"

int Server::stoppedNodes = 0;

Server::Server() : XmlRpcServer(){    

    listen(QHostAddress::Any, glb::port);
    registerSlot(this, SLOT(join(const QVariant&)),"/xmlrpc/");
	registerSlot(this, SLOT(signOff(const QVariant&)), "/xmlrpc/");
	registerSlot(this, SLOT(startElection()), "/xmlrpc/");
	registerSlot(this, SLOT(rpcElectionRequest(const QVariant&)), "/xmlrpc/");
	registerSlot(this, SLOT(hostBroadcast(const QVariant&)), "/xmlrpc/");
	registerSlot(this, SLOT(startConcatProcess()));
	registerSlot(this, SLOT(rpcLifeSign(const QVariant&)), "/xmlrpc/");
	registerSlot(this, SLOT(checkConcatResult()), "/xmlrpc/");
	registerSlot(this, SLOT(echo(const QVariant&)), "/xmlrpc/");
	registerSlot(this, SLOT(rpcRequestString()), "/xmlrpc/");
	registerSlot(this, SLOT(rpcOverrideString(const QVariant&)), "/xmlrpc/");

	concatObject = new WordConcatenation();

	critSectionBusy = isRunning = false;

	xmlRpcClient = new Client(glb::host, glb::port);
	xmlRpcClient->init();
}

void Server::init() {
	std::cout << std::endl << "Server starting..." << std::endl;

    glb::connectedNodes.push_back(Client::nodeIPnPort);
    Client::serverURLs.push_back(Client::nodeIPnPort);

	std::cout << std::endl << "Server succesfuly started!" << std::endl;
}

QVariant Server::join(const QVariant &newNodeIPVar){
	QString newNodeIP = newNodeIPVar.toString();
	if (!glb::connectedNodes.contains(newNodeIPVar)){
		glb::connectedNodes.append(newNodeIPVar);
		std::cout << std::endl << "[Server] NEW node with address: " << newNodeIP.toStdString() << " was connected!" << std::endl;
	}
	if (!Client::serverURLs.contains(newNodeIP))
		Client::serverURLs.push_back(newNodeIP);
    return QVariant::fromValue(glb::connectedNodes);
}

QVariant Server::signOff(const QVariant &nodeIPVar){
	QString nodeIP = nodeIPVar.toString();
    for (size_t n = 0; n < glb::connectedNodes.size(); n++){
        if (glb::connectedNodes[n] == nodeIP){
			std::cout << std::endl << "[Server] Node " << nodeIP.toStdString() << " is leaving the network." << std::endl;

            glb::connectedNodes.erase(glb::connectedNodes.begin() + n);

			for (int i = 0; i < Client::serverURLs.size(); i++){
				QString url = Client::serverURLs[i];
				if (url == nodeIP){
					Client::serverURLs.erase(Client::serverURLs.begin() + i);
				}
			}
            if (glb::host == nodeIP.left(nodeIP.indexOf(":")))
				startElection();
			return QVariant::fromValue(true);
		}
	}
	return QVariant::fromValue(false);
}

QVariant Server::startElection(){
    if (!(glb::connectedNodes.size() > 1)) {
		std::cout << std::endl << "you are not connected to a network" << std::endl;
	}
    else if (Bully::startElection(glb::port, glb::connectedNodes)) {
		glb::host = Client::nodeIPnPort.left(Client::nodeIPnPort.indexOf(":"));
		glb::port = Client::nodeIPnPort.right(Client::nodeIPnPort.length() - Client::nodeIPnPort.indexOf(":") - 1).toInt();
		std::cout << std::endl << "[Server] This application (" << glb::host.toStdString() << ":" << glb::port << ") won the host election" << std::endl;
		broadcastIamHost();
	}
	return QVariant::fromValue(true);
}

void Server::broadcastIamHost(){
	QList<QVariant> params;
    params.push_back(glb::host + QString(":%1").arg(glb::port));

	foreach(QVariant node, glb::connectedNodes) {
		if (node.toString() == xmlRpcClient->nodeIPnPort)
			hostBroadcast(params[0]);
		else{
			std::cout << std::endl << "broadcasting host to " << node.toString().toStdString() << std::endl;
			Client* tmpClient = new Client();
			tmpClient->setHostAndPort(node.toString());
			tmpClient->execute("hostBroadcast", params);
		}
	}
}

QVariant Server::rpcElectionRequest(const QVariant &requesterVar) {
	int requester = requesterVar.toInt();

	std::cout << std::endl << "[Server] received an election message from: " << requester << std::endl;

	//this should not happen
	if (requester > glb::port)
		return QVariant::fromValue(QString("Continue"));

	//this servers port has a higher value then the requester so it takes over the election process
	startElection();
	return QVariant::fromValue(QString("Stop"));
}

QVariant Server::hostBroadcast(const QVariant &newHostVar){
	QString newHost = newHostVar.toString();
	glb::host = newHost.left(newHost.indexOf(":"));
	glb::port = newHost.right(newHost.length() - newHost.indexOf(":") - 1).toInt();
	std::cout << std::endl << "set new host to " << newHost.toStdString() << std::endl;
	return QVariant::fromValue(true);
}

QVariant Server::startConcatProcess(){
	if (Client::nodeIPnPort.right(Client::nodeIPnPort.length() - Client::nodeIPnPort.indexOf(":") -1).toInt() == glb::port){
		return QVariant::fromValue(false);
	}
	bool keepGoing = true;
	concatObject->clearList();

	xmlRpcClient->setHostAndPort(glb::host + QString(":%1").arg(glb::port));

	while (keepGoing) {
        thread()->sleep((double)(rand()%200)/100.0 + 0.5);
		keepGoing = concatLoop();
	}
	QCoreApplication::processEvents();
	return QVariant::fromValue(concatObject->checkAddedWords());
}

bool Server::concatLoop(){
	QList<QVariant> params;
	params.push_back(Client::nodeIPnPort);
	QVariant response = "";

	while (true) {

		Client* tmpClient = new Client;
		tmpClient->setHostAndPort(xmlRpcClient->getHostAndPort());
		tmpClient->execute("rpcLifeSign", params);
		while (tmpClient->isWaiting())
            QCoreApplication::processEvents();

		response = tmpClient->response();

		if (response.toString() == "goOn"){
			concatObject->concatString();
			return true;
		}
		else if (response.toString() == "wait")
            thread()->sleep(0.05);
		else if (response.toString() == "stop")
			return false;

		std::cout << std::endl << "rpcLifeSign call failed" << std::endl;
		return false;
	}
}

QVariant Server::rpcLifeSign(const QVariant &requesterVar){
	QString requester = requesterVar.toString();
	if (!isRunning){
		startTime = clock();
		isRunning = true;
	}
	if (checkElapsedTime()) {
		stoppedRequester++;
        if (stoppedRequester == glb::connectedNodes.size() - 1) broadCastCheckConcat();
		return QVariant::fromValue(QString("stop"));
	}
	if (critSectionBusy) {
		requestQueue.push_back(requester);
		return QVariant::fromValue(QString("wait"));
	}
	else {
		if (requestQueue.size() == 0 || requestQueue[requestQueue.size()-1] == requester) {
			if (requestQueue.size()>0)
				requestQueue.pop_back();
			return QVariant::fromValue(QString("goOn"));
		}
		return QVariant::fromValue(QString("wait"));
	}
	//TODO catch every possible scenario
}

QVariant Server::rpcOverrideString(const QVariant &newStringVar){
	QString newString = newStringVar.toString();
	hostString = newString;
	std::cout << std::endl << "new hostString: " << hostString.toStdString();
	return QVariant::fromValue(true);
}

QVariant Server::checkConcatResult(){
	return QVariant::fromValue(concatObject->checkAddedWords());
}

bool Server::checkElapsedTime(){
	double elapsed = (double)(clock() - startTime) / (double)CLOCKS_PER_SEC;
	bool timeOver = elapsed > 6;
	if (timeOver)
		isRunning = false;
	return timeOver;
}

void Server::lockCritSection(){
	critSectionBusy = true;
}

void Server::unlockCritSection(){
	critSectionBusy = false;
}

void Server::broadCastCheckConcat(){

}

QVariant Server::rpcRequestString(){
	return QVariant::fromValue(hostString);
}

Client* Server::getClient(){
	return xmlRpcClient;
}