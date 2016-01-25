#include "Server.h"

int Server::stoppedNodes = 0;

Server::Server() : XmlRpcServer(){    

    listen(QHostAddress::Any, glb::port);
    registerSlot(this, SLOT(join(const QVariant&)));
    registerSlot(this, SLOT(signOff(const QVariant&)));
    registerSlot(this, SLOT(startElection()));
    registerSlot(this, SLOT(rpcElectionRequest(const QVariant&)));
    registerSlot(this, SLOT(hostBroadcast(const QVariant&)));
    registerSlot(this, SLOT(startConcatProcess()));
    registerSlot(this, SLOT(rpcLifeSign(const QVariant&)));
    registerSlot(this, SLOT(rpcOverrideString(const QVariant&)));
    registerSlot(this, SLOT(checkConcatResult()));
    registerSlot(this, SLOT(echo(const QVariant&)));

	concatObject = new WordConcatenation();
}

void Server::init() {
	std::cout << std::endl << "Server starting...";

    glb::connectedNodes.push_back(Client::nodeIPnPort);
    Client::serverURLs.push_back(Client::getFullAddress(Client::urlFormatter(Client::nodeIPnPort)));

	std::cout << std::endl << "Server succesfuly started!";

	if (!loadFile()) std::cout << std::endl << "File not found!";
	else std::cout << std::endl << "File loaded successfully!";
}

QVariant Server::join(const QVariant &newNodeIPVar){
	QString newNodeIP = newNodeIPVar.toString();
    glb::connectedNodes.append(newNodeIP);
	Client::serverURLs.push_back(Client::getFullAddress(newNodeIP));
    std::cout << std::endl << "[Server] NEW node with address: " << newNodeIP.toStdString() << " was connected!";
    return QVariant::fromValue(glb::connectedNodes);
}

QVariant Server::signOff(const QVariant &nodeIPVar){
	QString nodeIP = nodeIPVar.toString();
    for (size_t n = 0; n < glb::connectedNodes.size(); n++){
        if (glb::connectedNodes[n] == nodeIP){
            std::cout << std::endl << "[Server] Node " << nodeIP.toStdString() << " is leaving the network.";

            glb::connectedNodes.erase(glb::connectedNodes.begin() + n);

			for (int i = 0; i < Client::serverURLs.size(); i++){
				QString url = Client::serverURLs[i];
				if (url == Client::urlFormatter(nodeIP)){
					Client::serverURLs.erase(Client::serverURLs.begin() + i);
				}
			}
            if (glb::host == nodeIP) startElection();
			return true;
		}
	}
	return false;
}

QVariant Server::startElection(){
    if (!(glb::connectedNodes.size() > 1)) {
		std::cout << std::endl << "you are not connected to a network";
	}
    else if (Bully::startElection(glb::port, glb::connectedNodes)) {
        glb::host = Client::nodeIPnPort;
        std::cout << std::endl << "[Server] This application (" << glb::host.toStdString() << ") won the host election";
		broadcastIamHost();
		return true;
	}
	return true;
}

void Server::broadcastIamHost(){
	QList<QVariant> params;
    params.push_back(glb::host);

    foreach (QVariant node,glb::connectedNodes) {
        glbClient::client->setHost(Client::getFullAddress(Client::urlFormatter(node.toString())));
        glbClient::client->execute("hostBroadcast", params);
	}
}

QVariant Server::rpcElectionRequest(const QVariant &requesterVar) {
	int requester = requesterVar.toInt();

    std::cout << std::endl << "[Server] received an election message from: " << requester;

	//this should not happen
    if (requester > glb::port) return "Continue";

	//this servers port has a higher value then the requester so it takes over the election process
	startElection();
	return "Stop";
}

QVariant Server::hostBroadcast(const QVariant &newHostVar){
	QString newHost = newHostVar.toString();
    glb::host = newHost;
	return true;
}

QVariant Server::startConcatProcess(){
    if (Client::nodeIPnPort == glb::host){
		return false;
	}
	bool keepGoing = true;
	concatObject->clearList();

	while (keepGoing) {
        thread()->sleep((double)(rand()%250)/1000.0);
		keepGoing = concatLoop();
	}
	concatObject->checkAddedWords();
	return true;
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

		std::cout << std::endl << "rpcLifeSign call failed";
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
		return "stop";
	}
	if (critSectionBusy) {
		requestQueue.push_back(requester);
		return "wait";
	}
	else {
		if (requestQueue.size() == 0 || requestQueue[requestQueue.size()-1] == requester) {
			requestQueue.pop_back();
			return "goOn";
		}
		return "wait";
	}
	//TODO catch every possible scenario
}

QVariant Server::rpcOverrideString(const QVariant &newStringVar){
	QString newString = newStringVar.toString();
	hostString = newString;
	return true;
}

QVariant Server::checkConcatResult(){
	return concatObject->checkAddedWords();
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
