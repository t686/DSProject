#include "Client.h"

QString Client::nodeIp = "";
QString Client::nodeIPnPort = "";
std::vector<QString> Client::serverURLs = std::vector<QString>();

Client::Client() : XmlRpcClient("127.0.0.1",0){
    
}

Client::Client(QString host, int port) : XmlRpcClient(host,port){
	
}

void Client::init(){
	std::cout << std::endl << "Client data initializing...";

	foreach(const QHostAddress &address,QNetworkInterface::allAddresses()) {
        if (address.protocol() == QAbstractSocket::IPv4Protocol && address.toString().contains("192.")){
            nodeIp = address.toString();
        }
	}
	nodeIPnPort = nodeIp + ":" + QString("%1").arg(glb::port);

    std::cout << std::endl << "Your current IP address is: " << nodeIPnPort.toStdString();
}

void Client::join(QString newNodeIP){

    setHostAndPort(newNodeIP);

	if (serverURLs.size() > 1){
        std::cout << std::endl << "You are a part of an existing network!";
	}
    else if (newNodeIP == nodeIPnPort){
        std::cout << std::endl << "You can't connect to yourself!";
    }
    else{
		setHost(getFullAddress(urlFormatter(newNodeIP)));
        params.clear();
		params.append(nodeIPnPort);
			
        std::cout << std::endl << "[Client] NEW NODE JOINING: " << newNodeIP.toStdString();

		execute("join", params);

        while (isWaiting())
            QCoreApplication::processEvents();

        QStringList result = response().toStringList();
        if (result.size() > 0){
            foreach(QString str,result){
                if (str != urlFormatter(nodeIPnPort) && !glb::connectedNodes.contains(str)){
                    glb::connectedNodes.append(str);
                    serverURLs.push_back(getFullAddress(urlFormatter(str)));
                }
            }
            std::cout << std::endl << "[Client] Connected !" << std::endl;

            //Inform other nodes about a new member of the network
            for (int i = 0; i<serverURLs.size(); i++){
                setHostAndPort(serverURLs[i]);
                execute("join", params);
            }
            startElection();
        }
        else{
            std::cout << std::endl << "[Client] Could not connect!" << std::endl;
        }
	}
	emit finishedTask();
}

void Client::signOff(){
	//Notify other nodes about leaving the network
	if (serverURLs.size() > 1){

		foreach (QString url,serverURLs) {
			if (url == getFullAddress(nodeIPnPort)){
				setHost(url);
				params.clear();
				params.append(nodeIPnPort);
				execute("signOff", params);
                while (isWaiting())
                    QCoreApplication::processEvents();
				if (!response().toBool()) {
                    std::cout << std::endl << "[Client] Failed to signOff from " << url.toStdString();
				}
			}
		}
		//Probably optimize those straight forward commands 
		serverURLs.clear();
		serverURLs.push_back(getFullAddress(urlFormatter(nodeIPnPort)));
		glb::connectedNodes.clear();
		glb::connectedNodes.append(nodeIPnPort);
		glb::host = "none";
		//TODO cleanup
		std::cout << std::endl << "[Client] Signed off!";
	}
	else{
		std::cout << std::endl << "[Client] You are not connected to a network";
	}
	emit finishedTask();
}

void Client::startElection(){
	if (!(serverURLs.size() > 1)) {
		std::cout << std::endl << "[Client] You are not connected to a network";
	}
	else {
        setHostAndPort(getFullAddress(urlFormatter(nodeIPnPort)));
		params.clear();
        std::cout << std::endl << "starting election on: " << nodeIPnPort.toStdString() << std::endl;
		execute("startElection", params);
	}
	emit finishedTask();
}

void Client::startConcatProcess(){
	if (!(serverURLs.size() > 1)){
        std::cout << std::endl << "[Client] You are not connected to a network";
	}
    else{
        foreach(QString url,serverURLs) {
            std::cout << std::endl << "Concat started for node: " << url.toStdString();
            QList<QVariant> tmpParams;
            tmpParams.append(url);
            execute("startConcatProcess", tmpParams);
        }
    }
	emit finishedTask();
}

void Client::stopOperations(){
	params.clear();
	runOverRpc("stopOperations", params);
	emit finishedTask();
}

void Client::runOverRpc(QString functionName, QList<QVariant> in_params){

	foreach(QString url,serverURLs){
		setHost(url);
        std::cout << std::endl << "[Client] Running function: " << functionName.toStdString() << " over RPC.";
		execute(functionName, in_params);
	}
}

void Client::showAllLists(){
	glb::listOfConnections();
	std::cout << std::endl << "______";
	listOfNodes();
	emit finishedTask();
}

void Client::listOfNodes() {
	if (serverURLs.size() > 0){
		std::cout << std::endl << "[Client] There are " << serverURLs.size() << " network members:";
		foreach(QString url,serverURLs) {
            std::cout << std::endl << url.toStdString();
		}
	}
	else{
		std::cout << std::endl << "The network is empty!";
	}
}

QString Client::getFullAddress(QString address){
    /*if (!address.contains("http://"))
		address = "http://" + address;
    /*if (!address.contains("/xmlrpc"))
        address = address + "/xmlrpc";*/
	return address;
}

QString Client::urlFormatter(QString ip) {
    return ip;//"http://" + ip;// + "/xmlrpc";
}

void Client::setHost(QString host){
    dstHost = host.left(host.indexOf(":"));
}

void Client::setHostAndPort(QString host_port){
    dstHost = host_port.left(host_port.indexOf(":"));
    dstPort = host_port.right(host_port.length() - host_port.indexOf(":") - 1).toInt();
}

void Client::setHostAndPort(QString host, int port){
	dstHost = host;
	dstPort = port;
}

bool Client::isWaiting(){
    return (!isReady() && socket->isOpen() && socket->state() != QAbstractSocket::UnconnectedState);
}

void Client::echo(QString newNodeIP, QString echoStr){
    setHostAndPort(newNodeIP);
    params.clear();
    params.append(echoStr);
    execute("echo",params);

    while (isWaiting())
        QCoreApplication::processEvents();

	std::cout << std::endl << std::endl << "echo recieved: " << response().toString().toStdString() << std::endl << std::endl;
	emit finishedTask();
}