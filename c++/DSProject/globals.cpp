#include "globals.h"

int glb::findFreePort(){
    QTcpServer server;
    server.listen();
    int port = server.serverPort();
    server.close();
    return port;
}

int glb::port = glb::findFreePort();
QString glb::host = "127.0.0.1";
QList<QVariant> glb::connectedNodes = QList<QVariant>();

void glb::listOfConnections(){
    std::cout << std::endl << "[Server] MASTER node is: " << host.toStdString() << QString(":%1").arg(port).toStdString();
    if (connectedNodes.size() > 0){
        std::cout << std::endl << "[Server] There are " << connectedNodes.size() << " IPs:";
        foreach (QVariant str, connectedNodes) {
            std::cout << std::endl << str.toString().toStdString();
        }
    }
    else{
        std::cout << std::endl << "The network is empty!";
    }
}

QThread* glb::mainThread = new QThread();

