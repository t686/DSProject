#include "Server.h"

int Server::stoppedNodes = 0;

Server::Server() : XmlRpcServer(){    

    listen(QHostAddress::Any, glb::port);
    registerSlot(this, SLOT(join(const QVariant&)));
    registerSlot(this, SLOT(signOff(const QVariant&)));
	registerSlot(this, SLOT(startElection(const QVariant&)));
    registerSlot(this, SLOT(rpcElectionRequest(const QVariant&)));
    registerSlot(this, SLOT(hostBroadcast(const QVariant&)));
	registerSlot(this, SLOT(startConcatProcess(const QVariant&)));
    registerSlot(this, SLOT(rpcLifeSign(const QVariant&)));
	registerSlot(this, SLOT(checkConcatResult(const QVariant&)));
	registerSlot(this, SLOT(echo(const QVariant&)));
	registerSlot(this, SLOT(rpcRequestString(const QVariant&)));
	registerSlot(this, SLOT(rpcOverrideString(const QVariant&)));

	concatObject = new WordConcatenation();
}

void Server::init() {
	std::cout << std::endl << "Server starting..." << std::endl;

    glb::connectedNodes.push_back(Client::nodeIPnPort);
    Client::serverURLs.push_back(Client::getFullAddress(Client::urlFormatter(Client::nodeIPnPort)));

	std::cout << std::endl << "Server succesfuly started!" << std::endl;

	if (!loadFile()) std::cout << std::endl << "File not found!" << std::endl;
	else std::cout << std::endl << "File loaded successfully!" << std::endl;
}

QVariant Server::join(const QVariant &newNodeIPVar){
	QString newNodeIP = newNodeIPVar.toString();
	if (!glb::connectedNodes.contains(newNodeIPVar)){
		glb::connectedNodes.append(newNodeIPVar);
		std::cout << std::endl << "[Server] NEW node with address: " << newNodeIP.toStdString() << " was connected!" << std::endl;
	}
	if (!Client::serverURLs.contains(Client::getFullAddress(newNodeIP)))
		Client::serverURLs.push_back(Client::getFullAddress(newNodeIP));
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
				if (url == Client::urlFormatter(nodeIP)){
					Client::serverURLs.erase(Client::serverURLs.begin() + i);
				}
			}
            if (glb::host == nodeIP.left(nodeIP.indexOf(":")))
				startElection(QVariant());
			return QVariant::fromValue(true);
		}
	}
	return QVariant::fromValue(false);
}

QVariant Server::startElection(const QVariant &dummy){
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

    foreach (QVariant node,glb::connectedNodes) {
		if (glbClient::client->nodeIPnPort == node.toString())
			hostBroadcast(params[0]);
		else{
			glbClient::client->setHostAndPort(Client::getFullAddress(Client::urlFormatter(node.toString())));
			glbClient::client->execute("hostBroadcast", params);
		}
	}
}

QVariant Server::rpcElectionRequest(const QVariant &requesterVar) {
	int requester = requesterVar.toInt();

	std::cout << std::endl << "[Server] received an election message from: " << requester << std::endl;

	//this should not happen
	if (requester > glb::port)
		return QVariant("Continue");

	//this servers port has a higher value then the requester so it takes over the election process
	startElection(QVariant());
	return QVariant("Stop");
}

QVariant Server::hostBroadcast(const QVariant &newHostVar){
	QString newHost = newHostVar.toString();
	glb::host = newHost.left(newHost.indexOf(":"));
	glb::port = newHost.right(newHost.length() - newHost.indexOf(":") - 1).toInt();
	return QVariant::fromValue(true);
}

QVariant Server::startConcatProcess(const QVariant &dummy){
    if (Client::nodeIPnPort == glb::host){
		return QVariant::fromValue(false);
	}
	bool keepGoing = true;
	concatObject->clearList();

	while (keepGoing) {
        thread()->sleep((double)(rand()%50)/10000.0);
		QCoreApplication::processEvents();
		keepGoing = concatLoop();
	}
	concatObject->checkAddedWords();
	return QVariant::fromValue(true);
}

bool Server::concatLoop(){
	QList<QVariant> params;
	params.push_back(Client::nodeIPnPort);
	QVariant response = "";

	while (true) {
        glbClient::client->execute("rpcLifeSign", params);
        while (glbClient::client->isWaiting())
            QCoreApplication::processEvents();

        response = glbClient::client->response();

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
	if (!isRunning) {
		startTime = clock();
	}
	if (checkElapsedTime()) {
		stoppedRequester++;
        if (stoppedRequester == glb::connectedNodes.size() - 1) broadCastCheckConcat();
		return QVariant("stop");
	}
	if (critSectionBusy) {
		requestQueue.push_back(requester);
		return QVariant("wait");
	}
	else {
		if (requestQueue.size() == 0 || requestQueue[requestQueue.size()-1] == requester) {
			requestQueue.pop_back();
			return QVariant("goOn");
		}
		return QVariant("wait");
	}
	//TODO catch every possible scenario
}

QVariant Server::rpcOverrideString(const QVariant &newStringVar){
	QString newString = newStringVar.toString();
	hostString = newString;
	return QVariant::fromValue(true);
}

QVariant Server::checkConcatResult(const QVariant &dummy){
	return QVariant(concatObject->checkAddedWords());
}

bool Server::loadFile() {

	std::ifstream file("wordList.txt");

	if (file.fail()){
		std::cout << std::endl << "Couldn't read wordList.txt";
		return false;
	}

	std::string word;
    while (std::getline(file,word))
		rndWordSet.push_back(QString::fromStdString(word));

	file.close();

	return true;
}

bool Server::checkElapsedTime(){
	double elapsed = (double)(clock() - startTime) / (double)CLOCKS_PER_SEC;
	return elapsed > 20;
}

void Server::lockCritSection(){
	critSectionBusy = true;
}

void Server::unlockCritSection(){
	critSectionBusy = false;
}

void Server::broadCastCheckConcat(){

}

QVariant Server::rpcRequestString(const QVariant &dummy){
	return QVariant(hostString);
}