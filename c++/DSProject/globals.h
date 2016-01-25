#ifndef GLOBALS
#define GLOBALS

#include <QTcpServer>
#include <iostream>
#include <QTextStream>
#include <QThread>

class glb{
public:
    static int findFreePort();

    static int port;
	static QString host;
    static QList<QVariant> connectedNodes;

    static void listOfConnections();

    static QThread* mainThread;
};

#endif //GLOBALS
